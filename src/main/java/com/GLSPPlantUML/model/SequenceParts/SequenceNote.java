package com.GLSPPlantUML.model.SequenceParts;

public class SequenceNote {
    private final String id;
    private String label;
    private final String position;
    private final String background;
    private final String shape;
    private boolean paralell = false;

    public SequenceNote(String id, String label, String position,  String background, String shape) {
        this.id = id;
        this.label = label;
        this.position = position;
        this.background = background;
        this.shape = shape;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPosition() {
        return position;
    }

    public String getBackground() {
        return background;
    }

    public String getShape() {
        return shape;
    }

    public boolean isParalell() {
        return paralell;
    }

    public void setParalell(boolean paralell) {
        this.paralell = paralell;
    }

    @Override
    public String toString() {
        return "SequenceNote{" + "id=" + id + ", label=" + label + ", position=" + position + '}';
    }
}
