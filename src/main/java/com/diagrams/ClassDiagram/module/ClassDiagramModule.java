/*
 * File: ClassDiagramModule.java
 * Author: Norman Babiak
 * Description: Main module to connect together all parts of the diagram
 * Date: 4.2.2026
 */

package com.diagrams.ClassDiagram.module;

import com.GLSPPlantUML.handlers.IgnoreComputeBoundsHandler;
import com.GLSPPlantUML.handlers.SetDirtyStateHandler;
import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.ClassDiagram.ClassDiagramConfiguration;
import com.diagrams.ClassDiagram.factory.ClassModelFactory;
import com.diagrams.ClassDiagram.handler.ClassDeleteHandler;
import com.diagrams.ClassDiagram.handler.ClassLabelEditHandler;
import com.diagrams.ClassDiagram.handler.ChangeBoundsHandler;
import com.diagrams.ClassDiagram.handler.ChangeRoutingPointsHandler;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.parser.ClassModelParser;
import com.diagrams.ClassDiagram.state.ClassModelState;
import com.diagrams.ClassDiagram.storage.ClassModelStorage;
import com.google.inject.TypeLiteral;
import org.eclipse.glsp.server.actions.ActionHandler;
import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.di.MultiBinding;
import org.eclipse.glsp.server.diagram.DiagramConfiguration;
import org.eclipse.glsp.server.features.core.model.ComputedBoundsActionHandler;
import org.eclipse.glsp.server.features.core.model.GModelFactory;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.model.GModelState;
import org.eclipse.glsp.server.operations.OperationHandler;

public class ClassDiagramModule extends DiagramModule {

    @Override
    public String getDiagramType() {
        return "class-diagram";
    }

    @Override
    protected Class<? extends DiagramConfiguration> bindDiagramConfiguration() {
        return ClassDiagramConfiguration.class;
    }

    @Override
    protected Class<? extends SourceModelStorage> bindSourceModelStorage() {
        System.err.println("ClassDiagramModule.bindSourceModelStorage");
        return ClassModelStorage.class;
    }

    @Override
    protected Class<? extends GModelFactory> bindGModelFactory() {
        return ClassModelFactory.class;
    }

    @Override
    protected Class<? extends GModelState> bindGModelState() {
        return ClassModelState.class;
    }

    @Override
    protected void configureActionHandlers(MultiBinding<ActionHandler> mb) {
        super.configureActionHandlers(mb);
        mb.add(SetDirtyStateHandler.class); // Suppressing warning about dirty state, since not using edit-mode

        mb.remove(ComputedBoundsActionHandler.class);
        mb.add(IgnoreComputeBoundsHandler.class);
    }

    @Override
    protected void configureOperationHandlers(MultiBinding<OperationHandler<?>> mb) {
        super.configureOperationHandlers(mb);

        mb.add(ChangeBoundsHandler.class);
        mb.add(ChangeRoutingPointsHandler.class);
        mb.add(ClassLabelEditHandler.class);
        mb.add(ClassDeleteHandler.class);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(new TypeLiteral<PlantUMLParser<ClassModel>>() {})
                .to(ClassModelParser.class);
    }
}
