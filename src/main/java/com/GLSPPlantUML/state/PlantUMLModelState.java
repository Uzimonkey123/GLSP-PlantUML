package com.GLSPPlantUML.state;

public interface PlantUMLModelState<M> {
    M getModel();
    void setModel(M modelType);

    void setSourceUri(String uri);
    String getSourceUri();
}
