/*
 * File: PlantUMLModelState.java
 * Author: Norman Babiak
 * Description: Interface for model state, holding additionally source URI
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.state;

public interface PlantUMLModelState<M> {
    M getModel();
    void setModel(M modelType);

    void setSourceUri(String uri);
    String getSourceUri();
}
