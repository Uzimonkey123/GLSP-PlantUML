package com.GLSPPlantUML.model.SequenceParts;

import net.sourceforge.plantuml.klimt.color.HColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SequenceNode {
    private final String id;
    private String name;
    private final String type;
    private final int order;
    private final HColor background;
    private boolean createdNode;
    private int createdIndex;
    private final List<SequenceLifeEvent> lifeEvents = new ArrayList<>();
    private int destroyIndex = -1;
    private char stereotypeChar = '-';
    private String charColor = "#ADD1B2";
    private boolean isStereotype = false;
    private final List<String> engloberIds = new ArrayList<>();

    public SequenceNode(String id, String name, String type, int order, HColor background, boolean createdNode) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.order = order;
        this.background = background;
        this.createdNode = createdNode;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        List<SequenceLifeEvent> reversed = new ArrayList<>(lifeEvents);
        Collections.reverse(reversed);
        return reversed;
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

    public int getDestroyIndex() {
        return destroyIndex;
    }

    public void setDestroyIndex(int destroyIndex) {
        this.destroyIndex = destroyIndex;
    }

    public char getStereotypeChar() {
        // Empty char, replace it for client
        if ((int) stereotypeChar == 0) {
            stereotypeChar = '-';
        }

        return stereotypeChar;
    }

    public void setStereotypeChar(char stereotypeChar) {
        this.stereotypeChar = stereotypeChar;
    }

    public String getCharColor() {
        return charColor;
    }

    public void setCharColor(String charColor) {
        this.charColor = charColor;
    }

    public boolean isStereotype() {
        return isStereotype;
    }

    public void setStereotype(boolean isStereotype) {
        this.isStereotype = isStereotype;
    }

    public void addEngloberId(String id) {
        this.engloberIds.add(id);
    }

    public List<String> getEngloberIds() {
        return this.engloberIds;
    }
}