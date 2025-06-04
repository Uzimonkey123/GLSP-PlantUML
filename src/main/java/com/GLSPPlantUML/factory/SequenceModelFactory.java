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
        double gap    = 120; // Gap to add between the different nodes

        Map<String, Double> centre = new HashMap<>(); // Map to store the middle of all nodes for lifeline
        List<GModelElement> elements = new ArrayList<>();

        // Add participants as nodes to the list
        for (String p : model.participants) {
            double textWidth = p.length() * 8;
            double nodeWidth = textWidth + 10 * 2;

            double centreX = cursor + nodeWidth / 2;
            centre.put(p, centreX);

            elements.add(new GNodeBuilder("node:rectangle")
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

            GEdgeBuilder eb = new GEdgeBuilder("edge")
                    .id("msg-" + i)
                    .sourceId(msg.getFrom())
                    .targetId(msg.getTo())
                    .addRoutingPoint(point(centre.get(msg.getFrom()), y))
                    .addRoutingPoint(point(centre.get(msg.getTo()), y))
                    .add(new GLabelBuilder()
                            .text(msg.getMessage())
                            .edgePlacement(new GEdgePlacementBuilder()
                                    .side(GConstants.EdgeSide.TOP) // above the line
                                    .position(0.5d) // center
                                    .offset(8d)
                                    .rotate(false)
                                    .build())
                            .build());

            // Additional arguments to get every side and aspect of the arrow
            eb.addArgument("headStart", msg.getStartHead());
            eb.addArgument("headEnd", msg.getEndHead());
            eb.addArgument("partStart", msg.getStartPart());
            eb.addArgument("partEnd", msg.getEndPart());
            eb.addArgument("circleStart", msg.getStartDecor());
            eb.addArgument("circleEnd", msg.getEndDecor());
            eb.addArgument("style", msg.isDotted() ? "dotted" : "solid");

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
}
