package com.GLSPPlantUML.module;

import com.GLSPPlantUML.SequenceDiagramConfiguration;
import com.GLSPPlantUML.handlers.CustomLabelEdit;
import com.GLSPPlantUML.handlers.IgnoreComputeBoundsHandler;
import com.GLSPPlantUML.validators.SequenceLabelValidator;
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
import org.eclipse.glsp.server.features.core.model.ComputedBoundsActionHandler;
import org.eclipse.glsp.server.features.core.model.GModelFactory;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.features.directediting.LabelEditValidator;
import org.eclipse.glsp.server.model.GModelState;
import org.eclipse.glsp.server.operations.OperationHandler;

public class SequenceDiagramModule extends DiagramModule {

    @Override
    public String getDiagramType() {
        return "sequence-diagram";
    }

    @Override
    protected Class<? extends DiagramConfiguration> bindDiagramConfiguration() {
        return SequenceDiagramConfiguration.class;
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

        mb.remove(ComputedBoundsActionHandler.class);
        mb.add(IgnoreComputeBoundsHandler.class);
    }

    @Override
    protected void configureOperationHandlers(MultiBinding<OperationHandler<?>> mb) {
        super.configureOperationHandlers(mb);
        mb.add(CustomLabelEdit.class);
    }

    @Override
    protected Class<? extends LabelEditValidator> bindLabelEditValidator() {
        return SequenceLabelValidator.class;
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
