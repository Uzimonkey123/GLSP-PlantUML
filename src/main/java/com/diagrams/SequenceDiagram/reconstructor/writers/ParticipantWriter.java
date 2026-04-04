/*
 * File: ParticipantWriter.java
 * Author: Norman Babiak
 * Description: Writes participant declarations, life event keywords and update message lines with changes
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;
import com.diagrams.SequenceDiagram.utils.ReconstructorHelper;

import java.util.List;

public class ParticipantWriter {
    private final SequenceWriterContext ctx;

    public ParticipantWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    public void write() {
        for (SequenceNode node : ctx.getModel().participants) {
            if (!node.isModified()) continue;

            if (node.hasLine()) {
                ctx.changeLine(node.getSourceLineStart(), node.getSourceLineEnd(),
                        List.of(replaceParticipant(node)));
            }

            updateConnectedMessages(node);
            updateLifeEventKeywords(node);
        }
    }

    /**
     * Updates message lines that reference a renamed participant.
     */
    private void updateConnectedMessages(SequenceNode node) {
        MessageWriter msgWriter = new MessageWriter(ctx);

        for (SequenceMessage message : ctx.getModel().messages) {
            if (message.hasLine() && isConnected(message, node)) {
                ctx.changeLine(message.getSourceLineStart(), message.getSourceLineEnd(),
                        List.of(msgWriter.replaceMessage(message)));
            }

            for (SequenceNote note : message.getNotes()) {
                new NoteWriter(ctx).writeNote(message, note);
            }
        }
    }

    private boolean isConnected(SequenceMessage message, SequenceNode node) {
        return (message.getFrom() != null && message.getFrom().equals(node)) ||
                (message.getTo() != null && message.getTo().equals(node));
    }

    /**
     * Updates standalone life event keyword lines
     */
    private void updateLifeEventKeywords(SequenceNode node) {
        for (SequenceLifeEvent event : node.getLifeEvents()) {
            if (!event.hasLine()) continue;

            int startLine = event.getSourceLineStart();
            int endLine = event.getSourceLineEnd();

            LineMapper.LineInfo startInfo = ctx.getLineMap().getLineInfo(startLine);
            if (startInfo == null) continue;

            if (startInfo.type == LineMapper.LineType.DESTROY) {
                ctx.changeLine(startLine, endLine, List.of(replaceDestroy(node, event)));
            } else {
                ctx.changeLine(startLine, startLine, List.of(replaceActivate(node, event)));
            }

            if (startLine != endLine) {
                ctx.changeLine(endLine, endLine, List.of(replaceDeactivate(node, event)));
            }
        }
    }

    private String replaceParticipant(SequenceNode node) {
        StringBuilder sb = new StringBuilder();

        String source = node.getRawSourceText();
        boolean isCreate = ctx.getLineType(node.getSourceLineStart()) == LineMapper.LineType.CREATE;

        if (isCreate) {
            sb.append("create ");
            if (!node.getType().equals("PARTICIPANT")) {
                sb.append(node.getType().toLowerCase()).append(" ");
            }

        } else {
            sb.append(node.getType().toLowerCase()).append(" ");
        }

        String alias = ReconstructorHelper.extractAlias(source);

        if (alias != null && !alias.isEmpty()) {
            String name = node.getName();
            if (!name.matches("[a-zA-Z0-9_]+")) {
                sb.append("\"").append(name).append("\"");

            } else {
                sb.append(name);
            }
            sb.append(" as ").append(alias);

        } else {
            sb.append(ReconstructorHelper.getParticipant(node));
        }

        if (node.getOrder() != 0) {
            sb.append(" order ").append(node.getOrder());
        }

        if (!node.getBackground().equals("#5d4949")) {
            sb.append(" ").append(node.getBackground());
        }

        return ctx.indented(sb.toString(), source);
    }

    private String replaceDestroy(SequenceNode node, SequenceLifeEvent event) {
        String content = "destroy " + ReconstructorHelper.getParticipant(node);
        return ctx.indented(content, event.getRawSourceText());
    }

    private String replaceActivate(SequenceNode node, SequenceLifeEvent event) {
        String content = "activate " + ReconstructorHelper.getParticipant(node) + " " + event.getBackground();
        return ctx.indented(content, event.getRawSourceText());
    }

    private String replaceDeactivate(SequenceNode node, SequenceLifeEvent event) {
        String content = "deactivate " + ReconstructorHelper.getParticipant(node);
        return ctx.indented(content, event.getRawSourceText());
    }
}
