package com.diagrams.SequenceDiagram.utils;

import java.util.Arrays;

public class WidthCalculator {

    private static final String TAG_REGEX = "<[^>]*>(?!>)";

    public WidthCalculator() {}

    private static int calculateMaxLength(String lines) {
        return Arrays.stream(lines.split("<br>"))
                .map(line -> line.replaceAll(TAG_REGEX, ""))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    public static double calculateWidth(String lines, int padding) {
        return calculateMaxLength(lines) * 7 + padding;
    }
}
