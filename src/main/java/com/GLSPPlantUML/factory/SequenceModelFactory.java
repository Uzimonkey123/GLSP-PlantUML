package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.GLSPPlantUML.state.SequenceModelState;
import com.GLSPPlantUML.utils.NodeGap;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.*;

public class SequenceModelFactory implements GModelFactory {
    private Map<String, Double> centre; // Map to store the middle of all nodes for lifeline
    private Map<String, Double> halfWidth; // Half size of nodes for created node arrows
    private List<GModelElement> elements = new ArrayList<>();
    private NodeGap gapCalculator;
    private NodeBuild nodeBuild;

    private final List<Double> lifeEventYPos = new ArrayList<>();
    private final List<Double> messagesYPos = new ArrayList<>();

    private double cursor;

    @Inject
    protected SequenceModelState modelState;

    @Inject
    protected SequenceModel model;

    @Override
    public void createGModel() {
        double nodeY = 30;
        double nodeHeight = 30;
        double firstMsgY = nodeY + nodeHeight + 10;

        calculateYPositions(model, firstMsgY); // Get all Y coordinate information for messages/life events/ ver. spaces
        double totalHeight = messagesYPos.stream().mapToDouble(Double::doubleValue).max().orElse(firstMsgY);

        this.gapCalculator = new NodeGap(model.messages);
        this.nodeBuild = new NodeBuild();

        // Build all the nodes and create list to get their centers
        SequenceNodeFactory nodeFactory = new SequenceNodeFactory(model, nodeBuild, totalHeight,
                                                                    messagesYPos, gapCalculator);
        nodeFactory.createNodes();

        this.elements = nodeFactory.getElements();
        this.centre = nodeFactory.getCentre();
        this.halfWidth = nodeFactory.getHalfWidth();
        this.cursor = nodeFactory.getCursor();

        // Build all groups and separators
        SequenceGroupFactory groupFactory = new SequenceGroupFactory(model, messagesYPos, centre, elements);
        groupFactory.createGroups();

        // Build the necessary life events and destroy icons
        SequenceLifeEventFactory leFactory = new SequenceLifeEventFactory(model, lifeEventYPos, centre,
                                                                            elements, messagesYPos);
        leFactory.createSequenceLifeEvents();

        SequenceMessageFactory msgFactory = new SequenceMessageFactory(model, cursor, centre, halfWidth,
                                                                        elements, messagesYPos, gapCalculator);
        msgFactory.createEdges();

        // Build the graph
        GGraph newGModel = new GGraphBuilder()
                .id("sequence-diagram")
                .addAll(elements)
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    private void calculateYPositions(SequenceModel model, double firstMsgY) {
        double hspace = 0;
        double msgGap = 35;
        messagesYPos.clear();

        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);
            int lines = msg.getMessage().split("<br>").length;
            int extra = Math.max(0, (lines - 1) * 14);

            if (model.messageSpaces.containsKey(i)) {
                hspace += model.messageSpaces.get(i);
            } else {
                model.messageSpaces.put(i, extra);
            }

            hspace += extra;

            double y = firstMsgY + i * msgGap + hspace;
            messagesYPos.add(y);

            // If message is self call activation, the start of life event is lower
            // for deactivation or destroy of self message it does not set offset
            int finalI = i;
            boolean isStartOfLifeEvent = msg.isSelf() &&
                    model.participants.stream()
                    .anyMatch(node -> node.getLifeEvents().stream()
                            .anyMatch(event -> event.getStartMessage() == finalI));

            lifeEventYPos.add(isStartOfLifeEvent ? y + 15 : y);
        }

        int trailing = model.messageSpaces.getOrDefault(model.messages.size(), 0);
        if (trailing > 0) {
            // In case HSpace is added after the last message
            double lastY = messagesYPos.getLast() + msgGap + trailing;
            messagesYPos.add(lastY);
            lifeEventYPos.add(lastY);
        }
    }
}
