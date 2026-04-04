/*
 * File: EngloberWriter.java
 * Author: Norman Babiak
 * Description: Writes englober declaration lines
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceEnglober;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;

import java.util.List;

public class EngloberWriter {
    private final SequenceWriterContext ctx;

    public EngloberWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    public void write() {
        for (SequenceEnglober englober : ctx.getModel().englobers) {
            if (!englober.isModified()) continue;

            ctx.changeLine(englober.getSourceLineStart(), englober.getSourceLineStart(), List.of(replaceEnglober(englober)));
        }
    }

    private String replaceEnglober(SequenceEnglober englober) {
        StringBuilder sb = new StringBuilder("box ");
        String source = englober.getRawSourceText();

        sb.append("\"").append(englober.getLabel()).append("\"");
        if (!englober.getColor().equals("#CCCCCC")) {
            sb.append(" ").append(englober.getColor());
        }

        return ctx.indented(sb.toString(), source);
    }
}
