package com.GLSPPlantUML.state;

import com.GLSPPlantUML.model.PlantUMLModelState;
import com.GLSPPlantUML.model.SequenceModel;
import org.eclipse.glsp.server.model.DefaultGModelState;

public class SequenceModelState extends DefaultGModelState
        implements PlantUMLModelState<SequenceModel> {
    private SequenceModel model;

    public SequenceModel getModel() {
        return model;
    }

    @Override
    public void setModel(SequenceModel modelType) {
        this.model = modelType;
        this.setRoot(null);
    }
}
