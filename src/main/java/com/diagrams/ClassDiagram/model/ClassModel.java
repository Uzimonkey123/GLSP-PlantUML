package com.diagrams.ClassDiagram.model;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassModel {
    public List<ClassEntity> entities = new ArrayList<>();
    public List<ClassLink> links = new ArrayList<>();
    public List<Package> packages = new ArrayList<>();
    public List<ClassLabel> labels = new ArrayList<>();
    public List<ClassEntity> notes = new ArrayList<>();

    public String footer;
    public String header;
    public String title;

    public int titleLineStart  = -1;
    public int titleLineEnd    = -1;
    public int headerLineStart = -1;
    public int headerLineEnd   = -1;
    public int footerLineStart = -1;
    public int footerLineEnd   = -1;

    public boolean titleModified  = false;
    public boolean headerModified = false;
    public boolean footerModified = false;

    private ClassLineMapper lineMapper;
    private ClassLineFinder lineFinder;

    private boolean isLeftToRight = false;

    private final List<int[]> linesToDelete = new ArrayList<>();

    public ClassModel() {}

    public void setLineFinder(ClassLineFinder lineFinder) {
        this.lineFinder = lineFinder;
    }

    public ClassLineFinder getLineFinder() {
        return lineFinder;
    }

    public void setMapper(ClassLineMapper lineMapper) {
        this.lineMapper = lineMapper;
    }

    public ClassLineMapper getLineMapper() {
        return lineMapper;
    }

    public boolean isLeftToRight() {
        return isLeftToRight;
    }

    public void setLeftToRight(boolean leftToRight) {
        isLeftToRight = leftToRight;
    }

    public ClassEntity getClassEntity(String name) {
        return entities.stream()
                .filter(entity -> entity.getName() != null && entity.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public ClassEntity getClassEntityById(String id) {
        return entities.stream()
                .filter(entity -> entity.getId() != null && entity.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassLabel getClassLabelById(String id) {
        return labels.stream()
                .filter(label -> label.getLabelId() != null && label.getLabelId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassEntity getClassNoteById(String id) {
        return notes.stream()
                .filter(note -> note.getId() != null && note.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassLink getLinkById(String id) {
        return links.stream()
                .filter(link -> link.getLinkId() != null && link.getLinkId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void markLinesForDeletion(int start, int end) {
        linesToDelete.add(new int[]{start, end});
    }

    public List<int[]> getLinesToDelete() {
        return linesToDelete;
    }

    public void clearLinesToDelete() {
        linesToDelete.clear();
    }

    public void relocateAllElements(String newSourceText) {
        this.lineMapper = new ClassLineMapper(newSourceText, this);
        Map<Object, Integer> elementToLineMap = new HashMap<>();
        this.lineFinder = new ClassLineFinder(lineMapper, elementToLineMap);

        lineFinder.setPosition(0);
        for (ClassEntity entity : entities) {
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

        lineFinder.setPosition(0);
        for (ClassLink link : links) {
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

        lineFinder.setPosition(0);
        Set<String> processedNoteIds = new HashSet<>();
        for (ClassEntity note : notes) {
            if (processedNoteIds.contains(note.getId())) {
                continue;
            }

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

        lineFinder.setPosition(0);
        for (Package pkg : packages) {
            int line = lineFinder.findPackageLine(pkg.getName(), pkg);
            if (line >= 0) {
                pkg.setSourceLines(line, line);
                pkg.setRawSourceText(lineMapper.getLineInfo(line).originalText);

            } else {
                pkg.setSourceLines(-1, -1);
            }

            pkg.clearModified();
        }

        titleLineStart = lineFinder.findTitleLine();
        titleLineEnd = lineFinder.findEndTitleLine();
        if (titleLineEnd < 0) titleLineEnd = titleLineStart;

        headerLineStart = lineFinder.findHeaderLine();
        headerLineEnd = lineFinder.findEndHeaderLine();
        if (headerLineEnd < 0) headerLineEnd = headerLineStart;

        footerLineStart = lineFinder.findFooterLine();
        footerLineEnd = lineFinder.findEndFooterLine();
        if (footerLineEnd < 0) footerLineEnd = footerLineStart;

        titleModified = false;
        headerModified = false;
        footerModified = false;

        linesToDelete.clear();
    }

    private int findBlockEnd(int startLine) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        if (startLine < 0 || startLine >= all.size()) {
            return startLine;
        }

        String startText = all.get(startLine).originalText.trim();

        if (!startText.contains("{")) {
            return startLine;
        }

        if (startText.contains("}")) {
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