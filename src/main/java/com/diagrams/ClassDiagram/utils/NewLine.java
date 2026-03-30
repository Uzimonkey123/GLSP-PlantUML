/*
 * File: NewLine.java
 * Author: Norman Babiak
 * Description: Represents a pending line replacement in the source text.
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.utils;

import java.util.List;

public record NewLine(int startLine, int endLine, List<String> newLines) {
    @Override
    public String toString() {
        return "NewLine[" + startLine + "-" + endLine + " → " + newLines + "]";
    }
}