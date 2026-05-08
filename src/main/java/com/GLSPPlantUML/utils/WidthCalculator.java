/*
 * File: WidthCalculator.java
 * Author: Norman Babiak
 * Description: Common util width calculator for labels
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.utils;

import java.util.Arrays;

public class WidthCalculator {

    private static final String TAG_REGEX = "<[^>]*>(?!>)";

    public WidthCalculator() {}

    /**
     * Calculates the amount of lines
     */
    private static int calculateMaxLength(String lines) {
        return Arrays.stream(lines.split("<br>"))
                .map(line -> line.replaceAll(TAG_REGEX, ""))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    /**
     * Calculates the length of the line by character, possibility to add padding
     */
    public static double calculateWidth(String lines, int padding) {
        if (lines == null) {
            return 0;
        }

        return calculateMaxLength(lines) * 7 + padding;
    }
}
