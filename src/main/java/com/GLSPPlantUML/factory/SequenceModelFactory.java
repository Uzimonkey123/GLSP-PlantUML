package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.state.SequenceModelState;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GGraphBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.List;
import java.util.stream.Collectors;

public class SequenceModelFactory implements GModelFactory {

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        SequenceModel model = modelState.getModel();

        // Add participants as nodes to the list
        List<GModelElement> elements = model.participants.stream()
                .map(participant -> new GNodeBuilder("node:participant")
                        .layout("vbox")
                        .add(new GLabelBuilder()
                                .text(participant)
                                .build())
                        .build())
                .collect(Collectors.toList());

        // Add messages as edges with proper text, source and target
        List<GModelElement> edges = model.messages.stream()
                .map(msg -> new GEdgeBuilder("edge:message")
                        .sourceId(msg.getFrom())
                        .targetId(msg.getTo())
                        .add(new GLabelBuilder()
                                .text(msg.getMessage())
                                .build())
                        .build())
                .collect(Collectors.toList());

        elements.addAll(edges);

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
