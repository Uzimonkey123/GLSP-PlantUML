/*
 * File: SequenceEnglober.java
 * Author: Norman Babiak
 * Description: Englober box representation for sequence diagram
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.model.SequenceParts;

import com.diagrams.SequenceDiagram.reconstructor.SourceElement;

public class SequenceEnglober extends SourceElement {
    private final String id;
    private String label;
    private final String parentId;
    private final String color;
    private final int level;
    private String startParticipantId;
    private String endParticipantId;

    public SequenceEnglober(String id, String label, String parentId, String color, int level) {
        this.id = id;
        this.label = label;
        this.parentId = parentId;
        this.color = color;
        this.level = level;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        if (this.label != null && !this.label.equals(label)) {
            setModified();
        }

        this.label = label;
    }

    public String getParentId() {
        return parentId;
    }

    public String getColor() {
        return color;
    }

    public int getLevel() {
        return level;
    }

    public String getStartParticipantId() {
        return startParticipantId;
    }

    public String getEndParticipantId() {
        return endParticipantId;
    }

    public void setStartParticipantId(String id) {
        this.startParticipantId = id;
    }

    public void setEndParticipantId(String id) {
        this.endParticipantId = id;
    }
}
