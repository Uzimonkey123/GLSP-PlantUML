package com.diagrams.ClassDiagram.storage;

import com.GLSPPlantUML.storage.AbstractPlantUMLStorage;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;

public class ClassModelStorage extends AbstractPlantUMLStorage<ClassModel, ClassModelState> {

    @Override
    public void loadSourceModel(RequestModelAction action) {
        super.loadSourceModel(action);
        ClassModel model = modelState.getModel();
    }

    @Override
    public void saveSourceModel(SaveModelAction action) {

    }
}
