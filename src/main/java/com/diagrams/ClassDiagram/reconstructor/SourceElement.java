package com.diagrams.ClassDiagram.reconstructor;

public abstract class SourceElement {
    protected int sourceLineStart = -1;
    protected int sourceLineEnd = -1;
    protected String rawSourceText = null;
    protected boolean modified = false;

    public void setSourceLines(int start, int end) {
        this.sourceLineStart = start;
        this.sourceLineEnd   = end;
    }

    public int getSourceLineStart() {
        return sourceLineStart;
    }

    public int getSourceLineEnd() {
        return sourceLineEnd;
    }

    public boolean hasLine() {
        return sourceLineStart >= 0 && sourceLineEnd >= 0;
    }

    public void setRawSourceText(String text) {
        this.rawSourceText = text;
    }

    public String getRawSourceText() {
        return rawSourceText;
    }

    public void setModified() {
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        this.modified = false;
    }
}