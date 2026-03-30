/*
 * File: ElementRelocator.java
 * Author: Norman Babiak
 * Description: Re-maps all model elements to their source line positions after a write.
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;

import java.util.*;

public class ElementRelocator {

    private final ClassModel model;
    private ClassLineMapper lineMapper;

    public ElementRelocator(ClassModel model) {
        this.model = model;
    }

    public void relocateAll(String newSourceText) {
        this.lineMapper = new ClassLineMapper(newSourceText, model);
        model.setMapper(lineMapper);

        Map<Object, Integer> elementToLineMap = new HashMap<>();
        ClassLineFinder lineFinder = new ClassLineFinder(lineMapper, elementToLineMap);
        model.setLineFinder(lineFinder);

        relocateEntities(lineFinder);
        relocateLinks(lineFinder);
        relocateNotes(lineFinder);
        relocatePackages(lineFinder);
        relocatePageElements(lineFinder);

        model.titleModified = false;
        model.headerModified = false;
        model.footerModified = false;

        model.clearLinesToDelete();
    }

    private void relocateEntities(ClassLineFinder lineFinder) {
        lineFinder.setPosition(0);

        for (ClassEntity entity : model.entities) {
            if ("NOTE".equals(entity.getType())) continue;

            String name = entity.getAlias() != null ? entity.getAlias() : entity.getName();
            int startLine = lineFinder.findEntityLine(name, entity);

            if (startLine >= 0) {
                int endLine = findBlockEnd(startLine);
                entity.setSourceLines(startLine, endLine);
                entity.setRawSourceText(lineMapper.getLineInfo(startLine).originalText);

            } else {
                entity.setSourceLines(-1, -1);
            }

            entity.clearModified();
        }
    }

    private void relocateLinks(ClassLineFinder lineFinder) {
        lineFinder.setPosition(0);

        for (ClassLink link : model.links) {
            ClassEntity e1 = link.getEntity1();
            ClassEntity e2 = link.getEntity2();

            boolean isNoteLink = "NOTE".equals(e1.getType()) || "NOTE".equals(e2.getType());
            if (isNoteLink) {
                link.setSourceLines(-1, -1);
                link.clearModified();

                continue;
            }

            String alias1 = e1.getAlias() != null ? e1.getAlias() : e1.getName();
            String alias2 = e2.getAlias() != null ? e2.getAlias() : e2.getName();

            int line = lineFinder.findRelationshipLine(alias1, alias2, link);
            if (line >= 0) {
                link.setSourceLines(line, line);
                link.setRawSourceText(lineMapper.getLineInfo(line).originalText);

            } else {
                link.setSourceLines(-1, -1);
            }

            if (link.hasNoteOnLink()) {
                ClassEntity noteOnLink = link.getNoteOnLink();
                int noteStart = lineFinder.findNoteLine(noteOnLink.getName(), noteOnLink);
                if (noteStart >= 0) {
                    int noteEnd = lineFinder.findNoteEndLine(noteStart, noteOnLink);
                    noteOnLink.setSourceLines(noteStart, noteEnd);
                    noteOnLink.setRawSourceText(lineMapper.getLineInfo(noteStart).originalText);
                }

                noteOnLink.clearModified();
            }

            link.clearModified();
        }
    }

    private void relocateNotes(ClassLineFinder lineFinder) {
        lineFinder.setPosition(0);
        Set<String> processedNoteIds = new HashSet<>();

        for (ClassEntity note : model.notes) {
            if (processedNoteIds.contains(note.getId())) continue;
            processedNoteIds.add(note.getId());

            int startLine = lineFinder.findNoteLine(note.getName(), note);
            if (startLine >= 0) {
                int endLine = lineFinder.findNoteEndLine(startLine, note);
                note.setSourceLines(startLine, endLine);
                note.setRawSourceText(lineMapper.getLineInfo(startLine).originalText);

            } else {
                note.setSourceLines(-1, -1);
            }

            note.clearModified();
        }
    }

    private void relocatePackages(ClassLineFinder lineFinder) {
        lineFinder.setPosition(0);

        for (Package pkg : model.packages) {
            int line = lineFinder.findPackageLine(pkg.getName(), pkg);
            if (line >= 0) {
                pkg.setSourceLines(line, line);
                pkg.setRawSourceText(lineMapper.getLineInfo(line).originalText);

            } else {
                pkg.setSourceLines(-1, -1);
            }

            pkg.clearModified();
        }
    }

    private void relocatePageElements(ClassLineFinder lineFinder) {
        model.titleLineStart = lineFinder.findTitleLine();
        model.titleLineEnd = lineFinder.findEndTitleLine();
        if (model.titleLineEnd < 0) model.titleLineEnd = model.titleLineStart;

        model.headerLineStart = lineFinder.findHeaderLine();
        model.headerLineEnd = lineFinder.findEndHeaderLine();
        if (model.headerLineEnd < 0) model.headerLineEnd = model.headerLineStart;

        model.footerLineStart = lineFinder.findFooterLine();
        model.footerLineEnd = lineFinder.findEndFooterLine();
        if (model.footerLineEnd < 0) model.footerLineEnd = model.footerLineStart;
    }

    /**
     * Finds the closing brace that ends an entity block starting at startLine. Tracks depth for nested blocks
     */
    private int findBlockEnd(int startLine) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        if (startLine < 0 || startLine >= all.size()) {
            return startLine;
        }

        String startText = all.get(startLine).originalText.trim();

        if (!startText.contains("{") || startText.contains("}")) {
            return startLine;
        }

        int depth = 0;
        for (int i = startLine; i < all.size(); i++) {
            for (char c : all.get(i).originalText.toCharArray()) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }

        return startLine;
    }
}