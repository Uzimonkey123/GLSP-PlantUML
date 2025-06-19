package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.state.SequenceModelState;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.graph.util.GConstants;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class SequenceModelFactory implements GModelFactory {
    private Map<String, Double> centre;
    private Map<String, Double> halfWidth;
    private List<GModelElement> elements;

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        SequenceModel model = modelState.getModel();

        int messagesCount = model.messages.size();
        double nodeY = 30;
        double nodeHeight = 30;

        double firstMsgY = nodeY + nodeHeight + 20;
        double msgGap    = 30;
        double extraBottom = 50;
        double lifelineLength = (messagesCount - 1) * msgGap + extraBottom;
        double totalHeight = nodeHeight + lifelineLength + nodeHeight;

        double cursor = 40; // Start of the first node
        double gap = Math.max(40, getMaxMessageLength(model));

        this.centre = new HashMap<>(); // Map to store the middle of all nodes for lifeline
        this.halfWidth = new HashMap<>();
        this.elements = new ArrayList<>();

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
                    .size(nodeWidth, totalHeight - yOffset)
                    .add(new GLabelBuilder().text(p).build())
                    .build());

            cursor += nodeWidth + gap;
        }

        // Add invisible nodes for incoming or outgoing messages
        generateInvisibleNodes(cursor, nodeY, totalHeight);

        // Add messages as edges with proper text, source and target
        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);
            double y = firstMsgY + i * msgGap;
            addEdges(msg, y, model, cursor);
        }

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

    private void addEdges(SequenceMessage msg, double y, SequenceModel model, double cursor) {
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
            x2 = centre.get(routingTwo);

        } else if (outgoing) {
            sourceId = routingOne;
            targetId = "]";
            x1 = centre.get(routingOne);
            x2 = msg.getTo().equals("]") ? cursor : 0;

        } else {
            sourceId = routingOne;
            targetId = routingTwo;
            x1 = centre.get(routingOne);
            x2 = centre.get(routingTwo);
        }

        GEdgeBuilder eb;
        eb = new GEdgeBuilder(msg.getType())
                .id(msg.getMsgId())
                .sourceId(sourceId)
                .targetId(targetId)
                .addRoutingPoint(point(x1, y))
                .addRoutingPoint(point(x2, y))
                .add(new GLabelBuilder("label:html")
                        .text(msg.getMessage())
                        .addArgument("numbering", msg.getNumbering())
                        .edgePlacement(new GEdgePlacementBuilder()
                                .side(GConstants.EdgeSide.TOP) // above the line
                                .position(0.5d) // center
                                .offset(8d)
                                .rotate(false)
                                .build())
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
}
