package com.diagrams.ClassDiagram.model;

import com.diagrams.ClassDiagram.reconstructor.SourceElement;

public class NodePosition extends SourceElement {
    private double x;
    private double y;

    public NodePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setModified(boolean modified) {
        if (modified) {
            setModified();

        } else {
            clearModified();
        }
    }
}