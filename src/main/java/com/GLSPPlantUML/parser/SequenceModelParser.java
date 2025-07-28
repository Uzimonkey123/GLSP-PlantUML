package com.GLSPPlantUML.parser;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.google.inject.Inject;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.creole.Display;
import net.sourceforge.plantuml.sequencediagram.*;
import net.sourceforge.plantuml.skin.ArrowConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SequenceModelParser implements PlantUMLParser<SequenceModel> {
    private SequenceDiagram sequenceDiagram;

    @Inject
    private SequenceModel model;

    private int anchorCounter = 0; // For counting how many anchors started
    private final Stack<String> anchorIdStack = new Stack<>(); // To keep track of the nesting of anchors

    // Map of Participant name - activate life event to store life event start for deactivation
    private final Map<String, Stack<Integer>> activationStacks = new HashMap<>();
    private final Map<String, Stack<HColor>> activationColorStacks = new HashMap<>();

    // Map for storing leaf group attributes to the corresponding parent group (parent = GroupingStart)
    private final Map<GroupingStart, SequenceGroup> groupStack = new HashMap<>();

    @Inject
    public SequenceModelParser() {}

    public SequenceModel parse(File file) throws IOException {
        // Read .puml text
        System.err.println("We are in the parser");
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (!text.contains("@startuml")) {
            text = "@startuml\n" + text + "\n@enduml";
        }

        // Parse with PlantUML
        SourceStringReader reader = new SourceStringReader(text);
        List<BlockUml> blocks = reader.getBlocks();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();
            if (d instanceof PSystemError) {
                throw new IOException("Error, invalid Sequence diagram.");
            }

            if (d instanceof SequenceDiagram sd) {
                this.sequenceDiagram = sd;

                model.showFoot = sequenceDiagram.isShowFootbox();

                // Check for complete diagram related attributes
                handleHeader();
                handleFooter();
                handleTitle();
                MainframeHandler();

                // Record all participants, even if unused
                Collection<Participant> participants = sd.participants();
                for (Participant participant : participants) {
                    ParticipantHandler(participant);
                }

                // Extract participants and messages with API
                for (Event event : sd.events()) {
                    if (event instanceof GroupingStart gs) GroupingStartHandler(gs);
                    if (event instanceof GroupingLeaf leaf) GroupingLeafHandler(leaf);

                    if (event instanceof MessageExo msg) MessageExoHandler(msg);
                    if (event instanceof Message msg) MessageHandler(msg);
                    if (event instanceof Delay delay) DelayHandler(delay);
                    if (event instanceof Divider div) DividerHandler(div);

                    if (event instanceof LifeEvent le) LifeEventHandler(le);
                    if (event instanceof HSpace hSpace) hSpaceHandler(hSpace);
                    if (event instanceof Reference reference) ReferenceHandler(reference);
                    if (event instanceof Note note) SeparateNoteHandler(note, false);
                    if (event instanceof Notes notes) {
                        List<Note> noteList = notes.asList();

                        // First note is not parallel to keep the same logic as with messages
                        for (int i = 0; i < noteList.size(); i++) {
                            Note note = noteList.get(i);
                            boolean parallel = i > 0;
                            SeparateNoteHandler(note, parallel);
                        }
                    }
                }
                closeLifeEvents();
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
        String id = "par-" + model.participants.size();

        if(!hasParticipant(name)) {
            node = new SequenceNode(id, name, type, order, background, false);
            // Call to check if participant is englobed or not
            EngloberHandler(node, sequenceDiagram.getEnglober(participant));

            addParticipants(model.participants, node);
        }
        
        if (node != null && participant.getStereotype() != null) {
            node.setStereotype(true);
            node.setStereotypeChar(participant.getStereotype().getCharacter());
            if (participant.getStereotype().getHtmlColor() != null) {
                node.setCharColor(participant.getStereotype().getHtmlColor().asString());
            }
        }
    }

    private void EngloberHandler(SequenceNode node, ParticipantEnglober englober) {
        if (englober == null) return;

        for (ParticipantEnglober part : englober.getGenealogy()) {
            String title = String.join("<br>", part.getTitle());
            String id = "englober-" + title;
            String parentId = part.getParent() == null ? null : "englober-" + part.getParent().getTitle().toString().hashCode();
            String color = part.getBoxColor() != null ? part.getBoxColor().asString() : "#CCCCCC";
            int level = part.getGenealogy().size() - 1; // Level indicating depth of the box to set offset

            boolean alreadyAdded = model.englobers.stream().anyMatch(e -> e.getId().equals(id));
            if (!alreadyAdded) {
                model.englobers.add(new SequenceEnglober(id, title, parentId, color, level));
            }

            // Assign the englober ID to the node for further search in factory
            node.addEngloberId(id);
        }
    }

    private void GroupingStartHandler(GroupingStart groupStart) {
        SequenceGroup group = new SequenceGroup(model.messages.size(),
                                                groupStart.getTitle(),
                                                groupStart.getComment(),
                                                groupStart.getLevel());
        groupStack.put(groupStart, group);
        model.groups.add(group);

        if (groupStart.getBackColorGeneral() != null) {
            group.setBackColor(groupStart.getBackColorGeneral().asString());
        }

        if (groupStart.getBackColorElement() != null) {
            group.setElementColor(groupStart.getBackColorElement().asString());
        }
    }

    private void GroupingLeafHandler(GroupingLeaf groupLeaf) {
        GroupingStart parent = groupLeaf.getGroupingStart();
        if (parent != null) {
            SequenceGroup seqGroup = groupStack.get(parent);
            GroupingType type = groupLeaf.getType();
            if (type == GroupingType.END) {
                seqGroup.setEndIndex(model.messages.size());
            }

            if (type == GroupingType.ELSE) {
                seqGroup.addSeparator(model.messages.size());

                if (groupLeaf.getComment() == null) {
                    seqGroup.addSeparatorLabel("");
                } else {
                    seqGroup.addSeparatorLabel(groupLeaf.getComment());
                }
            }
        }
    }

    private void ReferenceHandler(Reference reference) {
        String firstParticipant = parseParticipantId(reference.getParticipant().getFirst());
        String lastParticipant = parseParticipantId(reference.getParticipant().getLast());

        String label = String.join("<br>", reference.getStrings());

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, firstParticipant, lastParticipant, label, null, "edge:ref"));
    }

    private void MessageExoHandler(MessageExo msg) {
        String participant = parseParticipantId(msg.getParticipant());

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

        SequenceMessage message = new SequenceMessage(
                msgId, exoMsg.from(), exoMsg.to(), label, arrowConfig, "edge", num,
                isShort, exoMsg.incoming(), exoMsg.outgoing());

        model.messages.add(message);

        if (msg.isParallel()) {
            message.setParallel(true);
        }

        MessageNoteHandler(msg, message);
        model.invisibleNodes = true;
    }

    private void MessageHandler(Message msg) {
        String from = parseParticipantId(msg.getParticipant1());
        String to = parseParticipantId(msg.getParticipant2());

        String num = msg.getMessageNumber();

        String label = String.join("<br>", msg.getLabel());
        if(num == null) {
            num = "";
        }

        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();
        boolean isSelf = msg.isSelfMessage();

        String msgId = "msg-" + model.messages.size();

        // Record message
        SequenceMessage message = new SequenceMessage(msgId, msg.isCreate(), from, to, label, arrowConfig,
                "edge", num, false, isSelf);
        model.messages.add(message);

        if (msg.isParallel()) {
            message.setParallel(true);
        }

        MessageNoteHandler(msg, message);

        if (msg.getAnchor() != null) {
            setupAnchor(msg, from, to);
        }
    }

    private void MessageNoteHandler(AbstractMessage msg, SequenceMessage message) {
        for(Note note : msg.getNoteOnMessages()) {
            String id = "note-" + model.notes.size();
            String label = String.join("<br>", note.getDisplay());
            String position = note.getPosition().toString();
            String shape = note.getNoteStyle().toString();
            String color = note.getColors().getColor(ColorType.BACK) == null
                            ? "#FFFFE0"
                            : note.getColors().getColor(ColorType.BACK).asString();

            SequenceNote newNote = new SequenceNote(id, label, position, color, shape);
            message.addNotes(newNote);
            model.notes.add(newNote);
        }
    }

    private void SeparateNoteHandler(Note note, boolean parallel) {
        String id = "msg-note-" + model.messages.size();
        String from = parseParticipantId(note.getParticipant());
        String to = parseParticipantId(note.getParticipant2());

        String position = note.getPosition().toString();
        String label = String.join("<br>", note.getDisplay());
        String shape = note.getNoteStyle().toString();
        String color = note.getColors().getColor(ColorType.BACK) == null
                ? "#FFFFE0"
                : note.getColors().getColor(ColorType.BACK).asString();

        SequenceMessage msg = new SequenceMessage(id, from, to, "edge:note");
        model.messages.add(msg);

        SequenceNote newNote = new SequenceNote("note-" + model.notes.size(), label, position, color, shape);
        msg.addNotes(newNote);

        if (parallel) {
            msg.setParallel(true); // For Y offset in factory
            newNote.setParalell(true); // For X offset between nodes in NodeGap
        }

        model.notes.add(newNote);
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
            label = String.join("<br>", delay.getText());
        }

        String msgId = "msg-" + model.messages.size();

        model.messages.add(new SequenceMessage(msgId, null, null, label,
                null, "edge:delay"));
    }

    private void DividerHandler(Divider div) {
        String label = String.join("<br>", div.getText());

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

        if (currentNode == null) {
            throw new RuntimeException("Error setting node for given life event.");
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

                    currentNode.addLifeEvent(lifeEvent);
                }

                // Set the destroy index for the node
                currentNode.setDestroyIndex(index);
            }

            case CREATE -> {
                currentNode.setCreatedNode(true);
                currentNode.setCreatedIndex(model.messages.size());
            }
        }
    }

    private void closeLifeEvents() {
        int lastMsg = model.messages.isEmpty() ? 0 : model.messages.size() - 1;

        activationStacks.forEach((participant, starts) -> {
            Stack<HColor> colors = activationColorStacks.get(participant);

            SequenceNode currentNode = null;

            // Search for node in the participant list
            for (SequenceNode node : model.participants) {
                if (node.getName().equals(participant)) {
                    currentNode = node;
                    break;
                }
            }
            if (currentNode == null) return;

            while (!starts.isEmpty()) {
                int startIndex = starts.pop();
                HColor color  = colors.pop();

                SequenceLifeEvent le = new SequenceLifeEvent(startIndex, lastMsg, color);
                le.setLevel(starts.size());
                currentNode.addLifeEvent(le);
            }
        });
    }

    private boolean hasParticipant(String name) {
        for (SequenceNode node : model.participants) {
            if (node.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private String parseParticipantId(Participant participant) {
        if (participant == null) return null;

        String rawName = String.join("<br>", participant.getDisplay(false));
        return model.participants.stream()
                .filter(p -> p.getName().equals(rawName))
                .findFirst()
                .map(SequenceNode::getId)
                .orElse(rawName);
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
        model.title = "";

        if (sequenceDiagram.getTitle() != null
                && sequenceDiagram.getTitle().getDisplay() != null
                && sequenceDiagram.getTitle().getDisplay().size() > 0) {
            model.title = String.join("<br>", sequenceDiagram.getTitle().getDisplay());
        }
    }

    void handleHeader() {
        model.header = "";

        if (sequenceDiagram.getHeader() != null
                && sequenceDiagram.getHeader().getDisplay() != null
                && sequenceDiagram.getHeader().getDisplay().size() > 0) {
            model.header = String.join("<br>", sequenceDiagram.getHeader().getDisplay());
        }
    }

    void handleFooter() {
        model.footer = "";

        if (sequenceDiagram.getFooter() != null
                && sequenceDiagram.getFooter().getDisplay() != null
                && sequenceDiagram.getFooter().getDisplay().size() > 0) {
            model.footer = String.join("<br>", sequenceDiagram.getFooter().getDisplay());
        }
    }

    void MainframeHandler() {
        model.mainframe = "";

        if (sequenceDiagram.getMainFrame() != null) {
            model.isMainframe = true;
            model.mainframe = String.join("<br>", sequenceDiagram.getMainFrame());
        }
    }

    void hSpaceHandler(HSpace hspace) {
        int gapLength = hspace.getPixel();

        model.messageSpaces.put(model.messages.size(), gapLength);
    }
}

