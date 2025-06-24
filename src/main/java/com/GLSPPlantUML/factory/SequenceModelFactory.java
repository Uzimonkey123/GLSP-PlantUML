package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.state.SequenceModelState;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.*;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class SequenceModelFactory implements GModelFactory {
    private Map<String, Double> centre; // Map to store the middle of all nodes for lifeline
    private Map<String, Double> halfWidth; // Half size of nodes for created node arrows
    private List<GModelElement> elements;

    private Stack<SequenceAnchor> anchors; // Stack for anchors in the diagram
    private Map<String, SequenceAnchor> anchorMap; // Map to store anchors with their ID for easier search
    private List<Double> lifeEventYPos;

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        SequenceModel model = modelState.getModel();

        int messagesCount = model.messages.size();
        double nodeY = 30;
        double nodeHeight = 30;

        double firstMsgY = nodeY + nodeHeight + 20;
        double msgGap    = 25;
        double extraBottom = 50;

        int hspace = 0;
        int additionalSpace = model.messageSpaces.values().stream().mapToInt(Integer::intValue).sum();

        double lifelineLength = (messagesCount - 1) * msgGap + extraBottom + additionalSpace;
        double totalHeight = nodeHeight + lifelineLength + nodeHeight;

        double cursor = 40; // Start of the first node
        double gap = Math.max(40, getMaxMessageLength(model));

        this.centre = new HashMap<>();
        this.halfWidth = new HashMap<>();
        this.elements = new ArrayList<>();
        this.lifeEventYPos = new ArrayList<>();

        // Add participants as nodes to the list
        for (SequenceNode node: model.participants) {
            String p = node.getName();
            double textWidth = p.length() * 8;
            double nodeWidth = textWidth + 10 * 2;

            double centreX = cursor + nodeWidth / 2;
            centre.put(p, centreX);
            halfWidth.put(p, nodeWidth / 2);

            int creationIndex = node.isCreatedNode() ? node.getCreatedIndex() : 0;
            double yOffset = creationIndex * msgGap;
            double nodeStartY = nodeY + yOffset;

            elements.add(new GNodeBuilder(node.getType())
                    .id(p)
                    .layout("vbox")
                    .position(cursor, nodeStartY)
                    .addArgument("background", node.getBackground())
                    .addArgument("showFoot", model.showFoot)
                    .size(nodeWidth, totalHeight - yOffset)
                    .add(new GLabelBuilder().text(p).build())
                    .build());

            cursor += nodeWidth + gap;
        }

        // Add invisible nodes for incoming or outgoing messages
        generateInvisibleNodes(cursor, nodeY, totalHeight);

        // Add messages as edges with proper text, source and target
        this.anchors = new Stack<>();
        this.anchorMap = new HashMap<>();

        // Populate anchor map with all anchors and match them with their ID
        for (SequenceAnchor anchor : model.anchors) {
            anchorMap.put(anchor.getAnchorId(), anchor);
        }

        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);

            // If a vertical space is here, add it to the overall hspace
            if (model.messageSpaces.containsKey(i)) {
                hspace += model.messageSpaces.get(i);
            }

            double y = firstMsgY + i * msgGap + hspace;
            // If message is self call activation, the start of life event is lower
            final boolean b = msg.isSelf()
                    ? lifeEventYPos.add(y + 15)
                    : lifeEventYPos.add(y);

            addEdges(msg, y, model, cursor, i);

            // Additionally to adding edges, check for anchors and create them if needed
            setupAnchors(msg, y, gap);
        }

        // Add life event boxes if they exist
        generateLifeEvents(model);

        // Add page details like header, title, footer
        addPageDetails(model, cursor, totalHeight);

        // Build the graph
        GGraph newGModel = new GGraphBuilder() //
                .id("sequence-diagram") //
                .addAll(elements) //
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    private void setupAnchors(SequenceMessage msg, double y, double gap) {
        // If message is an anchor start, get the top Y coordinate of it
        // and save it inside the anchors stack
        if (msg.isAnchorStart()) {
            SequenceAnchor concreteAnchor = anchorMap.get(msg.getAnchorId());
            concreteAnchor.setTopY(y);
            anchors.push(concreteAnchor);
        }

        if (msg.isAnchorEnd()) {
            SequenceAnchor concreteAnchor = anchors.pop();

            String from = concreteAnchor.getParticipant1();
            String to = concreteAnchor.getParticipant2();
            double xCoord;

            // Calculate the x coordinate to know if it is left or right from the give nodes
            if (centre.get(from) > centre.get(to)) {
                xCoord = centre.get(from) - gap / 2;
            } else {
                xCoord = centre.get(to) - gap / 2;
            }

            // Create anchor points in the middle of the messages
            generateAnchorPoints(concreteAnchor, from, xCoord, y);

            elements.add(new GEdgeBuilder("anchor-arrow")
                    .id(concreteAnchor.getAnchorId())
                    .sourceId(concreteAnchor.getAnchorId() + "-top")
                    .targetId(concreteAnchor.getAnchorId() + "-bottom")
                    .addRoutingPoint(point(xCoord, concreteAnchor.getTopY()))
                    .addRoutingPoint(point(xCoord, y))
                    .add(new GLabelBuilder("label:html")
                            .text(concreteAnchor.getLabel())
                            .addArgument("numbering", msg.getNumbering())
                            .edgePlacement(new GEdgePlacementBuilder()
                                    .side(GConstants.EdgeSide.RIGHT) // To the right from the label
                                    .position(0.5d) // center
                                    .offset(-4d)
                                    .rotate(false)
                                    .build())
                            .build())
                    .build());
        }
    }

    private void addEdges(SequenceMessage msg, double y, SequenceModel model, double cursor, int msgIndex) {
        String sourceId, targetId;
        double x1, x2;

        String routingOne = msg.getFrom();
        String routingTwo = msg.getTo();
        if (msg.getType().equals("edge:delay") || msg.getType().equals("edge:divider")) {
            routingOne = model.participants.getFirst().getName();
            routingTwo = model.participants.getLast().getName();
        }

        boolean incoming = msg.decideWay().equals("incoming");
        boolean outgoing = msg.decideWay().equals("outgoing");

        if (incoming) {
            sourceId = "[";
            targetId = routingTwo;
            x1 = msg.getFrom().equals("[") ? 0 : cursor;
            x2 = setX(routingTwo, msgIndex);

        } else if (outgoing) {
            sourceId = routingOne;
            targetId = "]";
            x1 = setX(routingOne, msgIndex);
            x2 = msg.getTo().equals("]") ? cursor : 0;

        } else {
            sourceId = routingOne;
            targetId = routingTwo;
            x1 = setX(routingOne, msgIndex);
            x2 = setX(routingTwo, msgIndex);
        }

        GEdgeBuilder eb;
        eb = new GEdgeBuilder(msg.getType())
                .id(msg.getMsgId())
                .sourceId(sourceId)
                .targetId(targetId)
                .addRoutingPoint(point(x1, y))
                .addRoutingPoint(point(x2, y));

        elements.add(new GLabelBuilder("label:html")
                .text(msg.getMessage())
                .addArgument("numbering", msg.getNumbering())
                .position((x1 + x2) / 2, y - 6)
                .build());

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
    }

    private void generateLifeEvents(SequenceModel model) {
        for (SequenceNode node : model.participants) {
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

    private void generateInvisibleNodes(double cursor, double nodeY, double totalHeight) {
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
                .position(cursor, nodeY)
                .size(0, totalHeight)
                .build());
        centre.put("]", cursor);
    }

    private void addPageDetails(SequenceModel model, double cursor, double totalHeight) {
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

    private double getMaxMessageLength(SequenceModel model) {
        double charWidth = 4;
        double padding = 5;

        double maxWidth = 0;
        for (SequenceMessage msg : model.messages) {
            int len = msg.getMessage() != null ? msg.getMessage().length() + msg.getNumbering().length() : 0;
            double width = charWidth * len + padding;
            if (maxWidth < width) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    private double setX(String participant, int messageIndex) {
        SequenceNode node = modelState.getModel().getNode(participant);
        Optional<SequenceLifeEvent> lifeEventPos = node.getLifeEventAt(messageIndex);

        return lifeEventPos
                // Set the arrow more to the right if there is life event/nesting
                // or just return normal participant middle X
                .map(lifeEvent -> centre.get(participant) + (lifeEvent.getLevel() + 1) * 3)
                .orElse(centre.get(participant));
    }
}
