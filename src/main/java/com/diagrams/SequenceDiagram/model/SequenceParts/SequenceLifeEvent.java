package com.diagrams.SequenceDiagram.model.SequenceParts;

import com.diagrams.SequenceDiagram.reconstructor.SourceElement;
import net.sourceforge.plantuml.klimt.color.HColor;

public class SequenceLifeEvent extends SourceElement {
    private final int startMessage;
    private final int endMessage;
    private int level = 0; // Depth of life event - the deeper, more to the right
    private final HColor background;

    public SequenceLifeEvent(int startMessage, int endMessage, HColor background) {
        this.startMessage = startMessage;
        this.endMessage = endMessage;
        this.background = background;
    }

    public int getStartMessage() {
        return startMessage;
    }

    public int getEndMessage() {
        return endMessage;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getBackground() {
        return this.background != null ? this.background.asString() : "#C0C0C0";
    }
}
