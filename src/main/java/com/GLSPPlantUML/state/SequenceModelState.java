package com.GLSPPlantUML.state;

import com.GLSPPlantUML.utils.ErrorMessage;
import com.GLSPPlantUML.model.PlantUMLModelState;
import com.GLSPPlantUML.model.SequenceModel;
import com.google.inject.Inject;
import org.eclipse.glsp.server.model.DefaultGModelState;

import java.util.Optional;

public class SequenceModelState extends DefaultGModelState
        implements PlantUMLModelState<SequenceModel> {

    @Inject
    private SequenceModel model = new SequenceModel();
    private Optional<String> uri;
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

    public void setUri(Optional<String> uri) {
        this.uri = uri;
    }

    public Optional<String> getUri() {
        return uri;
    }
}
