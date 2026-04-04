/*
 * File: SequenceMainframeFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating the mainframe border and label around the entire sequence diagram
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.graph.GPoint;

import java.util.List;
import java.util.Map;

import static com.diagrams.SequenceDiagram.factory.SequenceFactoryContext.*;

public class SequenceMainframeFactory {
    private final SequenceFactoryContext ctx;
    private final NodeBuild nodeBuild;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    public SequenceMainframeFactory(SequenceFactoryContext ctx) {
        this.ctx = ctx;
        this.nodeBuild = new NodeBuild();

        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;

        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;
    }

    public void createMainframe() {
        SequenceModel model = ctx.getModel();
        Map<String, Double> centre = ctx.getCentre();
        Map<String, Double> halfWidth = ctx.getHalfWidth();
        List<GModelElement> elements = ctx.getElements();

        computeMainframe(elements);

        if (model.invisibleNodes) {
            minX -= defPadding;
            maxX += defPadding;

        } else {
            String firstParticipant = model.participants.getFirst().getId();
            minX = centre.get(firstParticipant) - halfWidth.get(firstParticipant) - defPadding;

            String lastParticipant = model.participants.getLast().getId();
            maxX = centre.get(lastParticipant) + halfWidth.get(lastParticipant) + defPadding;
        }

        minY -= defPadding;
        maxY += calculateHeaderHeight(model.footer, defPadding);

        double labelWidth = WidthCalculator.calculateWidth(model.mainframe, 0);
        double labelHeight = calculateHeaderHeight(model.mainframe, defPadding) - defPadding;

        minY -= labelHeight;
        maxX = Math.max(maxX, minX + labelWidth);

        double frameWidth = maxX - minX;
        double frameHeight = maxY - minY + labelHeight;

        elements.add(nodeBuild.buildMainframe(model, minX, minY, frameWidth, frameHeight, labelWidth, labelHeight));
    }

    private void computeMainframe(List<GModelElement> elements) {
        for (GModelElement elem : elements) {
            if (elem instanceof GNode) {
                GPoint point = ((GNode) elem).getPosition();
                updateFrame(point);
            } else if (elem instanceof GLabel) {
                GPoint point = ((GLabel) elem).getPosition();
                updateFrame(point);
            }
        }
    }

    private void updateFrame(GPoint point) {
        if (point == null) return;

        minX = Math.min(minX, point.getX());
        minY = Math.min(minY, point.getY());
        maxX = Math.max(maxX, point.getX());
        maxY = Math.max(maxY, point.getY());
    }
}
