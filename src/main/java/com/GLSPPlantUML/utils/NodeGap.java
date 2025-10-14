package com.GLSPPlantUML.utils;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNote;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeGap {
    private final Map<String, Double> nodeGaps;
    private final SequenceModel model;

    private final double padding = 5;

    public NodeGap(SequenceModel model) {
        this.model = model;
        this.nodeGaps = getNodeGaps(model.messages);
    }

    private Map<String, Double> getNodeGaps(List<SequenceMessage> modelMessages) {
        Map<String, Double> finalGaps = new HashMap<>();
        SequenceMessage prevMessage = null;

        for (SequenceMessage msg : modelMessages) {
            String from = msg.getFromId();
            String to = msg.getToId();
            if ((from == null && to == null) || (from != null && from.equals(to))) continue; // In self messages, divider, delay.., ignore

            String mapKey = getMapKey(from, to);
            int msgLength = msg.getMessage().length();
            int numLength = msg.getNumbering().length();

            // Split the message into lines to get the longest
            int lengthOnLine = Arrays.stream(msg.getMessage().split("<br>"))
                    .mapToInt(String::length)
                    .max().orElse(0);

            double charWidth = 4;
            double finalLength = Math.max(lengthOnLine, msgLength + numLength) * charWidth + padding;

            finalGaps.merge(mapKey, finalLength, Math::max); // Keep the bigger value present in the finalGaps

            // Check for parallel notes to add more offset between the two nodes, so they do not overlap
            handleParallelNotes(msg, prevMessage, from, finalGaps);

            prevMessage = msg;
        }

        return finalGaps;
    }

    private void handleParallelNotes(SequenceMessage msg, SequenceMessage prevMessage, String from, Map<String, Double> finalGaps) {
        if (msg.getNotes() == null) return;

        // Get the current node width
        double maxCurrentParallelWidth = msg.getNotes().stream()
                .filter(SequenceNote::isParalell)
                .mapToDouble(note -> WidthCalculator.calculateWidth(note.getLabel(), 0) + padding)
                .max()
                .orElse(0);

        if (maxCurrentParallelWidth <= 0) return; // If not parallel

        double maxPrevWidth = 0;
        if (prevMessage != null && prevMessage.getNotes() != null) {
            // Get the previous note width regardless if it is parallel or not
            maxPrevWidth = prevMessage.getNotes().stream()
                    .mapToDouble(note -> WidthCalculator.calculateWidth(note.getLabel(), 0) + padding)
                    .max()
                    .orElse(0);
        }

        String prev = model.getPreviousParticipant(from); // Previous participant to set in the nodeGaps properly
        double gapWidth = (maxCurrentParallelWidth + maxPrevWidth) / 2 - 4 * padding;
        finalGaps.merge(prev + "-" + from, gapWidth, Math::max);
    }

    private String getMapKey(String from, String to) {
        if (from == null) {
            return to;
        }

        if (to == null) {
            return from;
        }

        return from.compareTo(to) < 0 ? from + "-" + to : to + "-" + from;
    }

    public double getGaps(String from, String to) {
        double baseGap = 20;
        return nodeGaps.getOrDefault(getMapKey(from, to), baseGap);
    }
}
