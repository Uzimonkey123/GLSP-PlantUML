/*
 * File: AnchorWriter.java
 * Author: Norman Babiak
 * Description: Writes duration anchor lines, preserving the original brace markers from the source text when available.
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceAnchor;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;

import java.util.List;

public class AnchorWriter {
    private final SequenceWriterContext ctx;

    public AnchorWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Writes all modified anchors.
     */
    public void write() {
        for (SequenceAnchor anchor : ctx.getModel().anchors) {
            if (anchor.hasLine() && anchor.isModified()) {
                ctx.changeLine(anchor.getSourceLineStart(), anchor.getSourceLineEnd(), List.of(replaceAnchor(anchor)));
            }
        }
    }

    /**
     * Reconstructs an anchor line, preserving original brace markers from the source if available
     */
    private String replaceAnchor(SequenceAnchor anchor) {
        String source = anchor.getRawSourceText();

        if (source != null && !source.isEmpty()) {
            int firstBrace = source.indexOf('{');
            int secondBrace = source.indexOf('}', firstBrace);
            int thirdBrace = source.indexOf('{', secondBrace);
            int fourthBrace = source.indexOf('}', thirdBrace);

            if (firstBrace >= 0 && fourthBrace >= 0) {
                String startMarker = source.substring(firstBrace, secondBrace + 1);
                String endMarker = source.substring(thirdBrace, fourthBrace + 1);
                String content = startMarker + " <-> " + endMarker + " : " + anchor.getLabel();

                return ctx.indented(content, source);
            }
        }

        return ctx.indented("{start} <-> {end} : " + anchor.getLabel(), source);
    }
}
