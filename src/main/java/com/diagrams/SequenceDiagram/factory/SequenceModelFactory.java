/*
 * File: SequenceModelFactory.java
 * Author: Norman Babiak
 * Description: Main factory that does GLSP graph creation for sequence diagrams from GModel
 * Date: 7.5.2026
 */

package com.diagrams.SequenceDiagram.factory;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceParts.*;
import com.GLSPPlantUML.utils.ErrorMessage;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import com.diagrams.SequenceDiagram.utils.NodeGap;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNote;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.*;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.*;

public class SequenceModelFactory implements GModelFactory {

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        Optional<ErrorMessage> error = modelState.getError();
        if (error.isPresent()) {
            // If there is error present, parsing failed, set message and render diagram
            errorHandler(error);
            return;
        }

        SequenceModel model = modelState.getModel();
        SequenceFactoryContext ctx = new SequenceFactoryContext(model);

        double nodeHeight = 30;
        double firstMsgY = SequenceFactoryContext.nodeY + nodeHeight + SequenceFactoryContext.defPadding;

        calculateYPositions(ctx, firstMsgY);
        double totalHeight = ctx.getMessagesYPos().stream()
                .mapToDouble(Double::doubleValue).max().orElse(firstMsgY);

        ctx.setGapCalculator(new NodeGap(model));

        // Build all the nodes and create list to get their centres
        SequenceNodeFactory nodeFactory = new SequenceNodeFactory(ctx, new NodeBuild(), totalHeight);
        nodeFactory.createNodes();

        // Build boxes around the created nodes
        SequenceEngloberFactory engloberFactory = new SequenceEngloberFactory(ctx, totalHeight);
        engloberFactory.createEnglobers();

        // Build the necessary life events and destroy icons
        SequenceLifeEventFactory leFactory = new SequenceLifeEventFactory(ctx);
        leFactory.createSequenceLifeEvents();

        // Build all groups and separators
        SequenceGroupFactory groupFactory = new SequenceGroupFactory(ctx);
        groupFactory.createGroups();

        // Build all messages, references, and notes
        SequenceMessageFactory msgFactory = new SequenceMessageFactory(ctx);
        msgFactory.createEdges();

        // Build mainframe if present
        if (model.isMainframe) {
            SequenceMainframeFactory mainframeFactory = new SequenceMainframeFactory(ctx);
            mainframeFactory.createMainframe();
        }

        // Build the graph
        GGraph newGModel = new GGraphBuilder()
                .id("sequence-diagram")
                .addAll(ctx.getElements())
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    /**
     * Calculates Y positions for all messages and life events, accounting for
     * parallel messages, vertical spacing (HSpace), and multi-line labels.
     */
    private void calculateYPositions(SequenceFactoryContext ctx, double firstMsgY) {
        SequenceModel model = ctx.getModel();
        List<Double> messagesYPos = ctx.getMessagesYPos();
        List<Double> lifeEventYPos = ctx.getLifeEventYPos();

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

                int autoExtra = getExtra(model, k);
                parallelExtra = Math.max(parallelExtra, autoExtra);
                spaces.putIfAbsent(k, autoExtra);
            }

            spaces.put(i, parallelExtra);

            // Accumulate height for this row
            hspace += parallelExtra;
            double y = firstMsgY + row * msgGap + hspace;
            messagesYPos.add(y);
            addLifeEventY(model, lifeEventYPos, msg, y, i);

            // Place parallels at same Y
            for (int k = i + 1; k < j; k++) {
                messagesYPos.add(y);
                addLifeEventY(model, lifeEventYPos, model.messages.get(k), y, k);
                spaces.putIfAbsent(k, getExtra(model, k));
            }

            row++;
            i = j;
        }

        int trailing = spaces.getOrDefault(model.messages.size(), 0);
        if (trailing > 0 && !messagesYPos.isEmpty()) {
            double lastY = messagesYPos.getLast() + msgGap + trailing;
            messagesYPos.add(lastY);
            lifeEventYPos.add(lastY);
        }
    }

    /**
     * Calculates extra vertical space needed for multi-line message labels or notes.
     */
    private int getExtra(SequenceModel model, int index) {
        SequenceMessage msg = model.messages.get(index);
        int lines = msg.getMessage().split("<br>").length;
        int noteLines = 0;
        if (msg.getNotes() != null) {
            for (SequenceNote note : msg.getNotes()) {
                noteLines = Math.max(noteLines, note.getLabel().split("<br>").length);
            }
        }

        int totalLines = Math.max(lines, noteLines);
        return Math.max(0, (totalLines - 1) * SequenceFactoryContext.lineHeight);
    }

    /**
     * Records the Y position for a life event, applying an offset for self-call activations.
     */
    private void addLifeEventY(SequenceModel model, List<Double> lifeEventYPos,
                               SequenceMessage msg, double y, int i) {
        boolean isStartOfLifeEvent = msg.isSelf() &&
                model.participants.stream()
                        .anyMatch(p -> p.getLifeEvents().stream()
                                .anyMatch(e -> e.getStartMessage() == i));
        lifeEventYPos.add(isStartOfLifeEvent ? y + 15 : y);
    }

    /**
     * Renders a diagram with just an error message when parsing fails
     */
    private void errorHandler(Optional<ErrorMessage> error) {
        List<GModelElement> elements = new ArrayList<>();
        elements.add(error.get().buildError());

        GGraph newGModel = new GGraphBuilder()
                .id("sequence-diagram")
                .addAll(elements)
                .build();

        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }
}