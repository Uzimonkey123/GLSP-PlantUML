package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.EngloberBuild;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceEnglober;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

public class SequenceEngloberFactory {
    private static class EngloberRange {
        private final List<SequenceNode> participants;
        private final List<SequenceEnglober> englobers;
        private double highNode = 0;

        public EngloberRange(List<SequenceNode> participants, List<SequenceEnglober> englobers) {
            this.participants = participants;
            this.englobers = englobers;
        }

        public void calculateEngloberRange() {
            Map<String, String> startMap = new HashMap<>();
            Map<String, String> endMap = new HashMap<>();

            for (SequenceNode p : this.participants) {
                if (p.getType().equals("ACTOR") || p.getType().equals("DATABASE")) {
                    highNode = 35;
                }

                for (String engloberId : p.getEngloberIds()) {
                    if (!startMap.containsKey(engloberId)) {
                        startMap.put(engloberId, p.getId());
                    }
                    endMap.put(engloberId, p.getId());
                }
            }

            for (SequenceEnglober englober : this.englobers) {
                englober.setStartParticipantId(startMap.get(englober.getId()));
                englober.setEndParticipantId(endMap.get(englober.getId()));
            }
        }
    }

    private final SequenceModel model;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<GModelElement> element;
    private final EngloberRange engloberRange;
    private final EngloberBuild engloberBuild;
    private final double totalHeight;
    private final double maxY = Double.MIN_VALUE;
    private final double nodeY = 30;
    private final int padding = 10;

    public SequenceEngloberFactory(SequenceModel model, Map<String, Double> centre,
                                   Map<String, Double> halfWidth, List<GModelElement> element, double totalHeight) {
        this.model = model;
        this.centre = centre;
        this.halfWidth = halfWidth;
        this.element = element;
        this.totalHeight = totalHeight;

        this.engloberRange = new EngloberRange(model.participants, model.englobers);
        this.engloberBuild = new EngloberBuild();
    }

    public void createEnglobers() {
        Collection<SequenceEnglober> reversed = model.reversedEnglobers();
        engloberRange.calculateEngloberRange();

        int maxHeaderHeight = model.englobers.stream()
                .mapToInt(e -> {
                    int h1 = calculateHeaderHeight(model.getNode(e.getStartParticipantId()).getName());
                    int h2 = calculateHeaderHeight(model.getNode(e.getEndParticipantId()).getName());
                    return Math.max(h1, h2);
                }).max().orElse(0);

        int maxLabelHeight = model.englobers.stream()
                .mapToInt(e -> calculateHeaderHeight(e.getLabel()))
                .max().orElse(0);
        int labelOffset = maxLabelHeight + padding;

        for (SequenceEnglober box : reversed) {

            double x1 = centre.get(box.getStartParticipantId()) - halfWidth.get(box.getStartParticipantId());
            double x2 = centre.get(box.getEndParticipantId()) + halfWidth.get(box.getEndParticipantId());

            double boxWidth = x2 - x1;
            double labelWidth = WidthCalculator.calculateWidth(box.getLabel(), padding);

            if (labelWidth > boxWidth) {
                x1 -= labelWidth;
            }

            double y1 = nodeY - maxHeaderHeight - (5 * padding) - labelOffset;
            double y2 = Math.max(maxY, (6 * padding) + totalHeight + 2 * maxHeaderHeight);

            element.addFirst(engloberBuild.buildEngloberBox(box, x1, x2, y1, y2, labelOffset, engloberRange.highNode));
        }
    }

    private int calculateHeaderHeight(String label) {
        int lineCount = label.split("<br>").length;
        int lineHeight = 14;

        return lineCount * lineHeight + padding;
    }
}
