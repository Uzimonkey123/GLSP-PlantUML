package com.GLSPPlantUML.utils;

import java.util.Arrays;

public class WidthCalculator {

    public WidthCalculator() {}

    private static int calculateMaxLength(String lines) {
        return Arrays.stream(lines.split("<br>"))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    public static double calculateWidth(String lines, int padding) {
        return calculateMaxLength(lines) * 6 + padding;
    }
}
