package com.GLSPPlantUML.utils;

import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeGap {
    private final Map<String, Double> nodeGaps;

    public NodeGap(List<SequenceMessage> modelMessages) {
        this.nodeGaps = getNodeGaps(modelMessages);
    }

    private Map<String, Double> getNodeGaps(List<SequenceMessage> modelMessages) {
        Map<String, Double> finalGaps = new HashMap<>();

        for (SequenceMessage msg : modelMessages) {
            String from = msg.getFrom();
            String to = msg.getTo();
            if ((from == null && to == null) || from.equals(to)) continue; // In self messages, divider, delay.., ignore

            String mapKey = getMapKey(from, to);
            int msgLength = msg.getMessage().length();
            int numLength = msg.getNumbering().length();

            // Split the message into lines to get the longest
            int lengthOnLine = Arrays.stream(msg.getMessage().split("<br>"))
                    .mapToInt(String::length)
                    .max().orElse(0);

            double padding = 5;
            double charWidth = 4;
            double finalLength = Math.max(lengthOnLine, msgLength + numLength) * charWidth + padding;

            finalGaps.merge(mapKey, finalLength, Math::max); // Keep the bigger value present in the finalGaps
        }

        return finalGaps;
    }

    private String getMapKey(String from, String to) {
        return from.compareTo(to) < 0 ? from + "-" + to : to + "-" + from;
    }

    public double getGaps(String from, String to) {
        double baseGap = 40;
        return nodeGaps.getOrDefault(getMapKey(from, to), baseGap);
    }
}
