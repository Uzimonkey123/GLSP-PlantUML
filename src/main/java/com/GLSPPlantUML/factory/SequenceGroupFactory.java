package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.builders.GroupBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import org.eclipse.glsp.graph.GModelElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SequenceGroupFactory {
    private final SequenceModel model;
    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final List<GModelElement> elements;

    List<Double> separatorYPos = new ArrayList<>();
    GroupBuild groupBuild;

    double minX = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;

    public SequenceGroupFactory(SequenceModel model, List<Double> messagesYPos, Map<String, Double> centre, List<GModelElement> elements) {
        this.model = model;
        this.messagesYPos = messagesYPos;
        this.centre = centre;
        this.elements = elements;

        this.groupBuild = new GroupBuild();
    }

    public void createGroups() {
        for (SequenceGroup seqGroup : model.groups) {
            minX = Double.MAX_VALUE;
            maxX = Double.MIN_VALUE;

            SequenceMessage msg = model.messages.get(seqGroup.getStartIndex());

            int labelHeight = calculateLabelHeight(msg);
            int commentLength = seqGroup.getComment() == null ? 0 : calculateLabelWidth(seqGroup.getComment());
            int titleLength = calculateLabelWidth(seqGroup.getLabel());

            double y1 = messagesYPos.get(seqGroup.getStartIndex()) - (labelHeight + 10);
            double y2 = messagesYPos.get(seqGroup.getEndIndex() - 1) + 7;

            int maxGroupLevel = model.groups.stream().mapToInt(SequenceGroup::getLevel).max().orElse(0);
            int nestingPadding = (maxGroupLevel - seqGroup.getLevel() + 1) * 10;

            calculateMinMax(seqGroup);
            double x1 = minX - nestingPadding;
            double x2 = maxX + nestingPadding;

            calculateSeparatorY(seqGroup);

            // TODO: Add group title and comments
            elements.add(groupBuild.buildGroupOutline(seqGroup, x1, x2, y1, y2, separatorYPos));
        }
    }

    private void calculateSeparatorY(SequenceGroup seqGroup) {
        for (Integer separatorIndex : seqGroup.getSeparatorList()) {
            SequenceMessage separatorMsg = model.messages.get(separatorIndex);
            int labelHeight = calculateLabelHeight(separatorMsg);
            double y = messagesYPos.get(separatorIndex) - (labelHeight + 10);
            separatorYPos.add(y);
        }
    }

    private int calculateLabelHeight(SequenceMessage msg) {
        String[] lines = msg.getMessage().split("<br>");

        int lineCount = lines.length;
        return lineCount * 14;
    }

    private int calculateLabelWidth(String label) {
        return label.length() * 8 + 5;
    }

    private void calculateMinMax(SequenceGroup seqGroup) {
        for (int i = seqGroup.getStartIndex(); i < seqGroup.getEndIndex(); i++) {
            SequenceMessage message = model.messages.get(i);
            double fromX = centre.get(message.getFrom());
            double toX = centre.get(message.getTo());

            minX = Math.min(minX, Math.min(fromX, toX));
            maxX = Math.max(maxX, Math.max(fromX, toX));
        }
    }
}
