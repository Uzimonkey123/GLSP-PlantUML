package com.diagrams.ClassDiagram.utils;

import java.util.List;

public record NewLine(int startLine, int endLine, List<String> newLines) {
    @Override
    public String toString() {
        return "NewLine[" + startLine + "-" + endLine + " → " + newLines + "]";
    }
}