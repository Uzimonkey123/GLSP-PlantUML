/*
 * File: SequenceModelStorage.java
 * Author: Norman Babiak
 * Description: Storage for the model, handles loading and saving of the diagram
 * Date: 4.2.2026
 */

package com.diagrams.SequenceDiagram.storage;

import com.GLSPPlantUML.storage.AbstractPlantUMLStorage;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriter;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

            File file = new File(URI.create(sourceUri));
            String newSource = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            model.relocateAllElements(newSource);

        } catch (IOException e) {
            System.err.println("Error: Failed to save model: " + e.getMessage());
            e.printStackTrace();
        }
    }
}