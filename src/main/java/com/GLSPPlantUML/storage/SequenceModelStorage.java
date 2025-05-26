package com.GLSPPlantUML.storage;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.state.SequenceModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

public class SequenceModelStorage extends AbstractPlantUMLStorage<SequenceModel, SequenceModelState> {

    @Override
    public void loadSourceModel(RequestModelAction action) {
        super.loadSourceModel(action);
        SequenceModel model = modelState.getModel();

        // Debug prints
        System.err.println("Participants:");
        System.err.println(model.participants);

        System.err.println("Messages");
        System.err.println(model.messages);
    }

    @Override
    public void saveSourceModel(SaveModelAction action) {

    }
}
