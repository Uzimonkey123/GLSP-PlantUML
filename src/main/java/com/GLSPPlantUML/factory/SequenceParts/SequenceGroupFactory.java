package com.GLSPPlantUML.factory.SequenceParts;

import com.GLSPPlantUML.builders.GroupBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.ArrayList;
import java.util.Collection;
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
    double globalMaxX = Double.MIN_VALUE; // Variable to keep track of the previous longest group x2

    public SequenceGroupFactory(SequenceModel model, List<Double> messagesYPos, Map<String, Double> centre, List<GModelElement> elements) {
        this.model = model;
        this.messagesYPos = messagesYPos;
        this.centre = centre;
        this.elements = elements;

        this.groupBuild = new GroupBuild();
    }

    public void createGroups() {
        Collection<SequenceGroup> reversedGroups = model.reversedGroups();
        globalMaxX = Double.MIN_VALUE;

        for (SequenceGroup seqGroup : reversedGroups) {
            minX = Double.MAX_VALUE;
            maxX = Double.MIN_VALUE;
            separatorYPos.clear();

            SequenceMessage msg = model.messages.get(seqGroup.getStartIndex());

            int labelHeight = calculateLabelHeight(msg);
            double commentLength = seqGroup.getComment() == null ? 0 : WidthCalculator.calculateWidth(seqGroup.getComment(), 10);
            double titleLength = WidthCalculator.calculateWidth(seqGroup.getLabel(), 10);

            double y1 = messagesYPos.get(seqGroup.getStartIndex()) - (labelHeight + 10);
            double y2 = messagesYPos.get(seqGroup.getEndIndex() - 1) + 7;

            int maxGroupLevel = model.groups.stream().mapToInt(SequenceGroup::getLevel).max().orElse(0);
            int nestingPadding = (maxGroupLevel - seqGroup.getLevel() + 1) * 10;

            calculateMinMax(seqGroup);
            double x1 = minX - nestingPadding;
            double x2 = maxX;
            // If maxX is smaller than the combination of labels, extend it
            if (maxX < commentLength + titleLength) {
                x2 = maxX + Math.max(commentLength, titleLength);
            }

            // Saving global max for the next level of group
            globalMaxX = Math.max(globalMaxX, x2);
            if (seqGroup.getLevel() > 0) {
                globalMaxX = Math.max(globalMaxX, x2);
                x2 = globalMaxX + nestingPadding;
            } else {
                // Reset globalMaxX for separate level 0 groups
                globalMaxX = x2 + nestingPadding;
                x2 += nestingPadding;
            }

            calculateSeparatorY(seqGroup);

            elements.add(groupBuild.buildGroupOutline(seqGroup, x1, x2, y1, y2, separatorYPos, titleLength));

            double labelPos = x1 + ((double) titleLength / 2);
            double commentPos = x1 + titleLength + 15 + ((double) commentLength / 2);

            elements.add(groupBuild.buildGroupLabel(seqGroup, labelPos, y1));
            elements.add(groupBuild.buildGroupComment(seqGroup, commentPos, y1));

            for (int i = 0; i < separatorYPos.size(); i++) {
                if (!seqGroup.getSeparatorLabel().get(i).isEmpty()) {
                    String label = seqGroup.getSeparatorLabel().get(i);
                    double labelLength = WidthCalculator.calculateWidth(label, 10);
                    double separatorLabelPos = x1 + (labelLength / 2);

                    elements.add(groupBuild.buildSeparatorLabel(label, separatorLabelPos, separatorYPos.get(i)));
                }
            }
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

    private void calculateMinMax(SequenceGroup seqGroup) {
        for (int i = seqGroup.getStartIndex(); i < seqGroup.getEndIndex(); i++) {
            SequenceMessage message = model.messages.get(i);
            double fromX = message.getFrom() != null ? centre.get(message.getFrom()) : -1;
            double toX = message.getTo() != null ? centre.get(message.getTo()) : -1;

            if (fromX != -1) {
                minX = Math.min(minX, fromX);
                maxX = Math.max(maxX, fromX);
            }

            if (toX != -1) {
                minX = Math.min(minX, toX);
                maxX = Math.max(maxX, toX);
            }
        }
    }
}
