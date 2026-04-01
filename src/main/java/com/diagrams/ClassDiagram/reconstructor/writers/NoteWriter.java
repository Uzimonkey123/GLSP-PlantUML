/*
 * File: NoteWriter.java
 * Author: Norman Babiak
 * Description: Writes modified notes back to source
 * Date: 31.3.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class NoteWriter {

    private final WriterContext ctx;

    public NoteWriter(WriterContext ctx) {
        this.ctx = ctx;
    }

    public void write() {
        for (ClassEntity note : ctx.getModel().notes) {
            if (!note.isModified()) continue;
            if (!note.hasLine()) continue;

            int start = note.getSourceLineStart();
            int end = note.getSourceLineEnd();

            ctx.changeLine(start, end, (start != end) ? rebuildMultilineNote(note) : rebuildSingleLineNote(note));
        }
    }

    private List<String> rebuildSingleLineNote(ClassEntity note) {
        String source = ctx.getEffectiveLine(note.getSourceLineStart());
        String indent = extractIndentation(source);
        String trimmed = source.trim();

        List<String> lines = new ArrayList<>();

        if (trimmed.matches("(?i)^note\\s+\".*\"\\s+as\\s+.*$")) {
            String newText = note.getName().replace("<br>", "\\n");
            String updatedLine = trimmed.replaceAll("\".*?\"", "\"" + Matcher.quoteReplacement(newText) + "\"");
            lines.add(applyIndentation(updatedLine, indent));

        } else if (trimmed.contains(":")) {
            String header = trimmed.substring(0, trimmed.indexOf(":")).trim();
            String content = note.getName().replaceAll("(\r?\n)+$", "").replace("<br>", "\\n");
            lines.add(applyIndentation(header + ": " + content, indent));

        } else {
            lines.add(applyIndentation(trimmed, indent));
        }

        return lines;
    }

    private List<String> rebuildMultilineNote(ClassEntity note) {
        String headerSource = ctx.getEffectiveLine(note.getSourceLineStart());
        String indent = extractIndentation(headerSource);
        String trimmedHeader = headerSource.trim();

        List<String> lines = new ArrayList<>();
        lines.add(applyIndentation(trimmedHeader, indent));

        for (String part : note.getName().split("<br>")) {
            lines.add(applyIndentation(part, indent));
        }

        lines.add(applyIndentation("end note", indent));
        return lines;
    }
}
