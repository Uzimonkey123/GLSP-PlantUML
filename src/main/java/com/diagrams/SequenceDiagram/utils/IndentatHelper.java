/*
 * File: IndentatHelper.java
 * Author: Norman Babiak
 * Description: Util class for extracting and applying indentation from / to source file in writer
 * Date: 6.5.2026
 */

package com.diagrams.SequenceDiagram.utils;

public class IndentatHelper {
    /**
     * Extracts the starting whitespaces from start of the line
     */
    public static String extractIndentation(String line) {
        if (line == null || line.isEmpty()) return "";

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < line.length() &&
                Character.isWhitespace(line.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        return line.substring(0, firstNonWhitespace);
    }

    /**
     * Appends the removed extracted indentation to the beginning of the edited source line to keep it consistent
     */
    public static String applyIndentation(String content, String indentation) {
        if (indentation.isEmpty()) return content;

        if (content.contains("\n")) {
            String[] lines = content.split("\n");
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                sb.append(indentation).append(lines[i]);
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        return indentation + content;
    }
}
