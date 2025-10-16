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

            if (trimmed.equals("end ref")) {
                return LineType.END_REFERENCE;
            }

            if (trimmed.equals("end")) {
                return LineType.GROUP_END;
            }

            if (trimmed.contains("{") && trimmed.contains("}") && trimmed.contains("<->")) {
                return LineType.ANCHOR;
            }

            // Get first word for switch
            String firstWord = trimmed.split("\\s+")[0].toLowerCase();

            return switch (firstWord) {
                case "'", "/*" -> LineType.COMMENT;
                case "@startuml" -> LineType.START_UML;
                case "@enduml" -> LineType.END_UML;
                case "participant", "actor", "boundary", "control",
                     "entity", "database", "collections" -> LineType.PARTICIPANT;
                case "ref" -> LineType.REFERENCE;
                case "alt", "opt", "loop", "par", "break", "critical", "group" -> LineType.GROUP_START;
                case "else" -> LineType.GROUP_ELSE;
                default -> {
                    if (trimmed.startsWith("==") && trimmed.endsWith("==")) yield LineType.DIVIDER;
                    if (trimmed.startsWith("...") && trimmed.endsWith("...")) yield LineType.DELAY;
                    if (containsArrow(trimmed)) yield LineType.MESSAGE;
                    yield LineType.UNKNOWN;
                }
            };
        }

        private boolean containsArrow(String line) {
            String arrowPattern =
                    "[ox]?" +
                    "[-\\\\/.]+" +
                    "[><]+" +
                    "[ox]?";

            return line.matches(".*" + arrowPattern + ".*");
        }
    }

    public enum LineType {
        EMPTY,
        COMMENT,
        START_UML,
        END_UML,
        PARTICIPANT,
        MESSAGE,
        DIVIDER,
        DELAY,
        REFERENCE,
        END_REFERENCE,
        GROUP_START,
        GROUP_ELSE,
        GROUP_END,
        ANCHOR,
        UNKNOWN
    }
}