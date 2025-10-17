package com.GLSPPlantUML.utils;

public class IndentatHelper {
    public static String extractIndentation(String line) {
        if (line == null || line.isEmpty()) return "";

        int firstNonWhitespace = 0;
        while (firstNonWhitespace < line.length() &&
                Character.isWhitespace(line.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }

        return line.substring(0, firstNonWhitespace);
    }

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
