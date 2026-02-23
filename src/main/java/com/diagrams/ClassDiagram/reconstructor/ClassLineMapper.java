package com.diagrams.ClassDiagram.reconstructor;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ClassLineMapper {
    private final List<LineInfo> lineInfos;

    public ClassLineMapper(String sourceText) {
        this.lineInfos = new ArrayList<>();
        String[] lines = sourceText.split("\n", -1);

        Deque<Boolean> blockStack = new ArrayDeque<>();
        boolean insideMultiLineComment = false;
        boolean insideNote = false;

        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            String trimmed = original.trim();

            if (insideMultiLineComment) {
                lineInfos.add(new LineInfo(i, original, LineType.COMMENT));
                if (trimmed.endsWith("'/")) insideMultiLineComment = false;
                continue;
            }

            if (trimmed.startsWith("/'") && !trimmed.endsWith("'/")) {
                insideMultiLineComment = true;
                lineInfos.add(new LineInfo(i, original, LineType.COMMENT));
                continue;
            }

            if (insideNote) {
                String lo = trimmed.toLowerCase();
                if (lo.equals("end note") || lo.equals("endnote")
                        || lo.equals("end hnote") || lo.equals("endhnote")
                        || lo.equals("end rnote") || lo.equals("endrnote")) {
                    insideNote = false;
                    lineInfos.add(new LineInfo(i, original, LineType.END_NOTE));

                } else {
                    lineInfos.add(new LineInfo(i, original, LineType.NOTE_BODY));
                }

                continue;
            }

            LineType type = determineType(trimmed);

            if (type == LineType.UNKNOWN
                    && !blockStack.isEmpty()
                    && Boolean.TRUE.equals(blockStack.peek())) {
                type = LineType.MEMBER;
            }

            if (type == LineType.NOTE_MULTILINE_START) {
                insideNote = true;
                type = LineType.NOTE;
            }

            lineInfos.add(new LineInfo(i, original, type));

            switch (type) {
                case ENTITY_DECLARATION -> blockStack.push(true);
                case PACKAGE_DECLARATION -> blockStack.push(false);
                case BLOCK_END -> { if (!blockStack.isEmpty()) blockStack.pop(); }
                default -> {}
            }
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

    private static LineType determineType(String trimmed) {
        if (trimmed.isEmpty()) return LineType.EMPTY;

        // Single-line and block comments
        if (trimmed.startsWith("'")) return LineType.COMMENT;
        if (trimmed.startsWith("/'") && trimmed.endsWith("'/")) return LineType.COMMENT;

        String lo = trimmed.toLowerCase();
        switch (lo) {
            case "@startuml" -> { return LineType.START_UML; }
            case "@enduml" -> { return LineType.END_UML; }
            case "{" -> { return LineType.BLOCK_START; }
            case "}" -> { return LineType.BLOCK_END; }
            case "end note", "endnote",
                 "end hnote", "endhnote",
                 "end rnote", "endrnote" -> { return LineType.END_NOTE; }
            case "end title" -> { return LineType.END_TITLE; }
            case "end header" -> { return LineType.END_HEADER; }
            case "end footer" -> { return LineType.END_FOOTER; }
        }

        String firstWord = trimmed.split("\\s+", 2)[0].toLowerCase();
        if (firstWord.contains("#")) {
            firstWord = firstWord.substring(0, firstWord.indexOf('#'));
        }

        switch (firstWord) {
            case "@startuml" -> { return LineType.START_UML; }
            case "@enduml" -> { return LineType.END_UML; }

            case "class", "interface", "enum", "annotation", "abstract" -> {
                if (hasInlineBody(trimmed)) return LineType.ENTITY_INLINE;
                return LineType.ENTITY_DECLARATION;
            }

            case "package", "namespace",
                 "rectangle", "node", "cloud", "database",
                 "frame", "storage", "component", "folder" -> {
                return LineType.PACKAGE_DECLARATION;
            }

            case "note", "hnote", "rnote" -> {
                if (isCompletedNote(trimmed)) return LineType.NOTE;
                return LineType.NOTE_MULTILINE_START;
            }

            case "hide" -> { return LineType.HIDE; }
            case "show" -> { return LineType.SHOW; }
            case "title" -> { return LineType.TITLE; }
            case "header" -> { return LineType.HEADER; }
            case "footer" -> { return LineType.FOOTER; }
        }

        if (isSeparatorLine(trimmed)) return LineType.SEPARATOR;

        if (containsClassArrow(trimmed)) return LineType.RELATIONSHIP;

        return LineType.UNKNOWN;
    }

    private static boolean hasInlineBody(String trimmed) {
        int open  = trimmed.indexOf('{');
        int close = trimmed.lastIndexOf('}');

        return open != -1 && close != -1 && close > open;
    }

    private static boolean isCompletedNote(String trimmed) {
        int first = trimmed.indexOf('"');
        if (first != -1 && trimmed.indexOf('"', first + 1) != -1) {
            return true;
        }

        return trimmed.contains(":");
    }

    private static boolean isSeparatorLine(String trimmed) {
        return (trimmed.startsWith("--") && trimmed.endsWith("--"))
                || (trimmed.startsWith("__") && trimmed.endsWith("__"));
    }

    private static boolean containsClassArrow(String line) {
        String s = line.replaceAll("\"[^\"]*\"", "");
        s = s.replaceAll("\\[[^]]*:[^]]*]", "");

        String dec = "[<>|*o#x^{}+]*";
        String lineSeg = "[-.]+";
        String modifier = "(?:\\[[^]]*])?";
        String direction =
                "(?:" +
                        "(?:left|right|up|down|[lrud])" + lineSeg +
                        "|(?:left|right|up|down)" +
                        ")?";

        String multiArrow =
                dec + "(?:"
                        + lineSeg + modifier + direction
                        + "|[<>|*o#x^{}+]+" + lineSeg
                        + "|" + lineSeg + "[<>|*o#x^{}+]+"
                        + ")" + dec;

        if (s.matches(".*\\S\\s*" + multiArrow + "\\s*\\S.*")) {
            return true;
        }

        return s.matches(".*\\s[-\\.]\\s.*");
    }

    public static class LineInfo {
        public final int lineNumber;
        public final String originalText;
        public final String trimmedText;
        public final LineType type;

        public LineInfo(int lineNumber, String originalText, LineType type) {
            this.lineNumber = lineNumber;
            this.originalText = originalText;
            this.trimmedText = originalText.trim();
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("[%3d] %-24s | %s", lineNumber, type, trimmedText);
        }
    }

    public enum LineType {
        EMPTY,
        COMMENT,
        START_UML,
        END_UML,

        ENTITY_DECLARATION,
        ENTITY_INLINE,
        BLOCK_START,
        BLOCK_END,
        MEMBER,
        SEPARATOR,

        RELATIONSHIP,

        PACKAGE_DECLARATION,

        NOTE,
        NOTE_MULTILINE_START,
        NOTE_BODY,
        END_NOTE,

        HIDE,
        SHOW,

        TITLE,
        END_TITLE,
        HEADER,
        END_HEADER,
        FOOTER,
        END_FOOTER,

        UNKNOWN
    }
}