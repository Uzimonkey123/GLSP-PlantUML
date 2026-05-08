/*
 * File: SequenceMessageFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating message edges, references, labels, anchors, and delegating note creation
 * Date: 7.5.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.AnchorBuild;
import com.diagrams.SequenceDiagram.builders.MessageBuild;
import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceAnchor;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceLifeEvent;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

import static com.diagrams.SequenceDiagram.factory.SequenceFactoryContext.*;

public class SequenceMessageFactory {
    private final SequenceFactoryContext ctx;

    private final Stack<SequenceAnchor> anchors = new Stack<>();
    private final Map<String, SequenceAnchor> anchorMap = new HashMap<>();

    private final SequenceModel model;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<Double> messagesYPos;
    private final List<GModelElement> elements;

    private SequenceMessage msg;
    private final MessageBuild msgBuild;
    private final NodeBuild nodeBuild;
    private final SequenceNoteFactory noteFactory;

    private final int centrePadding = 25;

    public SequenceMessageFactory(SequenceFactoryContext ctx) {
        this.ctx = ctx;

        this.nodeBuild = new NodeBuild();
        this.msgBuild = new MessageBuild(ctx.getHalfWidth());
        this.noteFactory = new SequenceNoteFactory(ctx);

        model = ctx.getModel();
        centre = ctx.getCentre();
        halfWidth = ctx.getHalfWidth();
        messagesYPos = ctx.getMessagesYPos();
        elements = ctx.getElements();
    }

    /**
     * Creates all message edges, references, and notes, plus anchor markers
     */
    public void createEdges() {
        SequenceModel model = ctx.getModel();

        // Populate anchor map with all anchors and match them with their ID
        for (SequenceAnchor anchor : model.anchors) {
            anchorMap.put(anchor.getAnchorId(), anchor);
        }

        for (int i = 0; i < model.messages.size(); i++) {
            this.msg = model.messages.get(i);

            // Handle special types early
            if (msg.getType().equals("edge:ref")) {
                createReference(i);
                continue;
            }

            if (msg.getType().equals("edge:note")) {
                noteFactory.createNote(msg, i);
                continue;
            }

            createEdge(i);

            // Additionally to adding edges, check for anchors and create them if needed
            setupAnchors(i);
        }
    }

    /**
     * Creates a single message edge with source/target coordinates and its label
     */
    private void createEdge(int msgIndex) {
        SequenceNode sourceNode;
        SequenceNode targetNode;
        double y = messagesYPos.get(msgIndex);

        String sourceId = msg.getFromId();
        String targetId = msg.getToId();

        if (msg.getType().equals("edge:delay") || msg.getType().equals("edge:divider")) {
            sourceNode = model.participants.getFirst();
            targetNode = model.participants.getLast();
            sourceId = sourceNode.getId();
            targetId = targetNode.getId();
        }

        boolean incoming = "incoming".equals(msg.decideWay());
        boolean outgoing = "outgoing".equals(msg.decideWay());

        double x1, x2;
        if (msg.isSelf()) {
            x1 = setSelfX(sourceId, msgIndex, true);
            x2 = setSelfX(sourceId, msgIndex, false);

        } else {
            boolean leftToRight = centre.get(targetId) > centre.get(sourceId);
            x1 = setX(sourceId, msgIndex, true, leftToRight);
            x2 = setX(targetId, msgIndex, false, leftToRight);
        }

        // Adjust source/target if message is external (incoming/outgoing)
        if (incoming) {
            x1 = "[".equals(sourceId) ? 0 : ctx.getCursor() + halfWidth.get(model.participants.getLast().getId());
        }

        if (outgoing) {
            x2 = "]".equals(targetId) ? ctx.getCursor() + halfWidth.get(model.participants.getLast().getId()) : 0;
        }

        elements.add(msgBuild.buildEdge(msg, sourceId, targetId, x1, x2, y, incoming, outgoing));
        noteFactory.createNote(msg, msgIndex);
        createMsgLabel(msgIndex, sourceId, targetId, x1, x2);
    }

    /**
     * Creates a reference box spanning from/to participants
     */
    private void createReference(int msgIndex) {
        String from = msg.getFromId();
        String to = msg.getToId();

        double x1 = centre.get(from) - centrePadding;
        double x2 = centre.get(to) + centrePadding;

        String[] lines = msg.getMessage().split("<br>");
        int maxLineLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        int labelWidth = maxLineLength * 8 + defPadding;

        int lineCount = lines.length;
        int labelHeight = lineCount * lineHeight;

        double y1 = messagesYPos.get(msgIndex) - (labelHeight + defPadding);
        double y2 = messagesYPos.get(msgIndex);

        double baseWidth = x2 - x1;
        if (labelWidth > baseWidth) {
            x2 = x1 + labelWidth;
        }

        elements.add(msgBuild.buildReference(msg, from, to, x1, x2, y1, y2));
        createMsgLabel(msgIndex, from, to, x1, x2);
    }

    /**
     * Creates and positions the message label between source and target
     */
    private void createMsgLabel(int msgIndex, String routingOne, String routingTwo, double x1, double x2) {
        double labelShift;
        double y = messagesYPos.get(msgIndex);

        if (msg.isSelf()) {
            labelShift = centre.get(routingOne) + WidthCalculator.calculateWidth(msg.getMessage(), 0) / 2;

        } else {
            String direction = msg.decideWay();

            labelShift = switch (direction) {
                case "outgoing" ->
                        centre.get(routingOne) + WidthCalculator.calculateWidth(msg.getMessage(), defPadding) / 2;

                case "incoming" ->
                        // labelShift is center position, so shift label left by half width
                        // so that right edge aligns exactly with centerX
                        centre.get(routingTwo) - WidthCalculator.calculateWidth(msg.getMessage(), defPadding) / 2;

                default -> (x1 + x2) / 2;
            };
        }

        int lineCount = msg.getMessage().split("<br>").length;
        double labelYOffset = lineCount > 1 ? lineCount * lineHeight : 6;

        elements.add(msgBuild.buildMsgLabel(msg, msgIndex, y, labelShift, labelYOffset));
    }

    /**
     * Pushes/pops anchor start/end markers and creates anchor elements on end
     */
    private void setupAnchors(int msgIndex) {
        double y = messagesYPos.get(msgIndex);
        // If message is an anchor start, get the top Y coordinate of it
        // and save it inside the anchors stack
        if (msg.isAnchorStart()) {
            SequenceAnchor concreteAnchor = anchorMap.get(msg.getAnchorId());
            concreteAnchor.setTopY(y);
            anchors.push(concreteAnchor);
        }

        if (msg.isAnchorEnd()) {
            SequenceAnchor concreteAnchor = anchors.pop();
            double gap = ctx.getGapCalculator().getGaps(concreteAnchor.getParticipant1Id(), concreteAnchor.getParticipant2Id());

            AnchorBuild anchor = new AnchorBuild(concreteAnchor, y, gap);
            double xCoord = anchor.getXCoord(centre.get(concreteAnchor.getParticipant1Id()),
                    centre.get(concreteAnchor.getParticipant2Id()));

            // Build anchor points in the middle of the messages
            nodeBuild.buildAnchorPoints(elements, concreteAnchor, xCoord, y, concreteAnchor.getParticipant1Id(), halfWidth);

            elements.add(anchor.build());
        }
    }

    /**
     * Calculates the X coordinate for a message endpoint, adjusting for life event bars
     */
    private double setX(String participant, int msgIndex, boolean isStart, boolean leftToRight) {
        double lifeEventBar = 6;
        double lifeEventOffset = 4;

        if ("[".equals(participant) || "]".equals(participant)) {
            return centre.getOrDefault(participant, 0.0);
        }

        double centreX = centre.get(participant);
        Optional<SequenceLifeEvent> event = model.getNode(participant).getLifeEventAt(msgIndex);

        // No life event present
        if (event.isEmpty()) return centreX;

        double shift = event.get().getLevel() * lifeEventOffset;
        double leftEdge = centreX + shift - lifeEventBar / 2;
        double rightEdge = centreX + shift + lifeEventBar / 2;

        if (isStart) {
            return leftToRight ? rightEdge : leftEdge;
        } else {
            return leftToRight ? leftEdge : rightEdge;
        }
    }

    /**
     * Calculates the X coordinate for self-message endpoints, handling nested activation levels
     */
    private double setSelfX(String participant, int msgIndex, boolean isStart) {
        double lifeEventBar = 6;
        double lifeEventOffset = 4;

        double centreX = centre.get(participant);
        Optional<SequenceLifeEvent> opt = model.getNode(participant).getLifeEventAt(msgIndex);

        // No life event present
        if (opt.isEmpty()) return centreX;

        SequenceLifeEvent le = opt.get();
        int level = le.getLevel();
        double shift = level * lifeEventOffset;

        // For life event activation and deactivation, end arrow start/end at the previous nested level right edge
        if ((isStart && le.getStartMessage() == msgIndex) ||
            (!isStart && le.getEndMessage() == msgIndex)) {
            return level > 0
                    ? centreX + (level - 1) * lifeEventOffset + lifeEventBar / 2
                    : centreX;
        }

        return centreX + shift + lifeEventBar / 2;
    }
}
