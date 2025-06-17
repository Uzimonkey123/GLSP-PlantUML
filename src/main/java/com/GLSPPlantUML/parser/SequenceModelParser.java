package com.GLSPPlantUML.parser;

import com.GLSPPlantUML.model.SequenceModel;
import com.google.inject.Inject;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.sequencediagram.*;
import net.sourceforge.plantuml.skin.ArrowConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
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

                // Check for complete diagram related attributes
                handleHeader(model, sd);
                handleFooter(model, sd);
                handleTitle(model, sd);

                // Record all participants, even if unused
                Collection<Participant> participants = sd.participants();
                for (Participant participant : participants) {
                    ParticipantHandler(participant, model);
                }

                // Extract participants and messages with API
                for (Event event : sd.events()) {
                    if (event instanceof MessageExo msg) {
                        MessageExoHandler(msg, model);
                    }

                    if (event instanceof Message msg) {
                        MessageHandler(msg, model);
                    }

                    if(event instanceof Delay delay) {
                        DelayHandler(delay, model);
                    }

                    if (event instanceof Divider div) {
                        DividerHandler(div, model);
                    }
                }
            }
        }
        return model;
    }

    private void ParticipantHandler(Participant participant, SequenceModel model) {
        String name = participant.getDisplay(false).get(0).toString();
        String type = participant.getType().toString();
        int order = participant.getOrder();
        HColor background = participant.getColors().getColor(ColorType.BACK);

        if(!hasParticipant(name, model)) {
            addParticipants(model.participants, new SequenceModel.SequenceNode(name, type, order, background));
        }
    }

    private void MessageExoHandler(MessageExo msg, SequenceModel model) {
        // TODO
        MessageExoType type = msg.getType();
        String from, to;
        if (type.equals(MessageExoType.FROM_LEFT) || type.equals(MessageExoType.FROM_RIGHT)) {
            from = "[";
            to = msg.getParticipant().toString();
        } else {
            from = msg.getParticipant().toString();
            to = "]";
        }

        String num = msg.getMessageNumber();
        if(num == null) {
            num = "";
        }

        String label = String.join(" ", msg.getLabel());

        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();

        model.messages.add(new SequenceModel.SequenceMessage(from, to, label, arrowConfig, "edge", num, false));
        System.err.println("Message: " + from + " -> " + to);
    }

    private void MessageHandler(Message msg, SequenceModel model) {
        String from = msg.getParticipant1().getDisplay(false).get(0).toString();

        String to = msg.getParticipant2().getDisplay(false).get(0).toString();

        String num = msg.getMessageNumber();

        String label = String.join(" ", msg.getLabel());
        if(num == null) {
            num = "";
        }

        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();
        boolean isSelf = msg.isSelfMessage();

        // Record message
        model.messages.add(new SequenceModel.SequenceMessage(from, to, label, arrowConfig, "edge", num, false, isSelf));
    }

    private void DelayHandler(Delay delay, SequenceModel model) {
        String label = delay.getText().get(0).toString();

        model.messages.add(new SequenceModel.SequenceMessage(null, null, label, null, "edge:delay"));
    }

    private void DividerHandler(Divider div, SequenceModel model) {
        String label = div.getText().get(0).toString();

        model.messages.add(new SequenceModel.SequenceMessage(null, null, label, null, "edge:divider"));
    }

    private boolean hasParticipant(String name, SequenceModel model) {
        for (SequenceModel.SequenceNode node : model.participants) {
            if (node.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void addParticipants(List<SequenceModel.SequenceNode> participants, SequenceModel.SequenceNode node) {
        for (int i = 0; i < participants.size(); i++) {
            int existingOrder = participants.get(i).getOrder();
            if (node.getOrder() < existingOrder) {
                participants.add(i, node);
                return;
            }
        }

        // Add to last position
        participants.add(node);
    }

    void handleTitle(SequenceModel model, SequenceDiagram diagram) {
        if (diagram.getTitle() != null
                && diagram.getTitle().getDisplay() != null
                && diagram.getTitle().getDisplay().size() > 0) {
            model.title = diagram.getTitle().getDisplay().get(0).toString();
        } else {
            model.title = "";
        }
    }

    void handleHeader(SequenceModel model, SequenceDiagram diagram) {
        if (diagram.getHeader() != null
                && diagram.getHeader().getDisplay() != null
                && diagram.getHeader().getDisplay().size() > 0) {
            model.header = diagram.getHeader().getDisplay().get(0).toString();
        } else {
            model.header = "";
        }
    }

    void handleFooter(SequenceModel model, SequenceDiagram diagram) {
        if (diagram.getFooter() != null
                && diagram.getFooter().getDisplay() != null
                && diagram.getFooter().getDisplay().size() > 0) {
            model.footer = diagram.getFooter().getDisplay().get(0).toString();
        } else {
            model.footer = "";
        }
    }
}

