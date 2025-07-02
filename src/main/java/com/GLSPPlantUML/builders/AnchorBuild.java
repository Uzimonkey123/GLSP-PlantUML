package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GEdgePlacementBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.util.GConstants;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class AnchorBuild {
    private final SequenceAnchor anchor;
    private double xCoord;
    private final double bottomY;
    private final double gap;

    public AnchorBuild(SequenceAnchor anchor, double bottomY, double gap) {
        this.anchor = anchor;
        this.bottomY = bottomY;
        this.gap = gap;
    }

    public double getXCoord(double from, double to) {
        // Calculate the x coordinate to know if it is left or right from the give nodes
        if (from > to) {
            this.xCoord = from - gap / 2;
        } else {
            this.xCoord = to - gap / 2;
        }

        return this.xCoord;
    }


    public GModelElement build() {
        return new GEdgeBuilder("anchor-arrow")
                .id(anchor.getAnchorId())
                .sourceId(anchor.getAnchorId() + "-top")
                .targetId(anchor.getAnchorId() + "-bottom")
                .addRoutingPoint(point(xCoord, anchor.getTopY()))
                .addRoutingPoint(point(xCoord, bottomY))
                .add(new GLabelBuilder("label:html")
                        .text(anchor.getLabel())
                        .edgePlacement(new GEdgePlacementBuilder()
                                .side(GConstants.EdgeSide.RIGHT) // To the right from the label
                                .position(0.5d) // center
                                .offset(-4d)
                                .rotate(false)
                                .build())
                        .build())
                .build();
    }
}
