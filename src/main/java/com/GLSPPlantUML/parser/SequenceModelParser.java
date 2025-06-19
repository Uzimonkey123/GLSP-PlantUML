package com.GLSPPlantUML.parser;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
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

                    if(event instanceof LifeEvent le) {
                        LifeEventHandler(le, model);
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
            addParticipants(model.participants, new SequenceNode(name, type, order, background, false));
        }
    }

    private void MessageExoHandler(MessageExo msg, SequenceModel model) {
        String participant = msg.getParticipant().getDisplay(false).get(0).toString();

        record Direction(String from, String to, boolean incoming, boolean outgoing) {}

        Direction exoMsg = switch (msg.getType()) {
            case FROM_LEFT -> new Direction("[", participant, true, false);
            case FROM_RIGHT -> new Direction("]", participant, true, false);
            case TO_LEFT -> new Direction(participant, "[", false, true);
            case TO_RIGHT -> new Direction(participant, "]", false, true);
        };

        boolean isShort = msg.isShortArrow();
        String num = msg.getMessageNumber();
        if(num == null) {
            num = "";
        }

        String label = String.join(" ", msg.getLabel());
        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(
                msgId, exoMsg.from(), exoMsg.to(), label, arrowConfig, "edge", num,
                isShort, exoMsg.incoming(), exoMsg.outgoing()));
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

        String msgId = "msg-" + model.messages.size();

        // Record message
        model.messages.add(new SequenceMessage(msgId, msg.isCreate(), from, to, label, arrowConfig,
                "edge", num, false, isSelf));
    }

    private void DelayHandler(Delay delay, SequenceModel model) {
        String label = "";
        if(delay.getText() != null
            && delay.getText().size() > 0) {
            label = delay.getText().get(0).toString();
        }

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, null, null, label,
                null, "edge:delay"));
    }

    private void DividerHandler(Divider div, SequenceModel model) {
        String label = div.getText().get(0).toString();

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, null, null, label,
                null, "edge:divider"));
    }

    private void LifeEventHandler(LifeEvent le, SequenceModel model) {
        // Temp code for dealing only with the CREATE life event
        if (le.getType() == LifeEventType.CREATE) {
            String participant = le.getParticipant().getDisplay(false).get(0).toString();
            for(SequenceNode node : model.participants) {
                if(node.getName().equals(participant)) {
                    node.setCreatedNode(true);
                    node.setCreatedIndex(model.messages.size());
                }
            }
        }
    }

    private boolean hasParticipant(String name, SequenceModel model) {
        for (SequenceNode node : model.participants) {
            if (node.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void addParticipants(List<SequenceNode> participants, SequenceNode node) {
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

