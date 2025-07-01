package com.GLSPPlantUML.parser;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
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
import java.util.*;

public class SequenceModelParser implements PlantUMLParser<SequenceModel> {
    private SequenceDiagram sequenceDiagram;
    private SequenceModel model;

    private int anchorCounter = 0; // For counting how many anchors started
    private final Stack<String> anchorIdStack = new Stack<>(); // To keep track of the nesting of anchors

    // Map of Participant name - activate life event to store life event start for deactivation
    private final Map<String, Stack<Integer>> activationStacks = new HashMap<>();
    private final Map<String, Stack<HColor>> activationColorStacks = new HashMap<>();

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
        this.model = new SequenceModel();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();
            if (d instanceof SequenceDiagram sd) {
                this.sequenceDiagram = sd;

                model.showFoot = sequenceDiagram.isShowFootbox();

                // Check for complete diagram related attributes
                handleHeader();
                handleFooter();
                handleTitle();

                // Record all participants, even if unused
                Collection<Participant> participants = sd.participants();
                for (Participant participant : participants) {
                    ParticipantHandler(participant);
                }

                // Extract participants and messages with API
                for (Event event : sd.events()) {
                    if (event instanceof MessageExo msg) {
                        MessageExoHandler(msg);
                    }

                    if (event instanceof Message msg) {
                        MessageHandler(msg);
                    }

                    if (event instanceof Delay delay) {
                        DelayHandler(delay);
                    }

                    if (event instanceof Divider div) {
                        DividerHandler(div);
                    }

                    if (event instanceof LifeEvent le) {
                        LifeEventHandler(le);
                    }

                    if (event instanceof HSpace hSpace) {
                        hSpaceHandler(hSpace);
                    }
                }
            }
        }
        return model;
    }

    private void ParticipantHandler(Participant participant) {
        String name = String.join("<br>", participant.getDisplay(false));
        String type = participant.getType().toString();
        int order = participant.getOrder();
        HColor background = participant.getColors().getColor(ColorType.BACK);
        SequenceNode node = null;

        if(!hasParticipant(name)) {
            node = new SequenceNode(name, type, order, background, false);
            addParticipants(model.participants, node);
        }
        
        if (participant.getStereotype() != null) {
            assert node != null;
            node.setStereotype(true);
            node.setStereotypeChar(participant.getStereotype().getCharacter());
            if (participant.getStereotype().getHtmlColor() != null) {
                node.setCharColor(participant.getStereotype().getHtmlColor().asString());
            }
        }
    }

    private void MessageExoHandler(MessageExo msg) {
        String participant = String.join("<br>", msg.getParticipant().getDisplay(false));

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

    private void MessageHandler(Message msg) {
        String from = String.join("<br>", msg.getParticipant1().getDisplay(false));

        String to = String.join("<br>", msg.getParticipant2().getDisplay(false));

        String num = msg.getMessageNumber();

        String label = String.join("<br>", msg.getLabel());
        if(num == null) {
            num = "";
        }

        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();
        boolean isSelf = msg.isSelfMessage();

        String msgId = "msg-" + model.messages.size();

        // Record message
        model.messages.add(new SequenceMessage(msgId, msg.isCreate(), from, to, label, arrowConfig,
                "edge", num, false, isSelf));

        if (msg.getAnchor() != null) {
            setupAnchor(msg, from, to);
        }
    }

    private void setupAnchor(Message msg, String from, String to) {
        if (msg.getAnchor().equals("start")) {
            // For the start of an anchor create ID and save it on stack,
            // + set it up in the last message that it connects to.
            String anchorId = "anchor-" + anchorCounter++;
            model.messages.getLast().setAnchorStart(true);
            model.messages.getLast().setAnchorId(anchorId);
            anchorIdStack.push(anchorId);

        } else if (msg.getAnchor().equals("end")) {
            // If it comes to end and stack is empty, error
            if (anchorIdStack.isEmpty()) {
                throw new RuntimeException("Empty anchor stack in parser. Start anchor point missing!");
            }

            // Get the anchor ID from stack and set up the last message as end holder
            String anchorId = anchorIdStack.pop();
            model.messages.getLast().setAnchorEnd(true);
            model.messages.getLast().setAnchorId(anchorId);

            // Get the message from the diagram linked anchors and create the anchor in the model list
            String anchorLabel = sequenceDiagram.getLinkAnchors().get(model.anchors.size()).getMessage();
            model.anchors.add(new SequenceAnchor(from, to, anchorId, anchorLabel));
        }
    }

    private void DelayHandler(Delay delay) {
        String label = "";
        if(delay.getText() != null
            && delay.getText().size() > 0) {
            label = delay.getText().get(0).toString();
        }

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, null, null, label,
                null, "edge:delay"));
    }

    private void DividerHandler(Divider div) {
        String label = div.getText().get(0).toString();

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, null, null, label,
                null, "edge:divider"));
    }

    private void LifeEventHandler(LifeEvent le) {
        String participant = String.join("<br>", le.getParticipant().getDisplay(false));
        HColor background = le.getSpecificColors().getBackColor();
        SequenceNode currentNode = null;

        // Search for node in the participant list
        for (SequenceNode node : model.participants) {
            if (node.getName().equals(participant)) {
                currentNode = node;
                break;
            }
        }

        // Initialize stack for this participant
        activationStacks.putIfAbsent(participant, new Stack<>());
        activationColorStacks.putIfAbsent(participant, new Stack<>());

        Stack<Integer> messageStack = activationStacks.get(participant);
        Stack<HColor> colorStack = activationColorStacks.get(participant);

        int index = model.messages.size() - 1;

        switch (le.getType()) {
            case ACTIVATE -> {
                messageStack.push(index); // Save current index
                colorStack.push(background); // Save the color
            }

            case DEACTIVATE -> {
                if (messageStack.isEmpty()) return;

                int startIndex = messageStack.pop(); // Last start for participant is ending first, on top of stack
                HColor color = colorStack.pop();

                // Add life event to the list
                SequenceLifeEvent lifeEvent = new SequenceLifeEvent(startIndex, index, color);
                // Set the depth of the life event for offset in factory
                lifeEvent.setLevel(messageStack.size());

                assert currentNode != null;
                currentNode.addLifeEvent(lifeEvent);
            }

            case DESTROY -> {
                if (!messageStack.isEmpty()) {
                    int startIndex = messageStack.pop();
                    HColor color = colorStack.pop();

                    // Add life event to the list
                    SequenceLifeEvent lifeEvent = new SequenceLifeEvent(startIndex, index, color);
                    // Set the depth of the life event for offset in factory
                    lifeEvent.setLevel(messageStack.size());

                    assert currentNode != null;
                    currentNode.addLifeEvent(lifeEvent);
                }

                // Set the destroy index for the node
                assert currentNode != null;
                currentNode.setDestroyIndex(index);
            }

            case CREATE -> {
                assert currentNode != null;
                currentNode.setCreatedNode(true);
                currentNode.setCreatedIndex(model.messages.size());
            }
        }
    }

    private boolean hasParticipant(String name) {
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

    void handleTitle() {
        if (sequenceDiagram.getTitle() != null
                && sequenceDiagram.getTitle().getDisplay() != null
                && sequenceDiagram.getTitle().getDisplay().size() > 0) {
            model.title = sequenceDiagram.getTitle().getDisplay().get(0).toString();
        } else {
            model.title = "";
        }
    }

    void handleHeader() {
        if (sequenceDiagram.getHeader() != null
                && sequenceDiagram.getHeader().getDisplay() != null
                && sequenceDiagram.getHeader().getDisplay().size() > 0) {
            model.header = sequenceDiagram.getHeader().getDisplay().get(0).toString();
        } else {
            model.header = "";
        }
    }

    void handleFooter() {
        if (sequenceDiagram.getFooter() != null
                && sequenceDiagram.getFooter().getDisplay() != null
                && sequenceDiagram.getFooter().getDisplay().size() > 0) {
            model.footer = sequenceDiagram.getFooter().getDisplay().get(0).toString();
        } else {
            model.footer = "";
        }
    }

    void hSpaceHandler(HSpace hspace) {
        int gapLength = hspace.getPixel();

        model.messageSpaces.put(model.messages.size(), gapLength);
    }
}

