package com.GLSPPlantUML.utils;

import java.util.List;

public record NewLine(
        int startLine,
        int endLine,
        List<String> newLines
) {}
