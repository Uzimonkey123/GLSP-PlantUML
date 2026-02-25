package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.SourceElement;

public class MapperInfo {
    public static void addMapperInfo(SourceElement element, int lineNum, ClassLineMapper lineMapper) {
        if (lineNum >= 0) {
            element.setSourceLines(lineNum, lineNum);
            ClassLineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

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
