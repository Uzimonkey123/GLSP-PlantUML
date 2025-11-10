package com.diagrams.SequenceDiagram.utils;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;

public class ReconstructorHelper {
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

    public static String getParticipant(SequenceNode node) {
        String alias = extractAlias(node.getRawSourceText());
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }

        // No alias, use quotes
        String name = node.getName();
        if (!name.matches("[a-zA-Z0-9_]+")) {
            return "\"" + name + "\"";
        }

        // Use regular name
        return name;
    }

    public static String extractLifeEventSymbol(String line) {
        if (line == null || line.isEmpty()) return "";

        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) return "";

        String beforeColon = line.substring(0, colonIndex).trim();
        String[] symbols = {"--++", "++", "--", "!!", "**"};

        for (String symbol : symbols) {
            int symbolPos = beforeColon.lastIndexOf(symbol);
            if (symbolPos > 0) {
                String afterSymbol = beforeColon.substring(symbolPos + symbol.length()).trim();

                // Check for color code
                if (afterSymbol.startsWith("#")) {
                    String colorCode = afterSymbol.split("\\s+")[0];
                    return symbol + " " + colorCode;
                } else if (afterSymbol.isEmpty()) {
                    return symbol;
                }
            }
        }

        // Color for auto activation
        String[] words = beforeColon.split("\\s+");
        if (words.length > 0) {
            String lastWord = words[words.length - 1];
            if (lastWord.startsWith("#")) {
                return lastWord;
            }
        }

        return "";
    }
}
