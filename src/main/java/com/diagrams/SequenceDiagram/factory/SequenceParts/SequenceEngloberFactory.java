/*
 * File: SequenceEngloberFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating participant englober boxes
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.EngloberBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceEnglober;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

import static com.diagrams.SequenceDiagram.factory.SequenceFactoryContext.*;

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

    private final SequenceFactoryContext ctx;
    private final EngloberRange engloberRange;
    private final EngloberBuild engloberBuild;
    private final double totalHeight;

    public SequenceEngloberFactory(SequenceFactoryContext ctx, double totalHeight) {
        this.ctx = ctx;
        this.totalHeight = totalHeight;

        SequenceModel model = ctx.getModel();
        this.engloberRange = new EngloberRange(model.participants, model.englobers);
        this.engloberBuild = new EngloberBuild();
    }

    public void createEnglobers() {
        SequenceModel model = ctx.getModel();
        Map<String, Double> centre = ctx.getCentre();
        Map<String, Double> halfWidth = ctx.getHalfWidth();
        List<GModelElement> elements = ctx.getElements();

        Collection<SequenceEnglober> reversed = model.reversedEnglobers();
        engloberRange.calculateEngloberRange();

        int maxHeaderHeight = model.englobers.stream()
                .mapToInt(e -> {
                    int h1 = calculateHeaderHeight(model.getNode(e.getStartParticipantId()).getName(), defPadding);
                    int h2 = calculateHeaderHeight(model.getNode(e.getEndParticipantId()).getName(), defPadding);
                    return Math.max(h1, h2);
                }).max().orElse(0);

        int maxLabelHeight = model.englobers.stream()
                .mapToInt(e -> calculateHeaderHeight(e.getLabel(), defPadding))
                .max().orElse(0);
        int labelOffset = maxLabelHeight + defPadding;

        for (SequenceEnglober box : reversed) {
            double x1 = centre.get(box.getStartParticipantId()) - halfWidth.get(box.getStartParticipantId());
            double x2 = centre.get(box.getEndParticipantId()) + halfWidth.get(box.getEndParticipantId());

            double boxWidth = x2 - x1;
            double labelWidth = WidthCalculator.calculateWidth(box.getLabel(), defPadding);

            if (labelWidth > boxWidth) {
                x1 -= labelWidth;
            }

            double y1 = nodeY - maxHeaderHeight - (5 * defPadding) - labelOffset;
            double y2 = Math.max(Double.MIN_VALUE, (6 * defPadding) + totalHeight + 2 * maxHeaderHeight);

            elements.addFirst(engloberBuild.buildEngloberBox(box, x1, x2, y1, y2, labelOffset, engloberRange.highNode));
        }
    }
}
