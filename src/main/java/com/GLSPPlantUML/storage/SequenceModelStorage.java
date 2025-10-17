package com.GLSPPlantUML.storage;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.reconstructor.SequenceWriter;
import com.GLSPPlantUML.state.SequenceModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

import java.io.IOException;

public class SequenceModelStorage extends AbstractPlantUMLStorage<SequenceModel, SequenceModelState> {

    @Override
    public void loadSourceModel(RequestModelAction action) {
        super.loadSourceModel(action);
        SequenceModel model = modelState.getModel();
    }

    @Override
    public void saveSourceModel(SaveModelAction action) {
        try {
            String sourceUri = modelState.getSourceUri();
            if (sourceUri == null || sourceUri.isEmpty()) {
                System.err.println("Error save: No source URI");
                return;
            }

            SequenceModel model = modelState.getModel();
            SequenceWriter writer = new SequenceWriter(model, sourceUri);
            writer.write();

            // Clear modification flags // TODO: Add rest
            model.participants.forEach(SequenceNode::clearModified);
            model.messages.forEach(SequenceMessage::clearModified);
            model.anchors.forEach(SequenceAnchor::clearModified);
            model.groups.forEach(SequenceGroup::clearModified);

        } catch (IOException e) {
            System.err.println("Error: Failed to save model: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
