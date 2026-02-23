package com.diagrams.ClassDiagram.reconstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassLineFinder {

    private final ClassLineMapper lineMapper;
    private final Map<Object, Integer> elementToLineMap;
    private int searchFrom = 0;
    private final Set<Integer> claimedLines;

    public ClassLineFinder(ClassLineMapper lineMapper, Map<Object, Integer> elementToLineMap) {
        this.lineMapper       = lineMapper;
        this.elementToLineMap = elementToLineMap;
        this.claimedLines     = new HashSet<>();
    }

    public int findEntityLine(String name, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

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

        for (int i = 0; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);
            if (claimedLines.contains(i)) continue;

            if (info.type == ClassLineMapper.LineType.RELATIONSHIP
                    && relMatchesName(info.originalText, name)) {
                register(element, i);
                return i;
            }
        }

        return -1;
    }

    public int findRelationshipLine(String alias1, String alias2, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = searchFrom; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);

            if (info.type == ClassLineMapper.LineType.RELATIONSHIP
                    && relMatchesName(info.originalText, alias1)
                    && relMatchesName(info.originalText, alias2)) {
                register(element, i);
                searchFrom = i + 1;
                return i;
            }
        }

        return -1;
    }

    public int findPackageLine(String name, Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = 0; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);

            if (info.type == ClassLineMapper.LineType.PACKAGE_DECLARATION
                    && packageMatchesName(info.originalText, name)) {
                register(element, i);
                return i;
            }

            // Also check entity declaration lines for implicit packages
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
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();
        boolean filter = text != null && !text.isEmpty();

        for (int i = searchFrom; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);
            if (info.type == ClassLineMapper.LineType.NOTE
                    && (!filter || info.originalText.contains(text))) {
                register(element, i);
                searchFrom = i + 1;
                return i;
            }
        }

        return -1;
    }

    public int findNoteEndLine(Object element) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();

        for (int i = searchFrom; i < all.size(); i++) {
            ClassLineMapper.LineInfo info = all.get(i);

            if (info.type == ClassLineMapper.LineType.END_NOTE) {
                register(element, i);
                searchFrom = i + 1;
                return i;
            }
        }

        return -1;
    }

    public int findTitleLine()     { return findLineByType(ClassLineMapper.LineType.TITLE); }
    public int findEndTitleLine()  { return findLineByType(ClassLineMapper.LineType.END_TITLE); }
    public int findHeaderLine()    { return findLineByType(ClassLineMapper.LineType.HEADER); }
    public int findEndHeaderLine() { return findLineByType(ClassLineMapper.LineType.END_HEADER); }
    public int findFooterLine()    { return findLineByType(ClassLineMapper.LineType.FOOTER); }
    public int findEndFooterLine() { return findLineByType(ClassLineMapper.LineType.END_FOOTER); }


    public void setPosition(int position) { this.searchFrom = position; }

    static boolean entityMatchesName(String line, String name) {
        if (name == null || name.isEmpty()) return true;

        // Strip generic type params <...>
        String stripped = line.replaceAll("<[^>]*>", "");

        // Quoted form
        if (stripped.contains("\"" + name + "\"")) return true;

        return wordBoundaryContains(stripped, name);
    }

    static boolean relMatchesName(String line, String name) {
        if (name == null || name.isEmpty()) return true;

        // Strip relationship label: ' : something'
        String haystack = line.replaceAll(" : .*$", "");

        // Quoted form: "name"
        if (haystack.contains("\"" + name + "\"")) return true;

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

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(pkgName)) return true;
        }

        for (int i = 1; i < parts.length; i++) {
            String prefix = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i));
            if (prefix.equals(pkgName)) return true;
        }

        return false;
    }

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

    public static String extractAlias(String line) {
        if (line == null) return null;
        String trimmed = line.trim();

        Matcher m = Pattern.compile(
                "^(?:abstract\\s+)?(?:class|interface|enum|annotation)\\s+(.*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(trimmed);

        if (!m.matches()) return null;

        String rest = m.group(1).trim();
        String name;
        String remainder;

        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            if (end < 0) return null;
            name      = rest.substring(1, end);
            remainder = rest.substring(end + 1).trim();

        } else {
            String[] parts = rest.split("[\\s{#<]", 2);
            name      = parts[0];
            remainder = parts.length > 1 ? rest.substring(parts[0].length()).trim() : "";
        }

        Matcher asM = Pattern.compile(
                "^as\\s+(.*)", Pattern.CASE_INSENSITIVE
        ).matcher(remainder);

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