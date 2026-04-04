/*
 * File: GroupWriter.java
 * Author: Norman Babiak
 * Description: Writes group start lines and else-separator lines for modified groups.
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceGroup;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;

import java.util.List;

public class GroupWriter {
    private final SequenceWriterContext ctx;

    public GroupWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Writes all modified groups, updating the start keyword line and all else-separator lines.
     */
    public void write() {
        for (SequenceGroup group : ctx.getModel().groups) {
            if (!group.isModified()) continue;

            ctx.changeLine(group.getSourceLineStart(), group.getSourceLineStart(),
                    List.of(replaceGroupStart(group)));

            List<Integer> separatorLines = group.getSeparatorLineNumbers();
            List<String> separatorLabels = group.getSeparatorLabel();

            for (int i = 0; i < separatorLines.size(); i++) {
                int line = separatorLines.get(i);
                String label = separatorLabels.get(i);
                String sourceLine = ctx.getSourceLines().get(line);

                ctx.changeLine(line, line, List.of(replaceGroupElse(label, sourceLine)));
            }
        }
    }

    private String replaceGroupStart(SequenceGroup group) {
        StringBuilder sb = new StringBuilder();
        String source = group.getRawSourceText();

        if (group.isGroup()) {
            sb.append("group");
            if (!group.getElementColor().equals("grey")) sb.append(group.getElementColor());
            if (!group.getBackColor().equals("none")) sb.append(" ").append(group.getBackColor());
            sb.append(" ").append(group.getLabel());

            if (group.getComment() != null && !group.getComment().isEmpty()) {
                sb.append(" [").append(group.getComment()).append("]");
            }

        } else {
            sb.append(group.getLabel());
            if (!group.getElementColor().equals("grey")) sb.append(group.getElementColor());
            if (!group.getBackColor().equals("none")) sb.append(" ").append(group.getBackColor());

            if (group.getComment() != null && !group.getComment().isEmpty()) {
                sb.append(" ").append(group.getComment());
            }
        }

        return ctx.indented(sb.toString(), source);
    }

    private String replaceGroupElse(String label, String source) {
        StringBuilder sb = new StringBuilder("else");

        if (label != null && !label.isEmpty()) {
            sb.append(" ").append(label);
        }

        return ctx.indented(sb.toString(), source);
    }
}
