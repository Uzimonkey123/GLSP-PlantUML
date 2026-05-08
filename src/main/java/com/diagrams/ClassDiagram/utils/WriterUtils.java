/*
 * File: WriterUtils.java
 * Author: Norman Babiak
 * Description: Utility methods for writing source file after save
 * Date: 6.5.2026
 */

package com.diagrams.ClassDiagram.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WriterUtils {
    private WriterUtils() {}

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Extracts leading whitespace from a line
     */
    public static String extractIndentation(String line) {
        if (line == null) return "";
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') sb.append(c);
            else break;
        }

        return sb.toString();
    }

    /**
     * Prepends the given indentation to a line
     */
    public static String applyIndentation(String line, String indent) {
        return indent + line;
    }

    public static int countChar(String s, char c) {
        int count = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }

        return count;
    }

    /**
     * Replaces occurrences of oldWord only when surrounded by non-identifier characters. Prevents "User"
     * from matching inside "UserService".
     */
    public static String replaceWordBoundary(String text, String oldWord, String replacement) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            int idx = text.indexOf(oldWord, i);
            if (idx < 0) {
                result.append(text.substring(i));

                break;
            }

            char before = idx > 0 ? text.charAt(idx - 1) : ' ';
            char after = idx + oldWord.length() < text.length() ? text.charAt(idx + oldWord.length()) : ' ';

            boolean ok = !Character.isLetterOrDigit(before) && before != '_'
                    && !Character.isLetterOrDigit(after) && after != '_';

            if (ok) {
                result.append(text, i, idx).append(replacement);
                i = idx + oldWord.length();

            } else {
                result.append(text, i, idx + 1);
                i = idx + 1;
            }
        }

        return result.toString();
    }

    /**
     * Wraps a value in quotes if it contains spaces or non-identifier characters
     */
    public static String quoteIfNeeded(String value) {
        if (value == null) return null;

        if (value.contains(" ") || !value.matches("[A-Za-z0-9_]+")) {
            return "\"" + value + "\"";
        }

        return value;
    }

    /**
     * Removes surrounding double quotes if present
     */
    public static String unquote(String value) {
        if (value == null) return null;

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    /**
     * Converts a link decorator enum value to its PlantUML arrow symbol
     */
    public static char visibilityToSymbol(String visibilityValue) {
        if (visibilityValue == null) return 0;

        return switch (visibilityValue) {
            case "private" -> '-';
            case "protected" -> '#';
            case "package_private" -> '~';
            case "public" -> '+';
            default -> 0;
        };
    }

    /**
     * Converts a link decorator enum value to its PlantUML arrow symbol.
    */
    public static String decoratorSymbol(String dec, boolean left) {
        if (dec == null || dec.isEmpty()) return "";

        return switch (dec) {
            case "ARROW" -> left ? "<" : ">";
            case "EXTENDS" -> left ? "<|" : "|>";
            case "COMPOSITION" -> "*";
            case "AGREGATION" -> "o";
            case "PLUS" -> "+";
            case "SQUARE" -> "#";
            case "CROWFOOT" -> left ? "}" : "{";
            default -> "";
        };
    }

    /**
     * Maps an internal entity type string to the PlantUML keyword used in source
     */
    public static String entityTypeToKeyword(String type, String rawSourceText) {
        if (type == null) return "class";

        return switch (type) {
            case "INTERFACE" -> "interface";
            case "ENUM" -> "enum";
            case "ANNOTATION" -> "annotation";
            case "ABSTRACT", "ABSTRACT_CLASS" -> {
                if (rawSourceText != null && rawSourceText.trim().startsWith("abstract class")) {
                    yield "abstract class";
                }
                yield "abstract";
            }
            case "DATACLASS" -> "dataclass";
            case "ENTITY" -> "entity";
            case "EXCEPTION" -> "exception";
            case "METACLASS" -> "metaclass";
            case "PROTOCOL" -> "protocol";
            case "RECORD" -> "record";
            case "STEREOTYPE" -> "stereotype";
            case "STRUCT" -> "struct";
            case "DIAMOND" -> "diamond";
            case "CIRCLE" -> "circle";
            default -> "class";
        };
    }

    /**
     * Extracts the reference part of a member declaration, stripping return types and keeping only method
     * name + params or field name.
     */
    public static String deriveMemberRef(String methodName) {
        if (methodName == null || methodName.isEmpty()) return methodName;
        String temp = methodName.trim();

        int parenIdx = temp.indexOf('(');
        if (parenIdx >= 0) {
            String beforeParen = temp.substring(0, parenIdx).trim();
            String[] parts = beforeParen.split("\\s+");

            return parts[parts.length - 1] + temp.substring(parenIdx);
        }

        int colonIdx = temp.indexOf(" : ");
        if (colonIdx >= 0) {
            String field = temp.substring(0, colonIdx).trim();
            String[] parts = field.split("\\s+");

            return parts[parts.length - 1];
        }

        String[] parts = temp.split("\\s+");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        }

        return temp;
    }

    /**
     * Strips member name until just the base of it remains without any visibility modifiers etc.
     */
    public static String parseRawMemberName(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String tempName = raw;
        boolean hasVis = false;

        if (tempName.length() >= 2 && tempName.charAt(0) != tempName.charAt(1)) {
            char c = tempName.charAt(0);
            if (c == '+' || c == '-' || c == '#' || c == '~') {
                hasVis = true;
            }
        }

        if (raw.charAt(0) == '\\' || hasVis) {
            tempName = raw.substring(1).trim();
        }

        tempName = tempName
                .replaceAll("\\{(?!static}|classifier}|abstract})[^}]*}", "")
                .trim();

        return tempName;
    }

    /**
     * Quotes a member reference if it contains spaces, parentheses, or colons
     */
    public static String formatMemberRef(String ref) {
        if (ref == null) return "";

        if (ref.contains(" ") || ref.contains("(") || ref.contains(":")) {
            return "\"" + ref + "\"";
        }

        return ref;
    }

    /**
     * Replaces Entity::oldRef with Entity::newRef for a given entity token.
     */
    public static String replaceMemberRefsForToken(String text, String token, Map<String, String> refMap) {
        for (var refEntry : refMap.entrySet()) {
            String oldRef = refEntry.getKey();
            String newRef = refEntry.getValue();

            text = text.replace(token + "::" + oldRef, token + "::" + formatMemberRef(newRef));
            text = text.replace(token + "::\"" + oldRef + "\"", token + "::" + formatMemberRef(newRef));
        }
        return text;
    }

    /**
     * Replaces entity name references using regex word boundaries. If strict, dots are also treated as
     * boundary characters
     */
    public static String replaceReference(String current, String oldName, String newToken, boolean strict) {
        if (oldName.equals(newToken)) return current;

        String lookbehind = strict ? "(?<![A-Za-z0-9_.])" : "(?<![A-Za-z0-9_])";

        String regex = lookbehind + Pattern.quote(oldName) + "(?![A-Za-z0-9_])";
        current = current.replaceAll(regex, Matcher.quoteReplacement(newToken));

        regex = lookbehind + "\"" + Pattern.quote(oldName) + "\"(?![A-Za-z0-9_])";
        current = current.replaceAll(regex, Matcher.quoteReplacement('"' + newToken + '"'));

        return current;
    }
}