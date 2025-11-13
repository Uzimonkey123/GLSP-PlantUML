package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.state.ClassModelState;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.builder.impl.GGraphBuilder;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

public class ClassModelFactory implements GModelFactory {

    @Inject
    protected ClassModelState modelState;
    protected ClassModel model;

    @Override
    public void createGModel() {
        GGraph newGModel = new GGraphBuilder()
                .id("class-diagram")
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }
}
