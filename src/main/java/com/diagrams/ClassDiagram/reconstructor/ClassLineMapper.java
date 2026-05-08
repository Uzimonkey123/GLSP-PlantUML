/*
 * File: ClassLineMapper.java
 * Author: Norman Babiak
 * Description: Classifies each line of PlantUML source into a LineType for the parser and writer.
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.model.ClassModel;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ClassLineMapper {
    private final List<LineInfo> lineInfos;

    /**
     * Walks every line in the source text and assigns a LineType.
     */
    public ClassLineMapper(String sourceText, ClassModel model) {
        this.lineInfos = new ArrayList<>();
        String[] lines = sourceText.split("\n", -1);

        // blockStack tracks whether we're inside an entity body { ... }
        Deque<Boolean> blockStack = new ArrayDeque<>();
        boolean insideMultiLineComment = false;
        boolean insideNote = false;

        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            String trimmed = original.trim();

            if (trimmed.equals("left to right direction")) {
                lineInfos.add(new LineInfo(i, original, LineType.LEFT_TO_RIGHT));
                model.setLeftToRight(true);
                continue;
            }

            // Multi-line comment
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

            // Multi-line note body
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

            // Unknown lines inside an entity block are treated as members
            if (type == LineType.UNKNOWN
                    && !blockStack.isEmpty()
                    && Boolean.TRUE.equals(blockStack.peek())) {
                type = LineType.MEMBER;
            }

            // Multi-line note start
            if (type == LineType.NOTE_MULTILINE_START) {
                insideNote = true;
                type = LineType.NOTE;
            }

            lineInfos.add(new LineInfo(i, original, type));

            String text = original.trim();

            if (opensBlock(text)) {
                blockStack.push(true);
            }

            if (closesBlock(text)) {
                if (!blockStack.isEmpty()) blockStack.pop();
            }
        }
    }

    private static boolean opensBlock(String text) {
        return text.contains("{");
    }

    private static boolean closesBlock(String text) {
        return text.contains("}");
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

    /**
     * Classifies a single line by its content. First standalone members, comments, arrows, then keywords, separators
     */
    private static LineType determineType(String trimmed) {
        if (trimmed.isEmpty()) return LineType.EMPTY;

        // Standalone member, so not confuse with relationship
        if (trimmed.matches("[A-Za-z0-9_<>]+\\s*:\\s*.+") && !trimmed.contains("::")) {
            return LineType.MEMBER;
        }

        // Single-line and block comments
        if (trimmed.startsWith("'")) return LineType.COMMENT;
        if (trimmed.startsWith("/'") && trimmed.endsWith("'/")) return LineType.COMMENT;

        if (containsClassArrow(trimmed)) return LineType.RELATIONSHIP;

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

        // Strip visibility prefixes to get the actual keyword
        String firstWord = trimmed.split("\\s+", 2)[0].toLowerCase();
        while (!firstWord.isEmpty() && "-#~+".indexOf(firstWord.charAt(0)) >= 0) {
            firstWord = firstWord.substring(1);
        }

        switch (firstWord) {
            case "@startuml" -> { return LineType.START_UML; }
            case "@enduml" -> { return LineType.END_UML; }

            case "class", "interface", "enum", "annotation", "abstract",
                 "dataclass", "entity", "exception", "metaclass",
                 "protocol", "record", "stereotype", "struct",
                 "diamond", "circle", "()", "<>" -> {
                // "diamond -- Foo" is a relationship
                if (lineLooksLikeRelationship(trimmed)) return LineType.RELATIONSHIP;
                if (hasInlineBody(trimmed)) return LineType.ENTITY_INLINE;
                return LineType.ENTITY_DECLARATION;
            }

            case "package", "namespace",
                 "rectangle", "node", "cloud", "database",
                 "frame", "storage", "component", "folder" -> {
                return LineType.PACKAGE_DECLARATION;
            }

            case "note", "hnote", "rnote" -> {
                // Single-line note and multi-line note start
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

        // Final arrow check
        if (containsClassArrow(trimmed)) return LineType.RELATIONSHIP;

        return LineType.UNKNOWN;
    }

    /**
     * Detects "diamond -- Foo" which starts with an entity keyword but is actually a relationship.
     */
    private static boolean lineLooksLikeRelationship(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("diamond") || trimmed.startsWith("<>")) {
            return trimmed.matches("^diamond\\s*[-.]\\s*\"?.+\"?\\s+\\S.*");
        }

        return false;
    }

    private static boolean hasInlineBody(String trimmed) {
        int open  = trimmed.indexOf('{');
        int close = trimmed.lastIndexOf('}');

        return open != -1 && close != -1 && close > open;
    }

    /**
     * Checks if a note is completed on a single line
     */
    private static boolean isCompletedNote(String trimmed) {
        int first = trimmed.indexOf('"');
        if (first != -1 && trimmed.indexOf('"', first + 1) != -1) {
            return true;
        }

        // Strip :: so "note of Foo::bar" doesn't falsely match the colon
        String stripped = trimmed.replace("::", "");
        return stripped.contains(":");
    }

    /**
     * Separator lines
     */
    private static boolean isSeparatorLine(String trimmed) {
        return (trimmed.startsWith("--") && trimmed.endsWith("--"))
                || (trimmed.startsWith("__") && trimmed.endsWith("__"))
                || (trimmed.startsWith("..") && trimmed.endsWith(".."))
                || (trimmed.startsWith("==") && trimmed.endsWith("=="));
    }

    /**
     * Detects PlantUML class diagram arrows by regex
     */
    private static boolean containsClassArrow(String line) {
        // Remove quoted strings and bracket color specs to avoid matching inside them
        String s = line.replaceAll("\"[^\"]*\"", "");
        s = s.replaceAll("\\[[^]]*:[^]]*]", "");

        // Build regex for multi-character arrows with optional decorators and directions
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

        // Fallback on minimal arrow
        return s.matches(".*\\s[-.]\\s.*");
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

        LEFT_TO_RIGHT,
        UNKNOWN
    }
}
