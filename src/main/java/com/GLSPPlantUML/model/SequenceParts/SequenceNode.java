package com.GLSPPlantUML.model.SequenceParts;

import net.sourceforge.plantuml.klimt.color.HColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SequenceNode {
    private final String name;
    private final String type;
    private final int order;
    private final HColor background;
    private boolean createdNode;
    private int createdIndex;
    private final List<SequenceLifeEvent> lifeEvents = new ArrayList<>();

    public SequenceNode(String name, String type, int order, HColor background, boolean createdNode) {
        this.name = name;
        this.type = type;
        this.order = order;
        this.background = background;
        this.createdNode = createdNode;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public boolean isCreatedNode() {
        return createdNode;
    }

    public void setCreatedNode(boolean createdNode) {
        this.createdNode = createdNode;
    }

    public int getCreatedIndex() {
        return createdIndex;
    }

    public void setCreatedIndex(int createdIndex) {
        this.createdIndex = createdIndex + 1;
    }

    public String getBackground() {
        return this.background != null ? this.background.asString() : "#5d4949";
    }

    public List<SequenceLifeEvent> getLifeEvents() {
        return lifeEvents;
    }

    public void addLifeEvent(SequenceLifeEvent lifeEvent) {
        this.lifeEvents.add(lifeEvent);
    }

    public Optional<SequenceLifeEvent> getLifeEventAt(int messageIndex) {
        return lifeEvents.stream()
                .filter(lifeEvent ->
                        messageIndex >= lifeEvent.getStartMessage() &&
                        messageIndex <= lifeEvent.getEndMessage())
                .findFirst();
    }
}