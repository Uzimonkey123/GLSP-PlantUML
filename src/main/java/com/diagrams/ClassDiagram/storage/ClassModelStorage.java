/*
 * File: ClassModelStorage.java
 * Author: Norman Babiak
 * Description: Storage for the model, handles loading and saving of the diagram
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.storage;

import com.GLSPPlantUML.storage.AbstractPlantUMLStorage;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.reconstructor.ClassWriter;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ClassModelStorage extends AbstractPlantUMLStorage<ClassModel, ClassModelState> {

    @Override
    public void loadSourceModel(RequestModelAction action) {
        super.loadSourceModel(action);
    }

    @Override
    public void saveSourceModel(SaveModelAction action) {
        try {
            String sourceUri = modelState.getSourceUri();
            if (sourceUri == null || sourceUri.isEmpty()) {
                System.err.println("Error save: No source URI");

                return;
            }

            ClassModel model = modelState.getModel();
            // In case of save of the file, rewrites changed contents into the file
            ClassWriter writer = new ClassWriter(model, sourceUri);
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