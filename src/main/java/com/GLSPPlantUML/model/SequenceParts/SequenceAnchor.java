package com.GLSPPlantUML.model.SequenceParts;

public class SequenceAnchor {
    private final String participant1;
    private final String participant2;
    private final String anchorId;
    private final String label;
    private double topY = 0;

    public SequenceAnchor(String participant1, String participant2, String anchorId, String label) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.anchorId = anchorId;
        this.label = label;
    }

    public String getParticipant1() {
        return participant1;
    }

    public String getParticipant2() {
        return participant2;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getLabel() {
        return label;
    }

    public double getTopY() {
        return topY;
    }

    public void setTopY(double topY) {
        this.topY = topY;
    }
}
