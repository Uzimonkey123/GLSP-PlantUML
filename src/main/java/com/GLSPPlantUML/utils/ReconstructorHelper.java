package com.GLSPPlantUML.utils;

public class ReconstructorHelper {
    public static void appendQuotedName(StringBuilder sb, String name) {
        if (name.contains(" ") || name.contains(":")) {
            sb.append("\"").append(name).append("\"");

        } else {
            sb.append(name);
        }
    }

    public static String extractAlias(String sourceLine) {
        if (sourceLine == null || !sourceLine.contains(" as ")) {
            return null;
        }

        int asIndex = sourceLine.indexOf(" as ");
        if (asIndex == -1) return null;

        String afterAs = sourceLine.substring(asIndex + 4).trim();

        int spaceIndex = afterAs.indexOf(' ');
        if (spaceIndex > 0) {
            return afterAs.substring(0, spaceIndex);
        }

        return afterAs;
    }
}
