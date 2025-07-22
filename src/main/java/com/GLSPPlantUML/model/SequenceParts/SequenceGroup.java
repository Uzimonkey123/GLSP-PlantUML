package com.GLSPPlantUML.model.SequenceParts;

import java.util.ArrayList;
import java.util.List;

public class SequenceGroup {
    private final int startIndex;
    private int endIndex = -1;
    private String label;
    private String comment = "";
    private final int level;
    private final List<Integer> separatorList; // Message index for all separators if available
    private final List<String> separatorLabel;
    private boolean isGroup = false;
    private String backColor = "none";
    private String elementColor = "grey";

    public SequenceGroup(int startIndex, String label, String comment, int level) {
        this.startIndex = startIndex;
        this.label = label;
        this.comment = comment;
        this.level = level;
        separatorList = new ArrayList<>();
        separatorLabel = new ArrayList<>();

        switch(label) {
            case "alt":
            case "opt":
            case "loop":
            case "par":
            case "break":
            case "critical":
                isGroup = false;
                break;
            default:
                isGroup = true;
        }
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public String getLabel() {
        if (label.equals("group") && !comment.isEmpty()) {
            label = comment;
        }

        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        if (isGroup && label.equals(comment)) {
            comment = "";
            return "";
        }

        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getLevel() {
        return level;
    }

    public void addSeparator(int separator) {
        separatorList.add(separator);
    }

    public List<Integer> getSeparatorList() {
        return separatorList;
    }

    public void addSeparatorLabel(String separatorLabel) {
        this.separatorLabel.add(separatorLabel);
    }

    public List<String> getSeparatorLabel() {
        return separatorLabel;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String getBackColor() {
        return backColor;
    }

    public void setBackColor(String backColor) {
        this.backColor = backColor;
    }

    public String getElementColor() {
        return elementColor;
    }

    public void setElementColor(String elementColor) {
        this.elementColor = elementColor;
    }
}
