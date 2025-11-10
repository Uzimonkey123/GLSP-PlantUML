package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.utils.WidthCalculator;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.graph.GPoint;

import java.util.List;
import java.util.Map;

public class SequenceMainframeFactory {
    private final SequenceModel model;
    private final Map<String, Double> centre;
    private final Map<String, Double> halfWidth;
    private final List<GModelElement> elements;
    private final NodeBuild nodeBuild;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private final int padding = 10;

    public SequenceMainframeFactory(SequenceModel model, Map<String, Double> centre,
                                    Map<String, Double> halfWidth, List<GModelElement> elements) {
        this.model = model;
        this.centre = centre;
        this.halfWidth = halfWidth;
        this.elements = elements;
        this.nodeBuild = new NodeBuild();

        minX = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;

        minY = Double.MAX_VALUE;
        maxY = Double.MIN_VALUE;
    }

    public void createMainframe() {
        computeMainframe();

        if (model.invisibleNodes) {
            minX -= padding;
            maxX += padding;
        } else {
            String firstParticipant = model.participants.getFirst().getId();
            minX = centre.get(firstParticipant) - halfWidth.get(firstParticipant) - padding;

            String lastParticipant = model.participants.getLast().getId();
            maxX = centre.get(lastParticipant) + halfWidth.get(lastParticipant) + padding;
        }

        minY -= padding;
        maxY += calculateHeaderHeight(model.footer);

        double labelWidth = WidthCalculator.calculateWidth(model.mainframe, 0);
        double labelHeight = calculateHeaderHeight(model.mainframe) - padding;

        minY -= labelHeight;
        maxX = Math.max(maxX, minX + labelWidth);

        double frameWidth = maxX - minX;
        double frameHeight = maxY - minY + labelHeight;

        elements.add(nodeBuild.buildMainframe(model, minX, minY, frameWidth, frameHeight, labelWidth, labelHeight));
    }

    private void computeMainframe() {
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

    private double calculateHeaderHeight(String label) {
        int lineCount = label.split("<br>").length;
        int lineHeight = 14;

        return lineCount * lineHeight + padding;
    }
}
