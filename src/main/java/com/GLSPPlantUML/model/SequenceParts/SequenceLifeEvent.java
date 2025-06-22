package com.GLSPPlantUML.model.SequenceParts;

public class SequenceLifeEvent {
    private final int startMessage;
    private final int endMessage;
    private int level = 0; // Depth of life event - the deeper, more to the right

    public SequenceLifeEvent(int startMessage, int endMessage) {
        this.startMessage = startMessage;
        this.endMessage = endMessage;
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
}
