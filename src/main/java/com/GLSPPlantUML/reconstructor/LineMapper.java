package com.GLSPPlantUML.reconstructor;

import java.util.ArrayList;
import java.util.List;

public class LineMapper {
    private final List<LineInfo> lineInfos;

    public LineMapper(String sourceText) {
        this.lineInfos = new ArrayList<>();
        String[] lines = sourceText.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            // For every line determine type and save the object
            lineInfos.add(new LineInfo(i, lines[i]));
        }
    }

    public List<LineInfo> getLineInfos() {
        return lineInfos;
    }

    public LineInfo getLineInfo(int lineNumber) {
        if (lineNumber >= 0 && lineNumber < lineInfos.size()) {
            return lineInfos.get(lineNumber);
        }

        return null;
    }

    public static class LineInfo {
        public final int lineNumber;
        public final String originalText;
        public final String trimmedText;
        public final LineType type;

        public LineInfo(int lineNumber, String originalText) {
            this.lineNumber = lineNumber;
            this.originalText = originalText;
            this.trimmedText = originalText.trim();
            this.type = determineType(trimmedText);
        }

        private LineType determineType(String trimmed) {
            if (trimmed.isEmpty()) return LineType.EMPTY;

            switch (trimmed) {
                case "end ref" -> {
                    return LineType.END_REFERENCE;
                }
                case "end box" -> {
                    return LineType.END_ENGLOBER;
                }
                case "end title" -> {
                    return LineType.END_TITLE;
                }
                case "end header" -> {
                    return LineType.END_HEADER;
                }
                case "end footer" -> {
                    return LineType.END_FOOTER;
                }
                case "end" -> {
                    return LineType.GROUP_END;
                }
            }

            if (trimmed.contains("{") && trimmed.contains("}") && trimmed.contains("<->")) {
                return LineType.ANCHOR;
            }

            // Get first word for switch
            String firstWord = trimmed.split("\\s+")[0].toLowerCase();
            if (firstWord.contains("#")) {
                firstWord = firstWord.substring(0, firstWord.indexOf("#"));
            }

            return switch (firstWord) {
                case "'", "/'" -> LineType.COMMENT;
                case "@startuml" -> LineType.START_UML;
                case "@enduml" -> LineType.END_UML;
                case "participant", "actor", "boundary", "control",
                     "entity", "database", "collections" -> LineType.PARTICIPANT;
                case "ref" -> LineType.REFERENCE;
                case "alt", "opt", "loop", "par", "break", "critical", "group" -> LineType.GROUP_START;
                case "else" -> LineType.GROUP_ELSE;
                case "activate" -> LineType.ACTIVATE;
                case "deactivate" -> LineType.DEACTIVATE;
                case "destroy" -> LineType.DESTROY;
                case "return" -> LineType.RETURN;
                case "create" -> LineType.CREATE;
                case "note", "hnote", "rnote" -> LineType.NOTE;
                case "endnote", "endhnote", "endrnote" -> LineType.END_NOTE;
                case "header" -> LineType.HEADER;
                case "title" -> LineType.TITLE;
                case "footer" -> LineType.FOOTER;
                case "mainframe" -> LineType.MAINFRAME;
                case "box" -> LineType.ENGLOBER;
                default -> {
                    if (trimmed.equals("end note")) yield LineType.END_NOTE;
                    if (trimmed.startsWith("==") && trimmed.endsWith("==")) yield LineType.DIVIDER;
                    if (trimmed.startsWith("...") && trimmed.endsWith("...")) yield LineType.DELAY;
                    if (containsArrow(trimmed)) yield LineType.MESSAGE;
                    yield LineType.UNKNOWN;
                }
            };
        }

        private boolean containsArrow(String line) {
            String arrowPattern =
                    "(\\?|\\[|[ox])?" +
                    "[-\\\\/.]+" +
                    "(\\[#[^\\]]+\\])?" +
                    "[><]+" +
                    "(\\?|\\]|[ox])?";

            return line.matches(".*" + arrowPattern + ".*");
        }
    }

    public enum LineType {
        EMPTY,
        COMMENT,
        START_UML,
        END_UML,
        PARTICIPANT, // TODO: Fix alias, spots
        MESSAGE,
        DIVIDER,
        DELAY,
        REFERENCE,
        END_REFERENCE,
        GROUP_START,
        GROUP_ELSE,
        GROUP_END,
        ANCHOR,
        ACTIVATE, // TODO Participant name rewrite
        DEACTIVATE, // TODO
        DESTROY, // TODO
        RETURN,
        CREATE,
        NOTE, // TODO
        END_NOTE, // TODO
        HEADER,
        END_HEADER,
        TITLE,
        END_TITLE,
        FOOTER,
        END_FOOTER,
        ENGLOBER,
        END_ENGLOBER,
        MAINFRAME,
        UNKNOWN
    }
}