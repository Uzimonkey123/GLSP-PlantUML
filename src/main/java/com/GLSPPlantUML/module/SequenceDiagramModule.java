package com.GLSPPlantUML.module;

import com.GLSPPlantUML.PlantUMLDiagramConfiguration;
import com.GLSPPlantUML.factory.SequenceModelFactory;
import com.GLSPPlantUML.handlers.SetDirtyStateHandler;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.parser.PlantUMLParser;
import com.GLSPPlantUML.parser.SequenceModelParser;
import com.GLSPPlantUML.state.SequenceModelState;
import com.GLSPPlantUML.storage.SequenceModelStorage;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.eclipse.glsp.server.actions.ActionHandler;
import org.eclipse.glsp.server.di.MultiBinding;
import org.eclipse.glsp.server.diagram.DiagramConfiguration;
import org.eclipse.glsp.server.features.core.model.GModelFactory;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.model.GModelState;

public class SequenceDiagramModule extends DiagramModule {

    @Override
    public String getDiagramType() {
        return "sequence-diagram";
    }

    @Override
    protected Class<? extends DiagramConfiguration> bindDiagramConfiguration() {
        return PlantUMLDiagramConfiguration.class;
    }

    @Override
    protected Class<? extends SourceModelStorage> bindSourceModelStorage() {
        System.err.println("SequenceDiagramModule.bindSourceModelStorage");
        return SequenceModelStorage.class;
    }

    @Override
    protected Class<? extends GModelFactory> bindGModelFactory() {
        return SequenceModelFactory.class;
    }

    @Override
    protected Class<? extends GModelState> bindGModelState() {
        return SequenceModelState.class;
    }

    @Override
    protected void configureActionHandlers(MultiBinding<ActionHandler> mb) {
        super.configureActionHandlers(mb);
        mb.add(SetDirtyStateHandler.class); // Suppressing warning about dirty state, since not using edit-mode
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
    public SequenceModel provideSequenceModel(SequenceModelState modelState) {
        return modelState.getModel();
    }
}
