package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.model.SequenceModel;
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
        System.err.println(gap);

        Map<String, Double> centre = new HashMap<>(); // Map to store the middle of all nodes for lifeline
        List<GModelElement> elements = new ArrayList<>();

        // Add participants as nodes to the list
        for (SequenceModel.SequenceNode node: model.participants) {
            String p = node.getName();
            double textWidth = p.length() * 8;
            double nodeWidth = textWidth + 10 * 2;

            double centreX = cursor + nodeWidth / 2;
            centre.put(p, centreX);

            elements.add(new GNodeBuilder(node.getType())
                    .id(p)
                    .layout("vbox")
                    .position(cursor, nodeY)
                    .size(nodeWidth, totalHeight)
                    .add(new GLabelBuilder().text(p).build())
                    .build());

            cursor += nodeWidth + gap;
        }

        // Add messages as edges with proper text, source and target
        for (int i = 0; i < model.messages.size(); i++) {
            SequenceModel.SequenceMessage msg = model.messages.get(i);
            double y = firstMsgY + i * msgGap;
            String routingOne, two;
            if(!msg.getType().equals("edge")) {
                routingOne = model.participants.getFirst().getName();
                two = model.participants.getLast().getName();
            } else {
                routingOne = msg.getFrom();
                two = msg.getTo();
            }

            GEdgeBuilder eb = new GEdgeBuilder(msg.getType())
                    .id("msg-" + i)
                    .sourceId(routingOne)
                    .targetId(two)
                    .addRoutingPoint(point(centre.get(routingOne), y))
                    .addRoutingPoint(point(centre.get(two), y))
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
            }

            elements.add(eb.build());
        }

        // Build the graph
        GGraph newGModel = new GGraphBuilder() //
                .id("sequence-diagram") //
                .addAll(elements) //
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    private double getMaxMessageLength(SequenceModel model) {
        double charWidth = 4;
        double padding = 5;

        double maxWidth = 0;
        for (SequenceModel.SequenceMessage msg : model.messages) {
            if (msg.isSelf()) continue;
            int len = msg.getMessage() != null ? msg.getMessage().length() + msg.getNumbering().length() : 0;
            System.err.println(msg.getMessage() + " " + len);
            double width = charWidth * len + padding;
            if (maxWidth < width) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }
}
