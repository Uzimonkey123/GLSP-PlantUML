package com.diagrams.SequenceDiagram.model.SequenceParts;

import com.diagrams.SequenceDiagram.reconstructor.SourceElement;
import net.sourceforge.plantuml.klimt.color.HColor;

public class SequenceLifeEvent extends SourceElement {
    private int startMessage;
    private int endMessage;
    private int level = 0; // Depth of life event - the deeper, more to the right
    private final HColor background;
    private boolean inlineStart = false;
    private boolean inlineEnd = false;
    private boolean returnEnd = false;
    private String startMarker = "";
    private String endMarker = "";

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

    public void setStartMessage(int startMessage) {
        this.startMessage = startMessage;
    }

    public void setEndMessage(int endMessage) {
        this.endMessage = endMessage;
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

    public boolean isInlineStart() {
        return inlineStart;
    }

    public void setInlineStart(boolean inlineStart) {
        this.inlineStart = inlineStart;
    }

    public boolean isInlineEnd() {
        return inlineEnd;
    }

    public void setInlineEnd(boolean inlineEnd) {
        this.inlineEnd = inlineEnd;
    }

    public boolean isReturnEnd() {
        return returnEnd;
    }

    public void setReturnEnd(boolean returnEnd) {
        this.returnEnd = returnEnd;
    }

    public String getStartMarker() {
        return startMarker;
    }

    public void setStartMarker(String startMarker) {
        this.startMarker = startMarker;
    }

    public String getEndMarker() {
        return endMarker;
    }

    public void setEndMarker(String endMarker) {
        this.endMarker = endMarker;
    }
}