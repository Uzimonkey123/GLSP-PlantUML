package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.attributes.AnchorBuild;
import com.GLSPPlantUML.attributes.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.state.SequenceModelState;
import com.GLSPPlantUML.utils.NodeGap;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.*;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class SequenceModelFactory implements GModelFactory {
    private Map<String, Double> centre; // Map to store the middle of all nodes for lifeline
    private Map<String, Double> halfWidth; // Half size of nodes for created node arrows
    private List<GModelElement> elements;
    private NodeGap gapCalculator;

    private Stack<SequenceAnchor> anchors; // Stack for anchors in the diagram
    private Map<String, SequenceAnchor> anchorMap; // Map to store anchors with their ID for easier search
    private List<Double> lifeEventYPos;
    private List<Double> messagesYPos;

    private final double nodeY = 30;
    private final double nodeHeight = 30;
    private final double msgGap = 35;
    private final double extraBottom = 50;
    private double cursor = 40; // Start of the first node

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        SequenceModel model = modelState.getModel();

        int messagesCount = model.messages.size();
        double firstMsgY = nodeY + nodeHeight + 20;

        this.messagesYPos = new ArrayList<>();
        this.lifeEventYPos = new ArrayList<>();
        calculateYPositions(model, firstMsgY);

        int additionalSpace = model.messageSpaces.values().stream().mapToInt(Integer::intValue).sum();
        double lifelineLength = (messagesCount - 1) * msgGap + extraBottom + additionalSpace;
        double totalHeight = nodeHeight + lifelineLength + nodeHeight;

        this.centre = new HashMap<>();
        this.halfWidth = new HashMap<>();
        this.elements = new ArrayList<>();

        this.gapCalculator = new NodeGap(model.messages);

        // Add participants as nodes to the list
        for (SequenceNode node: model.participants) {
            int creationIndex = node.isCreatedNode() ? node.getCreatedIndex() : 0;
            int extraOffset = 0;

            // To check if there was added extra spaces due to multiline labels
            for (int i = 0; i < creationIndex; i++) {
                extraOffset += model.messageSpaces.getOrDefault(i, 0);
            }

            NodeBuild nodeAttr = new NodeBuild(node, cursor, nodeY, totalHeight,
                                                            extraOffset, creationIndex, model.showFoot);
            elements.add(nodeAttr.build());

            centre.put(node.getName(), nodeAttr.getCenter());
            halfWidth.put(node.getName(), nodeAttr.getHalfWidth());

            String nextName = model.getNextParticipant(node.getName());
            if (!nextName.equals(node.getName())) {
                double gap = gapCalculator.getGaps(node.getName(), nextName);
                cursor += nodeAttr.getNodeWidth() + gap + halfWidth.get(node.getName());
            }

            int destroyIndex = node.getDestroyIndex() == -1 ? 0 : node.getDestroyIndex();

            // Add life event boxes if they exist
            generateLifeEvents(node);

            // Add destroy X if they exist
            addDestroyCross(node, destroyIndex);
        }

        cursor += halfWidth.get(model.participants.getLast().getName()) + 40;

        // Add invisible nodes for incoming or outgoing messages
        generateInvisibleNodes(totalHeight);

        // Add messages as edges with proper text, source and target
        this.anchors = new Stack<>();
        this.anchorMap = new HashMap<>();

        // Populate anchor map with all anchors and match them with their ID
        for (SequenceAnchor anchor : model.anchors) {
            anchorMap.put(anchor.getAnchorId(), anchor);
        }

        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);

            addEdges(msg, model, i);

            // Additionally to adding edges, check for anchors and create them if needed
            setupAnchors(msg, i);
        }

        // Add page details like header, title, footer
        addPageDetails(model, totalHeight);

        // Build the graph
        GGraph newGModel = new GGraphBuilder() //
                .id("sequence-diagram") //
                .addAll(elements) //
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    private void addDestroyCross(SequenceNode node, int destroyIndex) {
        if (destroyIndex == 0) return;

        elements.add(new GNodeBuilder("destroy")
                .id("dest-" + node.getName() + "-" + destroyIndex)
                .position(centre.get(node.getName()), messagesYPos.get(destroyIndex))
                .size(1, 1)
                .build());
    }

    private void calculateYPositions(SequenceModel model, double firstMsgY) {
        double hspace = 0;
        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);
            int lines = msg.getMessage().split("<br>").length;
            int extra = Math.max(0, (lines - 1) * 15);

            if (model.messageSpaces.containsKey(i)) {
                hspace += model.messageSpaces.get(i);
            } else {
                model.messageSpaces.put(i, extra);
            }

            hspace += extra;

            double y = firstMsgY + i * msgGap + hspace;
            messagesYPos.add(y);

            // If message is self call activation, the start of life event is lower
            lifeEventYPos.add(msg.isSelf() ? y + 15 : y);
        }
    }

    private void setupAnchors(SequenceMessage msg, int msgIndex) {
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

            // Create anchor points in the middle of the messages
            generateAnchorPoints(concreteAnchor, concreteAnchor.getParticipant1(), xCoord, y);

            elements.add(anchor.build());

        }
    }

    private void addEdges(SequenceMessage msg, SequenceModel model, int msgIndex) {
        String sourceId, targetId;
        double x1, x2;
        double y = messagesYPos.get(msgIndex);

        String routingOne = msg.getFrom();
        String routingTwo = msg.getTo();
        if (msg.getType().equals("edge:delay") || msg.getType().equals("edge:divider")) {
            routingOne = model.participants.getFirst().getName();
            routingTwo = model.participants.getLast().getName();
        }

        boolean incoming = msg.decideWay().equals("incoming");
        boolean outgoing = msg.decideWay().equals("outgoing");

        sourceId = routingOne;
        targetId = routingTwo;
        x1 = setX(routingOne, msgIndex);
        x2 = setX(routingTwo, msgIndex);

        if (incoming) {
            sourceId = "[";
            x1 = msg.getFrom().equals("[") ? 0 : cursor + halfWidth.get(model.participants.getLast().getName());

        } else if (outgoing) {
            targetId = "]";
            x2 = msg.getTo().equals("]") ? cursor + halfWidth.get(model.participants.getLast().getName()) : 0;

        }

        GEdgeBuilder eb;
        eb = new GEdgeBuilder(msg.getType())
                .id(msg.getMsgId())
                .sourceId(sourceId)
                .targetId(targetId)
                .addRoutingPoint(point(x1, y))
                .addRoutingPoint(point(x2, y));

        if (msg.getType().equals("edge:divider")) {
            eb.addArgument("labelWidth", msg.getMessage().length());
        }

        // Additional arguments to get every side and aspect of the arrow
        if (msg.getType().equals("edge")) {
            eb.addArgument("headStart", msg.getStartHead());
            eb.addArgument("headEnd", msg.getEndHead());
            eb.addArgument("partStart", msg.getStartPart());
            eb.addArgument("partEnd", msg.getEndPart());
            eb.addArgument("circleStart", msg.getStartDecor());
            eb.addArgument("circleEnd", msg.getEndDecor());
            eb.addArgument("style", msg.isDotted() ? "dotted" : "solid");
            eb.addArgument("self", msg.isSelf());
            eb.addArgument("arrColor", msg.getColor());

            eb.addArgument("creating", msg.isCreating());
            eb.addArgument("toWidth", halfWidth.get(targetId));
        }

        if (incoming || outgoing) {
            eb.addArgument("incoming", incoming);
            eb.addArgument("outgoing", outgoing);
            eb.addArgument("isShort", msg.isShort());

            // For short arrows
            eb.addArgument("fromX", x1);
            eb.addArgument("toX", x2);
        }

        elements.add(eb.build());

        // Add labels to messages
        addLabels(msg, model, msgIndex, x1, x2, routingOne);
    }

    private void addLabels(SequenceMessage msg, SequenceModel model, int msgIndex, double x1, double x2,
                           String routingOne) {
        double labelShift;
        double y = messagesYPos.get(msgIndex);

        if (msg.isSelf()) {
            labelShift = (centre.get(routingOne) + centre.get(model.getNextParticipant(routingOne))) / 2;
        } else {
            labelShift = (x1 + x2) / 2;
        }

        int lineCount = msg.getMessage().split("<br>").length;
        double labelYOffset = lineCount > 1 ? lineCount * 14 : 6;

        elements.add(new GLabelBuilder("label:html")
                .id("label-"+msgIndex)
                .text(msg.getMessage())
                .addArgument("numbering", msg.getNumbering())
                .position(labelShift, y - labelYOffset)
                .build());
    }

    private void generateLifeEvents(SequenceNode node) {
        for (SequenceLifeEvent le : node.getLifeEvents()) {
            double center = centre.get(node.getName()) - 3;
            double shift = 4 * le.getLevel();
            double y1 = lifeEventYPos.get(le.getStartMessage());
            double y2 = lifeEventYPos.get(le.getEndMessage());

            double x = center + shift;

            elements.add(new GNodeBuilder("lifeEvent")
                    .id("act-" + node.getName() + "-" + le.getStartMessage())
                    .position(x, y1)
                    .size(6, y2 - y1)
                    .addArgument("background", le.getBackground())
                    .build());
        }
    }

    private void generateAnchorPoints(SequenceAnchor anchor, String from, double xCoord, double y) {
        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-top")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), anchor.getTopY())
                .size(0, 0)
                .build());

        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-bottom")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), y)
                .size(0, 0)
                .build());
    }

    private void generateInvisibleNodes(double totalHeight) {
        elements.add(new GNodeBuilder()
                .id("[")
                .layout("vbox")
                .position(0, nodeY)
                .size(0, totalHeight)
                .build());
        centre.put("[", 0.0);

        elements.add(new GNodeBuilder()
                .id("]")
                .layout("vbox")
                .position(cursor * 2, nodeY)
                .size(0, totalHeight)
                .build());
        centre.put("]", cursor);
    }

    private void addPageDetails(SequenceModel model, double totalHeight) {
        elements.add(new GLabelBuilder("label:header")
                .id("header")
                .position(cursor / 2, -40)
                .text(model.header)
                .build());

        elements.add(new GLabelBuilder("label:title")
                .id("title")
                .position(cursor / 2, -20)
                .text(model.title)
                .build());

        elements.add(new GLabelBuilder("label:footer")
                .id("footer")
                .position(cursor / 2, totalHeight + 80)
                .text(model.footer)
                .build());
    }

    private double setX(String participant, int messageIndex) {
        if (participant.equals("[") || participant.equals("]")) {
            return centre.getOrDefault(participant, 0.0);
        }

        SequenceNode node = modelState.getModel().getNode(participant);
        Optional<SequenceLifeEvent> lifeEventPos = node.getLifeEventAt(messageIndex);

        return lifeEventPos
                // Set the arrow more to the right if there is life event/nesting
                // or just return normal participant middle X
                .map(lifeEvent -> centre.get(participant) + (lifeEvent.getLevel() + 1) * 3)
                .orElse(centre.get(participant));
    }
}
