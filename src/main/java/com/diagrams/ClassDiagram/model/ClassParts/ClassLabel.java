/*
 * File: ClassLabel.java
 * Author: Norman Babiak
 * Description: Label representation of text in diagram
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.NodePosition;

public class ClassLabel extends NodePosition {
    private final String labelId;
    private String label = "";

    public ClassLabel(double x, double y, String labelId, String label) {
        super(x, y);
        this.labelId = labelId;
        this.label = label;
    }

    public String getLabelId() {
        return labelId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
