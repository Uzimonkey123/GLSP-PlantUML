package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.factory.SequenceParts.*;
import com.GLSPPlantUML.utils.ErrorMessage;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.GLSPPlantUML.state.SequenceModelState;
import com.GLSPPlantUML.utils.NodeGap;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.*;
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
        Optional<ErrorMessage> error = modelState.getError();
        if (error.isPresent()) {
            // If there is error present, parsing failed, set message and render diagram
            errorHandler(error);
            return;
        }

        double nodeY = 30;
        double nodeHeight = 30;
        double firstMsgY = nodeY + nodeHeight + 10;

        calculateYPositions(model, firstMsgY); // Get all Y coordinate information for messages/life events/ ver. spaces
        double totalHeight = messagesYPos.stream().mapToDouble(Double::doubleValue).max().orElse(firstMsgY);

        this.gapCalculator = new NodeGap(model);
        this.nodeBuild = new NodeBuild();

        // Build all the nodes and create list to get their centers
        SequenceNodeFactory nodeFactory = new SequenceNodeFactory(model, nodeBuild, totalHeight,
                                                                    messagesYPos, gapCalculator);
        nodeFactory.createNodes();

        this.elements = nodeFactory.getElements();
        this.centre = nodeFactory.getCentre();
        this.halfWidth = nodeFactory.getHalfWidth();
        this.cursor = nodeFactory.getCursor();

        // Build boxes around the created nodes
        SequenceEngloberFactory engloberFactory = new SequenceEngloberFactory(model, centre, halfWidth,
                                                                                    elements, totalHeight);
        engloberFactory.createEnglobers();

        // Build the necessary life events and destroy icons
        SequenceLifeEventFactory leFactory = new SequenceLifeEventFactory(model, lifeEventYPos, centre,
                                                                            elements, messagesYPos);
        leFactory.createSequenceLifeEvents();

        // Build all groups and separators
        SequenceGroupFactory groupFactory = new SequenceGroupFactory(model, messagesYPos, centre, elements);
        groupFactory.createGroups();

        SequenceMessageFactory msgFactory = new SequenceMessageFactory(model, cursor, centre, halfWidth,
                                                                        elements, messagesYPos, gapCalculator);
        msgFactory.createEdges();

        if (model.isMainframe) {
            SequenceMainframeFactory mainframeFactory = new SequenceMainframeFactory(model, centre, halfWidth,
                                                                                        elements);
            mainframeFactory.createMainframe();
        }

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
        int row = 0;
        messagesYPos.clear();
        lifeEventYPos.clear();
        Map<Integer, Integer> spaces = new HashMap<>(model.messageSpaces);

        for (int i = 0; i < model.messages.size(); ) {
            SequenceMessage msg = model.messages.get(i);

            // Look ahead for parallel messages
            int j = i + 1;
            while (j < model.messages.size() && model.messages.get(j).isParallel()) {
                j++;
            }

            int parallelExtra = 0;
            for (int k = i; k < j; k++) {
                if (spaces.containsKey(k)) {
                    hspace += spaces.get(k);
                }

                int autoExtra = getExtra(k);
                parallelExtra = Math.max(parallelExtra, autoExtra);
                spaces.putIfAbsent(k, autoExtra);
            }

            spaces.put(i, parallelExtra);

            // Accumulate height for this row
            hspace += parallelExtra;
            double y = firstMsgY + row * msgGap + hspace;
            messagesYPos.add(y);
            selfActivation(msg, y, i);

            // Place parallels at same Y
            for (int k = i + 1; k < j; k++) {
                messagesYPos.add(y);
                selfActivation(model.messages.get(k), y, k);
                spaces.putIfAbsent(k, getExtra(k));
            }

            row++;
            i = j; // Skip parallel ones since processed
        }

        int trailing = spaces.getOrDefault(model.messages.size(), 0);
        if (trailing > 0 && !messagesYPos.isEmpty()) {
            double lastY = messagesYPos.getLast() + msgGap + trailing;
            messagesYPos.add(lastY);
            lifeEventYPos.add(lastY);
        }
    }

    private int getExtra(int index) {
        SequenceMessage msg = model.messages.get(index);
        int lines = msg.getMessage().split("<br>").length;
        int moteLines = 0;
        if (msg.getNotes() != null) {
            for (SequenceNote note : msg.getNotes()) {
                moteLines = Math.max(moteLines, note.getLabel().split("<br>").length);
            }
        }

        int prevTotalLines = Math.max(lines, moteLines);
        return Math.max(0, (prevTotalLines - 1) * 14);
    }

    private void selfActivation(SequenceMessage msg, double y, int i) {
        // If message is self call activation, the start of life event is lower
        // for deactivation or destroy of self message it does not set offset
        boolean isStartOfLifeEvent = msg.isSelf() &&
                model.participants.stream()
                        .anyMatch(p -> p.getLifeEvents().stream()
                                .anyMatch(e -> e.getStartMessage() == i));
        lifeEventYPos.add(isStartOfLifeEvent ? y + 15 : y);
    }

    private void errorHandler(Optional<ErrorMessage> error) {
        ErrorMessage errorMessage = error.get();
        elements.add(errorMessage.buildError());

        GGraph newGModel = new GGraphBuilder()
                .id("sequence-diagram")
                .addAll(elements)
                .build();

        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }
}
