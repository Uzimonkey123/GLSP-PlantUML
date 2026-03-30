/*
 * File: ClassModelState.java
 * Author: Norman Babiak
 * Description: Stores state of the model for the whole relation of the connected client
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.state;

import com.GLSPPlantUML.state.PlantUMLModelState;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.GLSPPlantUML.utils.ErrorMessage;
import com.google.inject.Inject;
import org.eclipse.glsp.server.model.DefaultGModelState;

import java.util.Optional;

public class ClassModelState extends DefaultGModelState implements PlantUMLModelState<ClassModel> {

    @Inject
    private ClassModel model = new ClassModel();
    private String sourceUri;
    private ErrorMessage error = null; // In case of error, stores the message and the built-in ready GLabel

    public ClassModel getModel() {
        return model;
    }

    @Override
    public void setModel(ClassModel modelType) {
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
