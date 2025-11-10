package com.diagrams.ClassDiagram.module;

import com.GLSPPlantUML.handlers.IgnoreComputeBoundsHandler;
import com.GLSPPlantUML.handlers.SetDirtyStateHandler;
import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.ClassDiagram.ClassDiagramConfiguration;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.state.ClassModelState;
import com.diagrams.SequenceDiagram.factory.SequenceModelFactory;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.parser.SequenceModelParser;
import com.diagrams.SequenceDiagram.storage.SequenceModelStorage;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
        return SequenceModelStorage.class;
    }

    @Override
    protected Class<? extends GModelFactory> bindGModelFactory() {
        return SequenceModelFactory.class;
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
    }

    @Override
    protected void configure() {
        super.configure();
        // Adding Parsers into the configuration
        bind(new TypeLiteral<PlantUMLParser<SequenceModel>>() {})
                .to(SequenceModelParser.class);
    }

    @Provides
    @Singleton
    public ClassModel provideSequenceModel(ClassModelState modelState) {
        return modelState.getModel();
    }
}
