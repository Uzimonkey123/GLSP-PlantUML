package com.diagrams.ClassDiagram.storage;

import com.GLSPPlantUML.storage.AbstractPlantUMLStorage;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassWriter;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

import java.io.IOException;

public class ClassModelStorage extends AbstractPlantUMLStorage<ClassModel, ClassModelState> {

    @Override
    public void loadSourceModel(RequestModelAction action) {
        super.loadSourceModel(action);
        ClassModel model = modelState.getModel();
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
            ClassWriter writer = new ClassWriter(model, sourceUri);
            writer.write();

            // Clear modification flags
            model.entities.forEach(ClassEntity::clearModified);
            model.links.forEach(ClassLink::clearModified);
            model.packages.forEach(Package::clearModified);

        } catch (IOException e) {
            System.err.println("Error: Failed to save model: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
