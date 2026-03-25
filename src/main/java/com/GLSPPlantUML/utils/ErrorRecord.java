package com.GLSPPlantUML.utils;

public record ErrorRecord(
    boolean hasError,
    String errorMsg,
    int lineNumber,
    int columnStart,
    int columnEnd
) {}