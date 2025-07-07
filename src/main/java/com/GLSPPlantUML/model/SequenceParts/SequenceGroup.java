package com.GLSPPlantUML.model.SequenceParts;

import java.util.ArrayList;
import java.util.List;

public class SequenceGroup {
    private final int startIndex;
    private int endIndex = -1;
    private final String label;
    private String comment = "";
    private final int level;
    private final List<Integer> separatorList; // Message index for all separators if available
    private final List<String> separatorLabel;

    public SequenceGroup(int startIndex, String label, String comment, int level) {
        this.startIndex = startIndex;
        this.label = label;
        this.comment = comment;
        this.level = level;
        separatorList = new ArrayList<>();
        separatorLabel = new ArrayList<>();
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
            return comment;
        }

        return label;
    }

    public String getComment() {
        if (label.equals("group") && !comment.isEmpty()) {
            return "";
        }

        return comment;
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
}
