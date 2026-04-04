/*
 * File: PageDetailsWriter.java
 * Author: Norman Babiak
 * Description: Writes page-level elements
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor.writers;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.reconstructor.SequenceWriterContext;

import java.util.ArrayList;
import java.util.List;

public class PageDetailsWriter {
    private final SequenceWriterContext ctx;

    public PageDetailsWriter(SequenceWriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Writes all modified page-level elements.
     */
    public void write() {
        SequenceModel model = ctx.getModel();

        if (model.titleModified) {
            ctx.changeLine(model.titleLineStart, model.titleLineEnd,
                    replacePageDetail("title", model.title, model.titleLineStart, model.titleLineEnd));
        }

        if (model.headerModified) {
            ctx.changeLine(model.headerLineStart, model.headerLineEnd,
                    replacePageDetail("header", model.header, model.headerLineStart, model.headerLineEnd));
        }

        if (model.footerModified) {
            ctx.changeLine(model.footerLineStart, model.footerLineEnd,
                    replacePageDetail("footer", model.footer, model.footerLineStart, model.footerLineEnd));
        }

        if (model.mainframeModified) {
            String text = model.mainframe.replace("<br>", "\\n");
            ctx.changeLine(model.mainframeLineNumber, model.mainframeLineNumber, List.of("mainframe " + text));
        }
    }

    /**
     * Reconstructs a page detail element, choosing between block syntax
     */
    private List<String> replacePageDetail(String keyword, String content, int startLine, int endLine) {
        List<String> lines = new ArrayList<>();
        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            lines.add(ctx.indentedFromLine(keyword, startLine));

            for (String line : content.split("<br>")) {
                lines.add(ctx.indentedFromLine(line, startLine));
            }

            lines.add(ctx.indentedFromLine("end " + keyword, startLine));

        } else {
            String text = content.replace("<br>", "\\n");
            lines.add(ctx.indentedFromLine(keyword + " " + text, startLine));
        }

        return lines;
    }
}
