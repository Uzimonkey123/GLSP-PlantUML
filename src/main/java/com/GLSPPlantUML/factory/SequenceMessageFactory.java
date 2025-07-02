package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.builders.AnchorBuild;
import com.GLSPPlantUML.builders.MessageBuild;
import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.NodeGap;
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
    }

    public void createEdges() {
        // Populate anchor map with all anchors and match them with their ID
        for (SequenceAnchor anchor : model.anchors) {
            anchorMap.put(anchor.getAnchorId(), anchor);
        }

        for (int i = 0; i < model.messages.size(); i++) {
            this.msg = model.messages.get(i);

            // Handle special type early
            if (msg.getType().equals("edge:ref")) {
                createReference(i);
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
            sourceId = model.participants.getFirst().getName();
            targetId = model.participants.getLast().getName();
        }

        boolean incoming = "incoming".equals(msg.decideWay());
        boolean outgoing = "outgoing".equals(msg.decideWay());

        double x1 = setX(sourceId, msgIndex);
        double x2 = setX(targetId, msgIndex);

        // Adjust source/target if message is external (incoming/outgoing)
        if (incoming) {
            sourceId = "[";
            x1 = "[".equals(msg.getFrom()) ? 0 : cursor + halfWidth.get(model.participants.getLast().getName());
        }

        if (outgoing) {
            targetId = "]";
            x2 = "]".equals(msg.getTo()) ? cursor + halfWidth.get(model.participants.getLast().getName()) : 0;
        }

        elements.add(msgBuild.buildEdge(msg, sourceId, targetId, x1, x2, y, incoming, outgoing));
        createMsgLabel(msgIndex, sourceId, x1, x2);
    }

    private void createReference(int msgIndex) {
        String from = msg.getFrom();
        String to = msg.getTo();

        double x1 = centre.get(from) - 25;
        double x2 = centre.get(to) + 25;

        String[] lines = msg.getMessage().split("<br>");
        int maxLineLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        int labelWidth = maxLineLength * 8 + 5;

        int lineCount = lines.length;
        int labelHeight = lineCount * 14;

        // Get y1 according to the current index - amount of lines and some padding
        double y1 = messagesYPos.get(msgIndex) - (labelHeight + 10);
        double y2 = messagesYPos.get(msgIndex);

        double baseWidth = x2 - x1;
        if (labelWidth > baseWidth) {
            x2 = x1 + labelWidth; // Move the ref just towards the right, no further extension to the left
        }

       elements.add(msgBuild.buildReference(msg, from, to, x1, x2, y1, y2));
        createMsgLabel(msgIndex, from, x1, x2);
    }

    private void createMsgLabel(int msgIndex, String routingOne, double x1, double x2) {
        double labelShift;
        double y = messagesYPos.get(msgIndex);

        if (msg.isSelf()) {
            labelShift = (centre.get(routingOne) + centre.get(model.getNextParticipant(routingOne))) / 2;
        } else {
            labelShift = (x1 + x2) / 2;
        }

        int lineCount = msg.getMessage().split("<br>").length;
        double labelYOffset = lineCount > 1 ? lineCount * 14 : 6;

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

    private double setX(String participant, int messageIndex) {
        if (participant.equals("[") || participant.equals("]")) {
            return centre.getOrDefault(participant, 0.0);
        }

        SequenceNode node = model.getNode(participant);
        Optional<SequenceLifeEvent> lifeEventPos = node.getLifeEventAt(messageIndex);

        return lifeEventPos
                // Set the arrow more to the right if there is life event/nesting
                // or just return normal participant middle X
                .map(lifeEvent -> centre.get(participant) + (lifeEvent.getLevel() + 1) * 3)
                .orElse(centre.get(participant));
    }
}
