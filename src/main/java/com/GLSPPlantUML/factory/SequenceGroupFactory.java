package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;
import java.util.Map;

public class SequenceGroupFactory {
    private final SequenceModel model;
    private final List<Double> messagesYPos;
    private final Map<String, Double> centre;
    private final List<GModelElement> elements;

    double minX = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;

    public SequenceGroupFactory(SequenceModel model, List<Double> messagesYPos, Map<String, Double> centre, List<GModelElement> elements) {
        this.model = model;
        this.messagesYPos = messagesYPos;
        this.centre = centre;
        this.elements = elements;
    }

    public void createGroups() {
        for (SequenceGroup seqGroup : model.groups) {
            SequenceMessage msg = model.messages.get(seqGroup.getStartIndex());

            int labelHeight = calculateLabelHeight(msg);
            int commentLength = seqGroup.getComment() == null ? 0 : calculateLabelWidth(seqGroup.getComment());
            int titleLength = calculateLabelWidth(seqGroup.getLabel());

            double y1 = messagesYPos.get(seqGroup.getStartIndex()) - (labelHeight + 10);
            double y2 = messagesYPos.get(seqGroup.getEndIndex() - 1) + 10;

            int maxGroupLevel = model.groups.stream().mapToInt(SequenceGroup::getLevel).max().orElse(0);
            int nestingPadding = (maxGroupLevel - seqGroup.getLevel() + 1) * 10;

            calculateMinMax(seqGroup);

            double x1 = minX - nestingPadding;
            double x2 = maxX + nestingPadding;

            System.err.println("Group " + seqGroup.getLabel() + " x1= " + x1 + " x2= " + x2);
            System.err.println("Group " + seqGroup.getLabel() + " y1= " + y1 + " y2= " + y2);

            // TODO: Move to builder + add separator flag to it (else/also)
//            elements.add(new GEdgeBuilder("group")
//                    .id("group-" + seqGroup.getLevel())
//                    .sourceId("[")
//                    .targetId("]")
//                    .addArgument("x1", x1)
//                    .addArgument("x2", x2)
//                    .addArgument("y1", y1)
//                    .addArgument("y2", y2)
//                    .build());
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
