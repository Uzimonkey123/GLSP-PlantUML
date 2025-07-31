package com.GLSPPlantUML.factory.SequenceParts;

import com.GLSPPlantUML.builders.AnchorBuild;
import com.GLSPPlantUML.builders.MessageBuild;
import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.GLSPPlantUML.utils.NodeGap;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

public class SequenceMessageFactory {
    private final SequenceModel model;
    private final double cursor;

    private final Stack<SequenceAnchor> anchors = new Stack<>(); // Stack for anchors in the diagram
    // Map to store anchors with their ID for easier search
    private final Map<String, SequenceAnchor> anchorMap = new HashMap<>();

    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<GModelElement> elements;
    private final NodeGap gapCalculator;

    private SequenceMessage msg;
    private final MessageBuild msgBuild;
    private final NodeBuild nodeBuild;
    private final SequenceNoteFactory noteFactory;

    private final int labelHeight = 14;
    private final int centrePadding = 25;
    private final int padding = 10;


    public SequenceMessageFactory(SequenceModel model, double cursor, Map<String, Double> centre,
                                  Map<String, Double> halfWidth, List<GModelElement> elements,
                                  List<Double> messagesYPos, NodeGap gapCalculator)
    {
        this.model = model;
        this.cursor = cursor;
        this.centre = centre;
        this.halfWidth = halfWidth;
        this.elements = elements;
        this.messagesYPos = messagesYPos;
        this.gapCalculator = gapCalculator;

        this.nodeBuild = new NodeBuild();
        this.msgBuild = new MessageBuild(halfWidth);
        this.noteFactory = new SequenceNoteFactory(model, messagesYPos, centre, halfWidth, elements);
    }

    public void createEdges() {
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

    private void createEdge(int msgIndex) {
        String sourceId = msg.getFrom();
        String targetId = msg.getTo();
        double y = messagesYPos.get(msgIndex);

        if (msg.getType().equals("edge:delay") || msg.getType().equals("edge:divider")) {
            sourceId = model.participants.getFirst().getId();
            targetId = model.participants.getLast().getId();
        }

        boolean incoming = "incoming".equals(msg.decideWay());
        boolean outgoing = "outgoing".equals(msg.decideWay());

        double x1, x2;
        if (msg.isSelf()) {
            x1 = setSelfX(sourceId, msgIndex, true);
            x2 = setSelfX(sourceId, msgIndex, false);

        } else {
            boolean leftToRight = centre.get(targetId) > centre.get(sourceId);
            x1 = setX(sourceId, msgIndex, true,  leftToRight);
            x2 = setX(targetId,   msgIndex, false, leftToRight);
        }

        // Adjust source/target if message is external (incoming/outgoing)
        if (incoming) {
            sourceId = "[";
            x1 = "[".equals(msg.getFrom()) ? 0 : cursor + halfWidth.get(model.participants.getLast().getId());
        }

        if (outgoing) {
            targetId = "]";
            x2 = "]".equals(msg.getTo()) ? cursor + halfWidth.get(model.participants.getLast().getId()) : 0;
        }

        elements.add(msgBuild.buildEdge(msg, sourceId, targetId, x1, x2, y, incoming, outgoing));
        noteFactory.createNote(msg, msgIndex);
        createMsgLabel(msgIndex, sourceId, targetId, x1, x2);
    }

    private void createReference(int msgIndex) {
        String from = msg.getFrom();
        String to = msg.getTo();

        double x1 = centre.get(from) - centrePadding;
        double x2 = centre.get(to) + centrePadding;

        String[] lines = msg.getMessage().split("<br>");
        int maxLineLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        int labelWidth = maxLineLength * 8 + padding;

        int lineCount = lines.length;
        int labelHeight = lineCount * this.labelHeight;

        // Get y1 according to the current index - amount of lines and some padding
        double y1 = messagesYPos.get(msgIndex) - (labelHeight + padding);
        double y2 = messagesYPos.get(msgIndex);

        double baseWidth = x2 - x1;
        if (labelWidth > baseWidth) {
            x2 = x1 + labelWidth; // Move the ref just towards the right, no further extension to the left
        }

        elements.add(msgBuild.buildReference(msg, from, to, x1, x2, y1, y2));
        createMsgLabel(msgIndex, from, to, x1, x2);
    }

    private void createMsgLabel(int msgIndex, String routingOne, String routingTwo, double x1, double x2) {
        double labelShift;
        double y = messagesYPos.get(msgIndex);

        if (msg.isSelf()) {
            labelShift = centre.get(routingOne) + WidthCalculator.calculateWidth(msg.getMessage(), 0) / 2;

        } else {
            String direction = msg.decideWay();

            labelShift = switch (direction) {
                case "outgoing" ->
                        centre.get(routingOne) + WidthCalculator.calculateWidth(msg.getMessage(), padding) / 2;

                case "incoming" ->
                        // labelShift is center position, so shift label left by half width
                        // so that right edge aligns exactly with centerX
                        centre.get(routingTwo) - WidthCalculator.calculateWidth(msg.getMessage(), padding) / 2;

                default -> (x1 + x2) / 2;
            };
        }

        int lineCount = msg.getMessage().split("<br>").length;
        double labelYOffset = lineCount > 1 ? lineCount * labelHeight : 6;

        elements.add(msgBuild.buildMsgLabel(msg, msgIndex, y, labelShift, labelYOffset));
    }

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
            double gap = gapCalculator.getGaps(concreteAnchor.getParticipant1(), concreteAnchor.getParticipant2());

            AnchorBuild anchor = new AnchorBuild(concreteAnchor, y, gap);
            double xCoord = anchor.getXCoord(centre.get(concreteAnchor.getParticipant1()),
                    centre.get(concreteAnchor.getParticipant2()));

            // Build anchor points in the middle of the messages
            nodeBuild.buildAnchorPoints(elements, concreteAnchor, xCoord, y, concreteAnchor.getParticipant1(), halfWidth);

            elements.add(anchor.build());
        }
    }

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
        double leftEdge  = centreX + shift - lifeEventBar / 2;
        double rightEdge = centreX + shift + lifeEventBar / 2;

        if (isStart) {
            return leftToRight ? rightEdge : leftEdge;
        } else {
            return leftToRight ? leftEdge  : rightEdge;
        }
    }

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
