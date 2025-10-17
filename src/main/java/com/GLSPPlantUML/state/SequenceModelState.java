package com.GLSPPlantUML.state;

import com.GLSPPlantUML.utils.ErrorMessage;
import com.GLSPPlantUML.model.SequenceModel;
import com.google.inject.Inject;
import org.eclipse.glsp.server.model.DefaultGModelState;

import java.util.Optional;

public class SequenceModelState extends DefaultGModelState implements PlantUMLModelState<SequenceModel> {

    @Inject
    private SequenceModel model = new SequenceModel();
    private String sourceUri;
    private ErrorMessage error = null;

    public SequenceModel getModel() {
        return model;
    }

    @Override
    public void setModel(SequenceModel modelType) {
        this.model = modelType;
        this.setRoot(null);
    }

    public Optional<ErrorMessage> getError() {
        return Optional.ofNullable(error);
    }

    public void setError(ErrorMessage error) {
        this.error = error;
    }

    @Override
    public void setSourceUri(String uri) {
        this.sourceUri = uri;
    }

    @Override
    public String getSourceUri() {
        return sourceUri;
    }
}