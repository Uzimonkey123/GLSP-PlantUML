/*
 * File: SequenceNoteFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating note boxes and labels attached to messages or standing alone
 * Date: 7.5.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.NoteBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNote;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;
import java.util.Map;

import static com.diagrams.SequenceDiagram.factory.SequenceFactoryContext.*;

public class SequenceNoteFactory {
    private record NotePosition(double x1, double x2, String source, String target) {

        public NotePosition {
            // Swap if later participant is defined first
            if (x2 < x1) {
                double temp = x1;
                x1 = x2;
                x2 = temp;
            }
        }
    }

    private final SequenceModel model;
    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<GModelElement> elements;

    private final NoteBuild noteBuild;

    private final int noteXOffset = 5;
    private final int singleYOffset = 6;

    public SequenceNoteFactory(SequenceFactoryContext ctx) {
        this.noteBuild = new NoteBuild();

        model = ctx.getModel();
        centre = ctx.getCentre();
        halfWidth = ctx.getHalfWidth();
        messagesYPos = ctx.getMessagesYPos();
        elements = ctx.getElements();
    }

    /**
     * Creates note rectangles and labels for all notes on a message
     */
    public void createNote(SequenceMessage msg, int msgIndex) {
        if (msg.getNotes() == null || msg.getNotes().isEmpty()) return;

        for (SequenceNote note : msg.getNotes()) {
            double width = WidthCalculator.calculateWidth(note.getLabel(), defPadding);
            SequenceNode from = msg.getFrom();
            SequenceNode to = msg.getTo();

            String fromId = from != null ? from.getId() : null;
            String toId = to != null ? to.getId() : null;

            // Exo message note position override
            String position = resolveNotePosition(fromId, toId, note.getPosition());

            NotePosition notePos = switch (position) {
                case "OVER", "OVER_SEVERAL" -> noteOver(fromId, toId, width);
                case "RIGHT" -> noteRight(fromId, toId, width);
                case "LEFT" -> noteLeft(fromId, toId, width);
                default -> throw new IllegalStateException("Unexpected value: " + position);
            };

            double x1 = notePos.x1();
            double x2 = notePos.x2();
            String source = notePos.source();
            String target = notePos.target();

            if (to != null && !(toId.equals("]") || toId.equals("["))) {
                if (to.isCreatedNode()) {
                    x1 += halfWidth.get(toId);
                    x2 += halfWidth.get(toId);
                }
            }

            int lineCount = note.getLabel().split("<br>").length;
            double labelYOffset = lineCount > 1 ? lineCount * lineHeight : singleYOffset;
            double noteYOffset = lineCount > 1 ? lineCount * lineHeight : lineHeight;
            double labelYPos = messagesYPos.get(msgIndex) - labelYOffset;

            double y1 = messagesYPos.get(msgIndex) - noteYOffset;
            double y2 = messagesYPos.get(msgIndex) + noteXOffset;

            elements.add(noteBuild.buildNote(msg.getMsgId(), note, source, target, x1, x2, y1, y2));
            elements.add(noteBuild.buildNoteLabel(note, x1, x2, labelYPos));
        }
    }

    /**
     * Resolves note position for external messages, overriding to LEFT/RIGHT as needed
     */
    private String resolveNotePosition(String from, String to, String defaultPosition) {
        if ("[".equals(from)) return "RIGHT";
        if ("]".equals(from)) return "LEFT";
        if ("[".equals(to)) return "RIGHT";
        if ("]".equals(to)) return "LEFT";
        return defaultPosition;
    }

    /**
     * Positions a note centered over one or spanning two participants
     */
    private NotePosition noteOver(String from, String to, double width) {
        if (from == null && to == null) { // Across all
            String firstId = model.participants.getFirst().getId();
            String lastId = model.participants.getLast().getId();
            double x1 = centre.get(firstId);
            double x2 = centre.get(lastId);

            if (width > x2 - x1) {
                double middleX = (x1 + x2) / 2;
                x1 = middleX - width / 2;
                x2 = middleX + width / 2;
            }

            return new NotePosition(x1 - noteXOffset, x2 + noteXOffset, firstId, lastId);

        } else if (to == null) { // Over from
            double center = centre.get(from);
            return new NotePosition(center - width / 2, center + width / 2, from, from);

        } else { // Across defined ones
            double fromX = centre.get(from);
            double toX = centre.get(to);
            double x1 = Math.min(fromX, toX);
            double x2 = Math.max(fromX, toX);

            if (width > x2 - x1) {
                double middleX = (x1 + x2) / 2;
                x1 = middleX - width / 2;
                x2 = middleX + width / 2;
            }

            return new NotePosition(x1 - noteXOffset, x2 + noteXOffset, from, to);
        }
    }

    /**
     * Positions a note to the right of the rightmost participant
     */
    private NotePosition noteRight(String from, String to, double width) {
        double rightX;

        if (from == null) {
            rightX = centre.get(to);
        } else if (to == null) {
            rightX = centre.get(from);
        } else {
            rightX = Math.max(centre.get(from), centre.get(to));
        }

        return new NotePosition(rightX + noteXOffset, rightX + noteXOffset + width,
                from != null ? from : to, from != null ? from : to);
    }

    /**
     * Positions a note to the left of the leftmost participant
     */
    private NotePosition noteLeft(String from, String to, double width) {
        double leftX;

        if (from == null) {
            leftX = centre.get(to);
        } else if (to == null) {
            leftX = centre.get(from);
        } else {
            leftX = Math.min(centre.get(from), centre.get(to));
        }

        return new NotePosition(leftX - noteXOffset - width, leftX - noteXOffset,
                from != null ? from : to, from != null ? from : to);
    }
}
