/*
 * File: SequenceEventHandlerContext.java
 * Author: Norman Babiak
 * Description: Context facade for sequence event handlers
 * Date: 2.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.LineFinder;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import com.diagrams.SequenceDiagram.reconstructor.SourceElement;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.sequencediagram.*;

import java.util.*;

public class SequenceEventHandlerContext {

    private final SequenceModel model;
    private final LineMapper lineMapper;
    private final LineFinder lineFinder;
    private final SequenceDiagram sequenceDiagram;

    // Activation tracking participant name - stack of start indices / colors / line numbers
    private final Map<String, Stack<Integer>> activationStacks = new HashMap<>();
    private final Map<String, Stack<HColor>> activationColorStacks = new HashMap<>();
    private final Map<String, Stack<Integer>> activationLineStacks = new HashMap<>();

    private final Map<GroupingStart, SequenceGroup> groupStack = new HashMap<>();

    // Anchor tracking
    private int anchorCounter = 0;
    private final Stack<String> anchorIdStack = new Stack<>();

    public SequenceEventHandlerContext(SequenceModel model, LineMapper lineMapper,
                                       LineFinder lineFinder, SequenceDiagram sequenceDiagram) {
        this.model = model;
        this.lineMapper = lineMapper;
        this.lineFinder = lineFinder;
        this.sequenceDiagram = sequenceDiagram;
    }

    public SequenceModel getModel() {
        return model;
    }

    public LineMapper getLineMapper() {
        return lineMapper;
    }

    public LineFinder getLineFinder() {
        return lineFinder;
    }

    public SequenceDiagram getSequenceDiagram() {
        return sequenceDiagram;
    }

    /**
     * Maps an element to a single source line.
     */
    public void addMapperInfo(SourceElement element, int lineNum) {
        if (lineNum >= 0) {
            element.setSourceLines(lineNum, lineNum);
            LineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    /**
     * Maps an element to a source line range.
     */
    public void addMapperInfo(SourceElement element, int startLine, int endLine) {
        if (startLine >= 0) {
            element.setSourceLines(startLine, endLine);
            LineMapper.LineInfo info = lineMapper.getLineInfo(startLine);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    /**
     * Resolves a node to the corresponding internal SequenceNode.
     */
    public SequenceNode getSequenceNode(Participant participant) {
        if (participant == null) return null;

        String rawName = String.join("<br>", participant.getDisplay(false));
        return model.participants.stream()
                .filter(p -> p.getName().equals(rawName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Processes notes attached to a message.
     */
    public void handleMessageNotes(AbstractMessage msg, SequenceMessage message) {
        for (Note note : msg.getNoteOnMessages()) {
            String id = "note-" + model.notes.size();
            String label = String.join("<br>", note.getDisplay());
            String position = note.getPosition().toString();
            String shape = note.getNoteStyle().toString();
            String color = note.getColors().getColor(ColorType.BACK) == null
                    ? "#FFFFE0"
                    : note.getColors().getColor(ColorType.BACK).asString();

            SequenceNote newNote = new SequenceNote(id, label, position, color, shape);

            int startLine = lineFinder.findNoteLine(null, note);
            int endLine = startLine;

            if (startLine >= 0) {
                LineMapper.LineInfo info = lineMapper.getLineInfo(startLine);
                if (info != null && !info.originalText.contains(":")) {
                    endLine = lineFinder.findNoteEndLine(note);
                }
            }

            addMapperInfo(newNote, startLine, endLine);
            message.addNotes(newNote);
            model.notes.add(newNote);
        }
    }

    /**
     * Processes a standalone note event
     */
    public void processSeparateNote(Note note, boolean parallel) {
        String id = "msg-note-" + model.messages.size();
        SequenceNode from = getSequenceNode(note.getParticipant());
        SequenceNode to = getSequenceNode(note.getParticipant2());

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

        int startLine = lineFinder.findNoteLine(null, note);
        int endLine = startLine;

        if (startLine >= 0) {
            LineMapper.LineInfo info = lineMapper.getLineInfo(startLine);
            if (info != null && !info.originalText.contains(":")) {
                endLine = lineFinder.findNoteEndLine(note);
            }
        }

        addMapperInfo(newNote, startLine, endLine);

        if (parallel) {
            msg.setParallel(true);
            newNote.setParalell(true);
        }

        model.notes.add(newNote);
    }

    /**
     * Handles anchor start/end markers on messages
     */
    public void setupAnchor(Message msg, SequenceNode from, SequenceNode to) {
        if (msg.getAnchor().equals("start")) {
            String anchorId = "anchor-" + anchorCounter++;
            model.messages.getLast().setAnchorStart(true);
            model.messages.getLast().setAnchorId(anchorId);
            anchorIdStack.push(anchorId);

        } else if (msg.getAnchor().equals("end")) {
            if (anchorIdStack.isEmpty()) return;

            String anchorId = anchorIdStack.pop();
            model.messages.getLast().setAnchorEnd(true);
            model.messages.getLast().setAnchorId(anchorId);

            String anchorLabel = sequenceDiagram.getLinkAnchors().get(model.anchors.size()).getMessage();
            SequenceAnchor anchor = new SequenceAnchor(from, to, anchorId, anchorLabel);

            int anchorLine = lineFinder.findAnchorLine("start", anchor);
            if (anchorLine >= 0) {
                anchor.setSourceLines(anchorLine, anchorLine);
                LineMapper.LineInfo info = lineMapper.getLineInfo(anchorLine);

                if (info != null) {
                    anchor.setRawSourceText(info.originalText);
                }
            }

            model.anchors.add(anchor);
        }
    }

    /**
     * Ensures activation stacks exist for a given participant name.
     */
    public void initActivationStacks(String participant) {
        activationStacks.putIfAbsent(participant, new Stack<>());
        activationColorStacks.putIfAbsent(participant, new Stack<>());
        activationLineStacks.putIfAbsent(participant, new Stack<>());
    }

    public Stack<Integer> getActivationStack(String participant) {
        return activationStacks.get(participant);
    }

    public Stack<HColor> getActivationColorStack(String participant) {
        return activationColorStacks.get(participant);
    }

    public Stack<Integer> getActivationLineStack(String participant) {
        return activationLineStacks.get(participant);
    }

    public Map<String, Stack<Integer>> getAllActivationStacks() {
        return activationStacks;
    }

    public Map<String, Stack<HColor>> getAllActivationColorStacks() {
        return activationColorStacks;
    }

    /**
     * Registers a GroupingStart with its corresponding internal SequenceGroup.
     */
    public void registerGroup(GroupingStart groupStart, SequenceGroup group) {
        groupStack.put(groupStart, group);
    }

    /**
     * Retrieves the internal SequenceGroup for a GroupingStart.
     */
    public SequenceGroup getGroup(GroupingStart groupStart) {
        return groupStack.get(groupStart);
    }

    /**
     * Generates the next sequential message ID based on current message count.
     */
    public String nextMessageId() {
        return "msg-" + model.messages.size();
    }
}