package com.GLSPPlantUML.storage;

import com.GLSPPlantUML.model.PlantUMLModelState;
import com.GLSPPlantUML.parser.PlantUMLParser;
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

public abstract class AbstractPlantUMLStorage<M, S extends PlantUMLModelState<M>>
        implements SourceModelStorage {
    private static final Logger LOGGER =
            LogManager.getLogger(SequenceModelStorage.class);

    @Inject
    protected S modelState;

    @Inject
    protected PlantUMLParser<M> pumlParser;

    public void loadSourceModel(RequestModelAction action) {
        System.err.println("loadSourceModel");

        Optional<String> uriOpt = MapUtil.getValue(action.getOptions(), "sourceURI");
        if (uriOpt.isEmpty() || uriOpt.get().isBlank()) {
            throw new GLSPServerException(
                    "'sourceURI' missing in options");
        }

        String uriString = uriOpt.get();
        final File file = new File(java.net.URI.create(uriString));

        try {
            M model = pumlParser.parse(file);
            modelState.setModel(model);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
