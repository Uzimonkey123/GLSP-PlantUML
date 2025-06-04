package com.GLSPPlantUML.parser;

import com.GLSPPlantUML.model.SequenceModel;
import com.google.inject.Inject;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.sequencediagram.*;
import net.sourceforge.plantuml.skin.ArrowConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class SequenceModelParser implements PlantUMLParser<SequenceModel> {
    @Inject
    public SequenceModelParser() {}

    public SequenceModel parse(File file) throws IOException {
        // Read .puml text
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (!text.contains("@startuml")) {
            text = "@startuml\n" + text + "\n@enduml";
        }

        // Parse with PlantUML
        SourceStringReader reader = new SourceStringReader(text);
        List<BlockUml> blocks = reader.getBlocks();
        SequenceModel model = new SequenceModel();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();
            if (d instanceof SequenceDiagram sd) {

                // Extract participants and messages with API
                for (Event event : sd.events()) {
                    if (event instanceof AbstractMessage msg) {
                        String from = msg.getParticipant1().getDisplay(false).get(0).toString();
                        String to = msg.getParticipant2().getDisplay(false).get(0).toString();
                        String label = String.join(" ", msg.getLabel());
                        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();

                        // Record participants
                        if (!model.participants.contains(from)) {
                            model.participants.add(from);
                        }
                        if (!model.participants.contains(to)) {
                            model.participants.add(to);
                        }

                        // Record message
                        model.messages.add(new SequenceModel.SequenceMessage(from, to, label, arrowConfig));
                    }
                }
            }
        }
        return model;
    }
}

