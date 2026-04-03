/*
 * File: NewLine.java
 * Author: Norman Babiak
 * Description: Represents a pending line replacement in the source text.
 * Date: 2.4.2026
 */

package com.diagrams.SequenceDiagram.utils;

import java.util.List;

public record NewLine(
        int startLine,
        int endLine,
        List<String> newLines
) {}
