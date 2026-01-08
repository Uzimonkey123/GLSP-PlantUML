package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassEntityFactory;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassLinkFactory;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.state.ClassModelState;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GGraphBuilder;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.ArrayList;
import java.util.List;

public class ClassModelFactory implements GModelFactory {

    @Inject
    protected ClassModelState modelState;
    protected ClassModel model;
    private final List<GModelElement> elements = new ArrayList<>();

    @Override
    public void createGModel() {

        model = modelState.getModel();
        EntityBuild entityBuild = new EntityBuild();

        ClassEntityFactory entityFactory = new ClassEntityFactory(model, entityBuild, elements);
        entityFactory.createEntities();

        ClassLinkFactory linkFactory = new ClassLinkFactory(model, elements);
        linkFactory.createLinks();

        GGraph newGModel = new GGraphBuilder()
                .id("class-diagram")
                .addAll(elements)
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }
}
