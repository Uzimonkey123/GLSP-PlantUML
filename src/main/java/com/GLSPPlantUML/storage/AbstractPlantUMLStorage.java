/*
 * File: AbstractPlantUMLStorage.java
 * Author: Norman Babiak
 * Description: Abstract storage for model state, handling loading and saving source model for the PlantUML diagrams
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.storage;

import com.GLSPPlantUML.state.PlantUMLModelState;
import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.SequenceDiagram.storage.SequenceModelStorage;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.types.GLSPServerException;
import org.eclipse.glsp.server.utils.MapUtil;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public abstract class AbstractPlantUMLStorage<M, S extends PlantUMLModelState<M>> implements SourceModelStorage {

    @Inject
    protected S modelState;

    @Inject
    protected PlantUMLParser<M> pumlParser;

    public void loadSourceModel(RequestModelAction action) {
        Optional<String> uriOpt = MapUtil.getValue(action.getOptions(), "sourceUri");
        if (uriOpt.isEmpty() || uriOpt.get().isBlank()) {
            throw new GLSPServerException("'sourceUri' missing in options");
        }

        String uriString = uriOpt.get();
        modelState.setSourceUri(uriString);
        final File file = new File(java.net.URI.create(uriString));

        try {
            M model = pumlParser.parse(file);
            modelState.setModel(model);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
