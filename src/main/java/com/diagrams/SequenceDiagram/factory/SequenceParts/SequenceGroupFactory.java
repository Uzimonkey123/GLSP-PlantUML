package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.GroupBuild;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceGroup;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

public class SequenceGroupFactory {
    private final SequenceModel model;
    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final List<GModelElement> elements;
    private final List<GModelElement> tempElements;

    List<Double> separatorYPos = new ArrayList<>();
    GroupBuild groupBuild;

    double minX = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;
    double globalMaxX = Double.MIN_VALUE; // Variable to keep track of the previous longest group x2
    private final int padding = 10;
    private final int titleCommentGap = 15;
    private final int lineHeight = 14;

    public SequenceGroupFactory(SequenceModel model, List<Double> messagesYPos, Map<String, Double> centre, List<GModelElement> elements) {
        this.model = model;
        this.messagesYPos = messagesYPos;
        this.centre = centre;
        this.elements = elements;

        this.groupBuild = new GroupBuild();
        this.tempElements = new ArrayList<>();
    }

    List<SequenceGroup> postOrder(List<SequenceGroup> groups) {
        Deque<SequenceGroup> stack = new ArrayDeque<>();
        List<SequenceGroup> out = new ArrayList<>();

        for (SequenceGroup g : groups) {
            // Popping all groups ending before this one starts
            while (!stack.isEmpty() &&
                    g.getStartIndex() >= stack.peek().getEndIndex()) {
                out.add(stack.pop());
            }
            // Set as new "parent"
            stack.push(g);
        }

        // Pop the remaining
        while (!stack.isEmpty()) out.add(stack.pop());
        return out;
    }

    public void createGroups() {
        globalMaxX = Double.MIN_VALUE;
        tempElements.clear();

        boolean prevNested = false;
        boolean nestedOuter = false;
        int maxGroupLevel = 0;

        // Sort groups to have nested ones first, followed by outer before next group starts
        List<SequenceGroup> sortedGroups = postOrder(model.groups);

        for (SequenceGroup seqGroup : sortedGroups) {
            minX = Double.MAX_VALUE;
            maxX = Double.MIN_VALUE;
            separatorYPos.clear();

            SequenceMessage msg = model.messages.get(seqGroup.getStartIndex());

            int labelHeight = calculateLabelHeight(msg);
            for (int i = seqGroup.getStartIndex() + 1; i < seqGroup.getEndIndex(); i++) {
                SequenceMessage msgCheck = model.messages.get(i);
                if (!msgCheck.isParallel()) break;

                labelHeight = Math.max(labelHeight, calculateLabelHeight(msgCheck));
            }

            double titleLength = WidthCalculator.calculateWidth(seqGroup.getLabel(), padding);
            double commentLength = seqGroup.getComment() == null ? 0 : WidthCalculator.calculateWidth(seqGroup.getComment(), padding);

            double y1 = messagesYPos.get(seqGroup.getStartIndex()) - (labelHeight + padding);
            double y2 = messagesYPos.get(seqGroup.getEndIndex() - 1) + (lineHeight / 2);

            int currentLevel = seqGroup.getLevel();
            if (maxGroupLevel == 0 && currentLevel != 0) {
                maxGroupLevel = currentLevel;
                globalMaxX = Double.MIN_VALUE;
            }
            int nestingPadding = (maxGroupLevel - seqGroup.getLevel() + 1) * padding;

            calculateMinMax(seqGroup);
            double x1 = minX - nestingPadding;
            double x2 = maxX;
            // If maxX is smaller than the combination of labels, extend it
            if (maxX - 2 * padding - titleCommentGap < commentLength + titleLength) {
                x2 = maxX + Math.max(commentLength, titleLength);
            }

            // Check if we are inside nested groups
            boolean isNested = seqGroup.getLevel() > 0;

            if (prevNested && !isNested) {
                // If previous is nested, but current no, it is the first outer
                nestedOuter = true;
            }
            x2 = calculateX2(isNested, nestedOuter, nestingPadding, x2);

            calculateSeparatorY(seqGroup);

            tempElements.addFirst(groupBuild.buildGroupOutline(seqGroup, x1, x2, y1, y2, separatorYPos, titleLength));

            double labelPos = x1 + (titleLength / 2);
            double commentPos = x1 + titleLength + titleCommentGap + (commentLength / 2);

            tempElements.add(groupBuild.buildGroupLabel(seqGroup, labelPos, y1));
            tempElements.add(groupBuild.buildGroupComment(seqGroup, commentPos, y1));

            createSeparatorLabels(seqGroup, x1);

            prevNested = isNested;
            nestedOuter = false;
            if (currentLevel == 0) {
                maxGroupLevel = currentLevel;
            }
        }

        elements.addAll(tempElements);
    }

    private void calculateSeparatorY(SequenceGroup seqGroup) {
        for (Integer separatorIndex : seqGroup.getSeparatorList()) {
            SequenceMessage separatorMsg = model.messages.get(separatorIndex);
            int labelHeight = calculateLabelHeight(separatorMsg);
            double y = messagesYPos.get(separatorIndex) - (labelHeight + padding) + 2;
            separatorYPos.add(y);
        }
    }

    private void createSeparatorLabels(SequenceGroup seqGroup, double x1) {
        for (int i = 0; i < separatorYPos.size(); i++) {
            if (!seqGroup.getSeparatorLabel().get(i).isEmpty()) {
                String label = seqGroup.getSeparatorLabel().get(i);
                double labelLength = WidthCalculator.calculateWidth(label, padding);
                double separatorLabelPos = x1 + (labelLength / 2);
                String id = seqGroup.separatorId(i);

                tempElements.add(groupBuild.buildSeparatorLabel(id, label, separatorLabelPos, separatorYPos.get(i)));
            }
        }
    }

    private int calculateLabelHeight(SequenceMessage msg) {
        String[] lines = msg.getMessage().split("<br>");

        int lineCount = lines.length;
        return lineCount * lineHeight;
    }

    private void calculateMinMax(SequenceGroup seqGroup) {
        for (int i = seqGroup.getStartIndex(); i < seqGroup.getEndIndex(); i++) {
            SequenceMessage message = model.messages.get(i);
            double fromX = message.getFrom() != null ? centre.get(message.getFromId()) : -1;
            double toX = message.getTo() != null ? centre.get(message.getToId()) : -1;

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

    private double calculateX2(boolean isNested, boolean nestedOuter, int nestingPadding, double x2) {
        if (isNested) { // For nested we need to always save the x2 so the more outer group can use it
            globalMaxX = Math.max(globalMaxX, x2);
            return globalMaxX + nestingPadding;
        }

        // If it is the outer group of a nested one, use different padding
        double calculated = nestedOuter ? Math.max(globalMaxX, x2) + nestingPadding : x2 + padding;
        globalMaxX = calculated;

        return calculated;
    }
}
