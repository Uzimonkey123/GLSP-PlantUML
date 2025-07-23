package com.GLSPPlantUML.factory.SequenceParts;

import com.GLSPPlantUML.builders.NoteBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.model.SequenceParts.SequenceNote;
import com.GLSPPlantUML.utils.NotePosition;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;
import java.util.Map;

public class SequenceNoteFactory {
    private final SequenceModel model;
    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<GModelElement> elements;

    private final NoteBuild noteBuild;

    private final int labelHeight = 14;
    private final int noteXOffset = 5;
    private final int padding = 10;
    private final int singleYOffset = 6;

    public SequenceNoteFactory(SequenceModel model, List<Double> messagesYPos,
                               Map<String, Double> centre, Map<String, Double> halfWidth,
                               List<GModelElement> elements) {
        this.model = model;
        this.messagesYPos = messagesYPos;
        this.centre = centre;
        this.halfWidth = halfWidth;
        this.elements = elements;

        this.noteBuild = new NoteBuild();
    }

    public void createNote(SequenceMessage msg, int msgIndex) {
        if (msg.getNotes() == null || msg.getNotes().isEmpty()) return;

        for (SequenceNote note : msg.getNotes()) {
            double width = WidthCalculator.calculateWidth(note.getLabel(), padding);
            String from = msg.getFrom();
            String to = msg.getTo();

            // Exo message note position override
            String position = resolveNotePosition(from, to, note.getPosition());

            NotePosition notePos = switch(position) {
                case "OVER", "OVER_SEVERAL" -> noteOver(from, to, width);
                case "RIGHT" -> noteRight(from, to, width);
                case "LEFT" -> noteLeft(from, to, width);
                default -> throw new IllegalStateException("Unexpected value: " + position);
            };

            double x1 = notePos.x1();
            double x2 = notePos.x2();
            String source = notePos.source();
            String target = notePos.target();

            if (to != null && !(to.equals("]") || to.equals("["))) {
                SequenceNode toNode = model.getNode(msg.getTo());
                if (toNode.isCreatedNode()) {
                    x1 += halfWidth.get(msg.getTo());
                    x2 += halfWidth.get(msg.getTo());
                }
            }

            int lineCount = note.getLabel().split("<br>").length;
            double labelYOffset = lineCount > 1 ? lineCount * labelHeight : singleYOffset;
            double noteYOffset = lineCount > 1 ? lineCount * labelHeight : labelHeight;
            double labelYPos = messagesYPos.get(msgIndex) - labelYOffset;

            double y1 = messagesYPos.get(msgIndex) - noteYOffset;
            double y2 = messagesYPos.get(msgIndex) + noteXOffset;

            elements.add(noteBuild.buildNote(note, source, target, x1, x2, y1, y2));
            elements.add(noteBuild.buildNoteLabel(note, x1, x2, labelYPos));
        }
    }

    private String resolveNotePosition(String from, String to, String defaultPosition) {
        if ("[".equals(from)) return "RIGHT";
        if ("]".equals(from)) return "LEFT";
        if ("[".equals(to)) return "RIGHT";
        if ("]".equals(to)) return "LEFT";
        return defaultPosition;
    }

    private NotePosition noteOver(String from, String to, double width) {
        NotePosition notePos;

        if (from == null && to == null) { // Across all
            String firstId = model.participants.getFirst().getId();
            String lastId = model.participants.getLast().getId();
            notePos = new NotePosition(centre.get(firstId) - noteXOffset,
                                        centre.get(lastId) + noteXOffset,
                                        firstId, lastId);

        } else if (to == null) { // Over from
            double center = centre.get(from);
            notePos = new NotePosition(center - width / 2,
                                        center + width / 2,
                                        from, from);

        } else { // Across defined ones
            double fromX = centre.get(from);
            double toX = centre.get(to);
            notePos = new NotePosition(Math.min(fromX, toX) - noteXOffset,
                                        Math.max(fromX, toX) + noteXOffset,
                                        from, to);
        }

        return notePos;
    }

    private NotePosition noteRight(String from, String to, double width) {
        double rightX;

        if (from == null) {
            rightX = centre.get(to);
        } else if (to == null) {
            rightX = centre.get(from);
        } else {
            rightX = Math.max(centre.get(from), centre.get(to));
        }

        return new NotePosition(rightX + noteXOffset,
                rightX + noteXOffset + width,
                from != null ? from : to,
                from != null ? from : to);
    }

    private NotePosition noteLeft(String from, String to, double width) {
        double leftX;

        if (from == null) {
            leftX = centre.get(to);
        } else if (to == null) {
            leftX = centre.get(from);
        } else {
            leftX = Math.min(centre.get(from), centre.get(to));
        }

        return new NotePosition(leftX - noteXOffset - width,
                leftX - noteXOffset,
                from != null ? from : to,
                from != null ? from : to);
    }
}
