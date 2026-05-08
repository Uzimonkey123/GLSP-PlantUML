/*
 * File: MapperInfo.java
 * Author: Norman Babiak
 * Description: Maps model elements to their source line positions.
 * Date: 6.5.2026
 */

package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.SourceElement;

public class MapperInfo {
    /**
     * Maps an element to a single source line, storing both the line number and original text
     */
    public static void addMapperInfo(SourceElement element, int lineNum, ClassLineMapper lineMapper) {
        if (lineNum >= 0) {
            element.setSourceLines(lineNum, lineNum);
            ClassLineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    /**
     * Maps an element to a source line range, storing the start line's original text
     */
    public static void addMapperInfo(SourceElement element, int startLine, int endLine, ClassLineMapper lineMapper) {
        if (startLine >= 0) {
            element.setSourceLines(startLine, endLine >= 0 ? endLine : startLine);
            ClassLineMapper.LineInfo info = lineMapper.getLineInfo(startLine);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }
}
