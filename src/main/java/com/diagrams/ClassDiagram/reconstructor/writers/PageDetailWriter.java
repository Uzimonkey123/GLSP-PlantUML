/*
 * File: EntityWriter.java
 * Author: Norman Babiak
 * Description: Writes modified page details back to source
 * Date: 31.3.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.ArrayList;
import java.util.List;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class PageDetailWriter {

    private final WriterContext ctx;

    public PageDetailWriter(WriterContext ctx) {
        this.ctx = ctx;
    }

    public void write() {
        ClassModel model = ctx.getModel();

        if (model.titleModified) {
            ctx.changeLine(model.titleLineStart, model.titleLineEnd,
                    buildPageDetail("title", model.title, model.titleLineStart, model.titleLineEnd));
        }

        if (model.headerModified) {
            ctx.changeLine(model.headerLineStart, model.headerLineEnd,
                    buildPageDetail("header", model.header, model.headerLineStart, model.headerLineEnd));
        }

        if (model.footerModified) {
            ctx.changeLine(model.footerLineStart, model.footerLineEnd,
                    buildPageDetail("footer", model.footer, model.footerLineStart, model.footerLineEnd));
        }
    }

    private List<String> buildPageDetail(String keyword, String content,
                                         int startLine, int endLine) {
        List<String> lines = new ArrayList<>();
        String indent = extractIndentation(ctx.getSourceLines().get(startLine));

        // If the original occupied multiple lines, preserve multi-line form
        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            lines.add(applyIndentation(keyword, indent));

            for (String part : content.split("<br>")) {
                lines.add(applyIndentation(part, indent));
            }

            lines.add(applyIndentation("end " + keyword, indent));

        } else {
            // Single-line form, convert <br> back to PlantUML's \n escape
            String text = content.replace("<br>", "\\n");
            lines.add(applyIndentation(keyword + " " + text, indent));
        }

        return lines;
    }
}
