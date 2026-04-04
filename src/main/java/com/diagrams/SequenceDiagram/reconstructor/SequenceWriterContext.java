/*
 * File: SequenceWriterContext.java
 * Author: Norman Babiak
 * Description: Shared context for sequence diagram writer operations
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.utils.IndentatHelper;
import com.diagrams.SequenceDiagram.utils.NewLine;

import java.util.*;

public class SequenceWriterContext {

    private final SequenceModel model;
    private final List<String> sourceLines;
    private final Map<Integer, NewLine> newLines;
    private final LineMapper lineMap;

    public SequenceWriterContext(SequenceModel model, List<String> sourceLines, LineMapper lineMap) {
        this.model = model;
        this.sourceLines = sourceLines;
        this.lineMap = lineMap;
        this.newLines = new HashMap<>();
    }

    public SequenceModel getModel() {
        return model;
    }

    public List<String> getSourceLines() {
        return sourceLines;
    }

    public LineMapper getLineMap() {
        return lineMap;
    }

    public Map<Integer, NewLine> getNewLines() {
        return newLines;
    }

    /**
     * Schedules a source line range to be replaced with new content.
     */
    public void changeLine(int start, int end, List<String> lines) {
        if (!newLines.containsKey(start)) {
            newLines.put(start, new NewLine(start, end, lines));
        }
    }

    /**
     * Clears all pending replacements for a fresh write pass.
     */
    public void clearReplacements() {
        newLines.clear();
    }

    /**
     * Extracts indentation from a source element's raw text and applies it to new content.
     */
    public String indented(String content, String rawSourceText) {
        String indent = IndentatHelper.extractIndentation(rawSourceText);
        return IndentatHelper.applyIndentation(content, indent);
    }

    /**
     * Extracts indentation from a source line number and applies it to new content.
     */
    public String indentedFromLine(String content, int lineNumber) {
        String indent = IndentatHelper.extractIndentation(sourceLines.get(lineNumber));
        return IndentatHelper.applyIndentation(content, indent);
    }

    /**
     * Returns the LineType classification for a given source line.
     */
    public LineMapper.LineType getLineType(int lineNumber) {
        LineMapper.LineInfo info = lineMap.getLineInfo(lineNumber);
        return info != null ? info.type : null;
    }
}
