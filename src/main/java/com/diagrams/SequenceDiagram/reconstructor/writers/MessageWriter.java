/*
 * File: MessageWriter.java
 * Author: Norman Babiak
 * Description: Writes message lines including regular messages, delays, dividers, references, and return statements.
 *              Also reconstructs arrow syntax.
 * Date: 7.5.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNote;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;
import com.diagrams.SequenceDiagram.utils.ReconstructorHelper;

import java.util.List;

public class MessageWriter {
    private final SequenceWriterContext ctx;

    public MessageWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Writes all modified messages and their attached notes.
     */
    public void write() {
        NoteWriter noteWriter = new NoteWriter(ctx);

        for (SequenceMessage message : ctx.getModel().messages) {
            if (message.hasLine() && message.isModified()) {
                ctx.changeLine(message.getSourceLineStart(), message.getSourceLineEnd(),
                        List.of(replaceMessage(message)));
            }

            for (SequenceNote note : message.getNotes()) {
                if (!note.isModified()) continue;
                noteWriter.writeNote(message, note);
            }
        }
    }

    /**
     * Reconstructs a message line from its model state.
     */
    public String replaceMessage(SequenceMessage message) {
        String sourceText = message.getRawSourceText();
        String type = message.getType();

        return switch (type) {
            case "edge:delay" ->
                    ctx.indented("..." + message.getMessage() + "...", sourceText);
            case "edge:divider" ->
                    ctx.indented("==" + message.getMessage() + "==", sourceText);
            case "edge:ref" ->
                    ctx.indented(buildReference(message), sourceText);
            default -> {
                if (ctx.getLineType(message.getSourceLineStart()) == LineMapper.LineType.RETURN) {
                    yield ctx.indented("return " + message.getMessage(), sourceText);
                }

                yield ctx.indented(buildRegularMessage(message), sourceText);
            }
        };
    }

    /**
     * Builds a regular message line with participant tokens, arrow syntax, and label
     */
    private String buildRegularMessage(SequenceMessage message) {
        StringBuilder sb = new StringBuilder();

        if (message.isParallel()) sb.append("& ");
        if (message.isAnchorStart()) sb.append("{start} ");
        if (message.isAnchorEnd()) sb.append("{end} ");

        // Source participant
        if (message.getFrom() != null) {
            sb.append(ReconstructorHelper.getParticipant(message.getFrom())).append(" ");

        } else if (message.getFromId().equals("[")) {
            sb.append(message.isShort() ? "?" : "[");
        }

        sb.append(buildArrow(message));

        // Target participant
        if (message.getTo() != null) {
            sb.append(" ");
            sb.append(ReconstructorHelper.getParticipant(message.getTo())).append(" ");

        } else if (message.getToId().equals("]")) {
            sb.append(message.isShort() ? "?" : "]").append(" ");
        }

        sb.append(ReconstructorHelper.extractLifeEventSymbol(message.getRawSourceText()));

        if(!message.getMessage().isEmpty()) {
            sb.append(": ");
            sb.append(message.getMessage());
        }

        return sb.toString();
    }

    /**
     * Builds a "ref over" line, using block syntax for multi-line content
     */
    private String buildReference(SequenceMessage message) {
        StringBuilder sb = new StringBuilder("ref over ");

        if (message.getFrom() != null) {
            sb.append(ReconstructorHelper.getParticipant(message.getFrom()));
        }

        if (message.getTo() != null && !message.getFrom().equals(message.getTo())) {
            sb.append(", ");
            sb.append(ReconstructorHelper.getParticipant(message.getTo()));
        }

        String msgText = message.getMessage();
        if (msgText != null && msgText.contains("<br>")) {
            sb.append("\n");

            for (String line : msgText.split("<br>")) {
                sb.append("  ").append(line).append("\n");
            }
            sb.append("end ref");

        } else {
            sb.append(" : ").append(msgText);
        }

        return sb.toString();
    }

    /**
     * Reconstructs the arrow syntax from head, decoration, and color properties
     */
    private String buildArrow(SequenceMessage message) {
        StringBuilder sb = new StringBuilder();

        if (message.getStartDecor().equals("circle")) sb.append("o");

        sb.append(buildArrowHead(message.getStartHead(), message.getStartPart(), true));

        sb.append(message.isDotted() ? "--" : "-");
        String color = message.getColor();
        if (color != null && !color.equals("black")) {
            sb.append("[").append(color).append("]");
        }

        sb.append(buildArrowHead(message.getEndHead(), message.getEndPart(), false));

        if (message.getEndDecor().equals("circle")) sb.append("o");

        return sb.toString();
    }

    /**
     * Builds one end of an arrow
     */
    private String buildArrowHead(String head, String part, boolean isStart) {
        return switch (part) {
            case "full" -> switch (head) {
                case "block" -> isStart ? "<" : ">";
                case "open" -> isStart ? "<<" : ">>";
                case "cross" -> "x";
                default -> "";
            };
            case "bottom" -> switch (head) {
                case "open" -> "//";
                case "block" -> "/";
                default -> "";
            };
            case "top" -> switch (head) {
                case "open" -> "\\\\";
                case "block" -> "\\";
                default -> "";
            };
            default -> "";
        };
    }
}
