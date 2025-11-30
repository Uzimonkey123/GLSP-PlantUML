package com.diagrams.ClassDiagram.model;

public enum Visibility {
    PRIVATE('-', "private"),
    PROTECTED('#', "protected"),
    PACKAGE_PRIVATE('~', "package_private"),
    PUBLIC('+', "public");

    private final char symbol;
    private final String value;

    Visibility(char symbol, String value) {
        this.symbol = symbol;
        this.value = value;
    }

    public char getSymbol() {
        return symbol;
    }

    public String getValue() {
        return value;
    }

    public static String fromChar(char c) {
        for (Visibility v : values()) {
            if (v.symbol == c) {
                return v.value;
            }
        }

        return "";
    }
}