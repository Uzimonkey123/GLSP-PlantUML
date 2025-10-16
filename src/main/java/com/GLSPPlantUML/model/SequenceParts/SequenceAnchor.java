package com.GLSPPlantUML.model.SequenceParts;

import com.GLSPPlantUML.reconstructor.SourceElement;

public class SequenceAnchor extends SourceElement {
    private final SequenceNode participant1;
    private final SequenceNode participant2;
    private final String anchorId;
    private String label;
    private double topY = 0;

    public SequenceAnchor(SequenceNode participant1, SequenceNode participant2, String anchorId, String label) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.anchorId = anchorId;
        this.label = label;
    }

    public String getParticipant1Id() {
        return participant1.getId();
    }

    public String getParticipant2Id() {
        return participant2.getId();
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        if (!this.label.equals(label)) {
            setModified();
        }

        this.label = label;
    }

    public double getTopY() {
        return topY;
    }

    public void setTopY(double topY) {
        this.topY = topY;
    }
}
