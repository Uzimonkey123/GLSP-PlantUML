/*
 * File: ClassLineFinder.java
 * Author: Norman Babiak
 * Description: Locates model elements in the PlantUML source by line type and content matching.
 * Date: 1.4.2026
 */

package com.diagrams.ClassDiagram.reconstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassLineFinder {

    private final ClassLineMapper lineMapper;
    private final Map<Object, Integer> elementToLineMap;
    private int searchFrom = 0;
    private final Set<Integer> claimedLines;

    public ClassLineFinder(ClassLineMapper lineMapper, Map<Object, Integer> elementToLineMap) {
        this.lineMapper = lineMapper;
        this.elementToLineMap = elementToLineMap;
        this.claimedLines = new HashSet<>();
    }

    /**
     * Searches forward from searchFrom, then wraps around from 0, returning the first unclaimed line that matches the predicate.
     */
    private int searchWithWrap(Predicate<ClassLineMapper.LineInfo> matcher, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = searchFrom; i < all.size(); i++) {
            if (!claimedLines.contains(i) && matcher.test(all.get(i))) {
                register(element, i);
                searchFrom = i + 1;
                return i;
            }
        }

        for (int i = 0; i < searchFrom; i++) {
            if (!claimedLines.contains(i) && matcher.test(all.get(i))) {
                register(element, i);
                return i;
            }
        }

        return -1;
    }

    public int findEntityLine(String name, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        // look for entity declarations
        for (int i = 0; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);
            if (claimedLines.contains(i)) continue;

            if ((info.type == ClassLineMapper.LineType.ENTITY_DECLARATION
                    || info.type == ClassLineMapper.LineType.ENTITY_INLINE)
                    && entityMatchesName(info.originalText, name)) {
                register(element, i);
                return i;
            }
        }

        // entity might only appear in a relationship line
        for (int i = 0; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);
            if (claimedLines.contains(i)) continue;

            if (info.type == ClassLineMapper.LineType.RELATIONSHIP && relMatchesName(info.originalText, name)) {
                return i;
            }
        }

        return -1;
    }

    public int findRelationshipLine(String alias1, String alias2, Object element) {
        return searchWithWrap(info ->
                info.type == ClassLineMapper.LineType.RELATIONSHIP
                        && relMatchesName(info.originalText, alias1)
                        && relMatchesName(info.originalText, alias2), element);
    }

    public int findPackageLine(String name, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = 0; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);

            if (info.type == ClassLineMapper.LineType.PACKAGE_DECLARATION && packageMatchesName(info.originalText, name)) {
                register(element, i);
                return i;
            }

            // Implicit packages
            if ((info.type == ClassLineMapper.LineType.ENTITY_DECLARATION
                    || info.type == ClassLineMapper.LineType.ENTITY_INLINE)
                    && implicitPackageMatchesName(info.originalText, name)) {
                register(element, i);
                return i;
            }
        }

        return -1;
    }

    public int findNoteLine(String text, Object element) {
        boolean filter = text != null && !text.isEmpty();
        // Normalize <br> to \n for comparison since source uses \n and model uses <br>
        String normalizedText = filter ? text.replace("<br>", "\\n").trim() : null;

        return searchWithWrap(info -> {
            if (info.type != ClassLineMapper.LineType.NOTE) return false;

            // Build the full note content by concatenating all following lines
            String noteText = buildFullNoteText(info);
            return !filter || noteText.contains(normalizedText);
        }, element);
    }

    /**
     * Join the note header line with all following lines to build the full note text for content matching.
     */
    private String buildFullNoteText(ClassLineMapper.LineInfo noteInfo) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();
        StringBuilder fullNote = new StringBuilder();
        fullNote.append(noteInfo.originalText.replace("<br>", "\\n").trim());

        for (int j = noteInfo.lineNumber + 1; j < all.size(); j++) {
            ClassLineMapper.LineInfo body = all.get(j);
            fullNote.append("\\n").append(body.originalText.replace("<br>", "\\n").trim());
        }

        return fullNote.toString();
    }

    public int findNoteEndLine(int noteStartLine, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = noteStartLine + 1; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);

            if (info.type == ClassLineMapper.LineType.END_NOTE) {
                register(element, i);
                searchFrom = i + 1;
                return i;
            }

            // Hit another note before finding end note this was a single-line note
            if (info.type == ClassLineMapper.LineType.NOTE) {
                break;
            }
        }

        // Single-line note: start and end are the same line
        register(element, noteStartLine);
        return noteStartLine;
    }

    public int findTitleLine() {
        return findLineByType(ClassLineMapper.LineType.TITLE);
    }

    public int findEndTitleLine() {
        return findLineByType(ClassLineMapper.LineType.END_TITLE);
    }

    public int findHeaderLine() {
        return findLineByType(ClassLineMapper.LineType.HEADER);
    }

    public int findEndHeaderLine() {
        return findLineByType(ClassLineMapper.LineType.END_HEADER);
    }

    public int findFooterLine() {
        return findLineByType(ClassLineMapper.LineType.FOOTER);
    }

    public int findEndFooterLine() {
        return findLineByType(ClassLineMapper.LineType.END_FOOTER);
    }

    public void setPosition(int position) {
        this.searchFrom = position;
    }

    static boolean entityMatchesName(String line, String name) {
        if (name == null || name.isEmpty()) return true;

        // Strip generic type params so "Container<T>" matches "Container"
        String stripped = line.replaceAll("<[^>]*>", "");

        if (stripped.contains("\"" + name + "\"")) return true;

        return wordBoundaryContains(stripped, name);
    }

    static boolean relMatchesName(String line, String name) {
        if (name == null || name.isEmpty()) return true;

        String haystack = line.replaceAll(" : .*$", "");

        if (haystack.contains("\"" + name + "\"")) return true;

        // Strip all quoted strings to avoid matching inside labels
        String stripped = haystack.replaceAll("\"[^\"]*\"", "");
        return wordBoundaryContains(stripped, name);
    }

    private static boolean packageMatchesName(String line, String name) {
        if (name == null || name.isEmpty()) return true;
        String stripped = line.replaceAll("<[^>]*>", "");

        return wordBoundaryContains(stripped, name);
    }

    private static boolean implicitPackageMatchesName(String line, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) return false;

        String stripped = line.replaceAll("<[^>]*>", "");
        Matcher m = Pattern.compile(
                "(?:class|interface|enum|annotation)\\s+([A-Za-z0-9_.]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(stripped);

        if (!m.find()) return false;

        String dotted = m.group(1);
        String[] parts = dotted.split("\\.");
        if (parts.length <= 1) return false;

        // Check individual segments
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(pkgName)) return true;
        }

        // Check prefixes
        for (int i = 1; i < parts.length; i++) {
            String prefix = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i));
            if (prefix.equals(pkgName)) return true;
        }

        return false;
    }

    /**
     * Checks if word appears in text surrounded by non-identifier characters. Prevents "User" from matching
     * inside "UserService".
     */
    private static boolean wordBoundaryContains(String text, String word) {
        int idx = text.indexOf(word);

        while (idx >= 0) {
            char before = idx > 0 ? text.charAt(idx - 1) : ' ';
            char after  = idx + word.length() < text.length()
                    ? text.charAt(idx + word.length()) : ' ';
            boolean beforeOk = !Character.isLetterOrDigit(before) && before != '_';
            boolean afterOk  = !Character.isLetterOrDigit(after)  && after  != '_';
            if (beforeOk && afterOk) return true;
            idx = text.indexOf(word, idx + 1);
        }

        return false;
    }

    /**
     * Extracts the alias, or name if no alias
     */
    public static String extractAlias(String line) {
        if (line == null) return null;
        String trimmed = line.trim();

        // Note alias
        Matcher noteAlias = Pattern.compile(
                "^note\\s+.*?\\bas\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(trimmed);

        if (noteAlias.find()) {
            return noteAlias.group(1);
        }

        // Entity declaration
        Matcher m = Pattern.compile(
                "^(?:abstract\\s+)?(?:class|interface|diamond|circle|enum|annotation)\\s+(.*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(trimmed);

        if (!m.matches()) return null;

        String rest = m.group(1).trim();
        String name;
        String remainder;

        if (rest.startsWith("\"")) {
            // Quoted name
            int end = rest.indexOf('"', 1);
            if (end < 0) return null;
            name = rest.substring(1, end);
            remainder = rest.substring(end + 1).trim();

        } else {
            // Unquoted name
            String[] parts = rest.split("[\\s{#<]", 2);
            name = parts[0];
            remainder = parts.length > 1 ? rest.substring(parts[0].length()).trim() : "";
        }

        Matcher asM = Pattern.compile("^as\\s+(.*)", Pattern.CASE_INSENSITIVE).matcher(remainder);

        if (asM.matches()) {
            String aliasPart = asM.group(1).trim();
            if (aliasPart.startsWith("\"")) {
                return name;

            } else {
                String tok = aliasPart.split("[\\s{#]", 2)[0];

                return tok.isEmpty() ? name : tok;
            }
        }

        return name;
    }

    private int findLineByType(ClassLineMapper.LineType type) {
        for (ClassLineMapper.LineInfo info : lineMapper.getLineInfos()) {
            if (info.type == type) return info.lineNumber;
        }

        return -1;
    }

    private void register(Object element, int line) {
        if (element != null) elementToLineMap.put(element, line);
        claimedLines.add(line);
    }
}