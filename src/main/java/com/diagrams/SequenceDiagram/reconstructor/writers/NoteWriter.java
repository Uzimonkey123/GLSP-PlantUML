/*
 * File: NoteWriter.java
 * Author: Norman Babiak
 * Description: Writes note lines for both inline and block notes.
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNote;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;
import com.diagrams.SequenceDiagram.utils.ReconstructorHelper;

import java.util.List;

public class NoteWriter {
    private final SequenceWriterContext ctx;

    public NoteWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Writes a note, choosing between single-line and multi-line format
     */
    public void writeNote(SequenceMessage message, SequenceNote note) {
        int startLine = note.getSourceLineStart();
        int endLine = note.getSourceLineEnd();

        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            // Multi-line notes replace up to endLine - 1 (the "end note" line is kept)
            ctx.changeLine(startLine, endLine - 1, List.of(replaceNote(message, note, true)));

        } else {
            ctx.changeLine(startLine, endLine, List.of(replaceNote(message, note, false)));
        }
    }

    /**
     * Reconstructs a note line from its model state.
     */
    private String replaceNote(SequenceMessage message, SequenceNote note, boolean multiline) {
        StringBuilder sb = new StringBuilder();
        String source = note.getRawSourceText();

        // Parallel prefix
        if (note.isParalell()) {
            sb.append("/ ");
        }

        // Note shape keyword
        appendShape(sb, note.getShape());

        // "across" is a special case with no position/participant logic
        if (source.contains("across")) {
            if (multiline) {
                sb.append("across");

            } else {
                sb.append("across: ");
            }

            appendLabel(sb, note, multiline);
            return ctx.indented(sb.toString(), source);
        }

        // Position keyword
        String position = note.getPosition();
        boolean overSeveral = appendPosition(sb, position);

        // For inline notes on messages (not standalone notes), the label follows directly
        if (!message.getType().equals("edge:note")) {
            if (multiline) {
                sb.append(note.getLabel().replace("<br>", "\n"));

            } else {
                sb.append(": ").append(note.getLabel());
            }
            return ctx.indented(sb.toString(), source);
        }

        // "of" keyword for left/right positioned standalone notes
        if (position.equals("LEFT") || position.equals("RIGHT")) {
            sb.append("of ");
        }

        // Participant references
        sb.append(ReconstructorHelper.getParticipant(message.getFrom()));
        if (overSeveral) {
            sb.append(", ").append(ReconstructorHelper.getParticipant(message.getTo()));
        }

        // Background color
        if (!note.getBackground().equals("#FFFFE0")) {
            sb.append(" ").append(note.getBackground());
        }

        // Separator and label
        if (multiline) {
            sb.append("\n");

        } else {
            sb.append(": ");
        }

        appendLabel(sb, note, multiline);
        return ctx.indented(sb.toString(), source);
    }

    /**
     * Appends the note shape keyword.
     */
    private void appendShape(StringBuilder sb, String shape) {
        switch (shape) {
            case "HEXAGONAL" -> sb.append("hnote ");
            case "BOX" -> sb.append("rnote ");
            case "NORMAL" -> sb.append("note ");
        }
    }

    /**
     * Appends the position keyword and returns whether this is an "over several" note.
     */
    private boolean appendPosition(StringBuilder sb, String position) {
        return switch (position) {
            case "LEFT" -> { sb.append("left "); yield false; }
            case "RIGHT" -> { sb.append("right "); yield false; }
            case "OVER" -> { sb.append("over "); yield false; }
            case "OVER_SEVERAL" -> { sb.append("over "); yield true; }
            default -> false;
        };
    }

    /**
     * Appends the note label, using newlines for multi-line or escaped newlines for single-line.
     */
    private void appendLabel(StringBuilder sb, SequenceNote note, boolean multiline) {
        if (multiline) {
            sb.append(note.getLabel().replace("<br>", "\n"));

        } else {
            sb.append(note.getLabel().replace("<br>", "\\n"));
        }
    }
}
