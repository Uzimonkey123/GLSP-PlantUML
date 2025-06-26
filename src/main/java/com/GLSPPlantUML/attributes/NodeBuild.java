package com.GLSPPlantUML.attributes;

import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class NodeBuild implements FactoryBuild {
    private final SequenceNode node;
    private final double cursor;
    private final double nodeY;
    private final double totalHeight;
    private double nodeWidth;
    private final int creationIndex;
    private final double extraOffset;
    private final boolean showFoot;

    public NodeBuild(SequenceNode node, double cursor, double nodeY, double totalHeight,
                     double extraOffset, int creationIndex, boolean showFoot) {
        this.node = node;
        this.cursor = cursor;
        this.nodeY = nodeY;
        this.totalHeight = totalHeight;
        this.extraOffset = extraOffset;
        this.creationIndex = creationIndex;
        this.showFoot = showFoot;
    }

    public double getCenter() {
        double textWidth = node.getName().length() * 8;
        this.nodeWidth = textWidth + 20;

        return cursor + nodeWidth / 2;
    }

    public double getHalfWidth() {
        return nodeWidth / 2;
    }

    public double getNodeWidth() {
        return nodeWidth;
    }


    @Override
    public GModelElement build() {
        double textWidth = node.getName().length() * 8;
        this.nodeWidth = textWidth + 20;

        double yOffset = creationIndex * 35 + extraOffset;
        double nodeStartY = nodeY + yOffset;

        return new GNodeBuilder(node.getType())
                .id(node.getName())
                .layout("vbox")
                .position(cursor, nodeStartY)
                .addArgument("background", node.getBackground())
                .addArgument("showFoot", showFoot)
                .size(nodeWidth, totalHeight - yOffset)
                .add(new GLabelBuilder().text(node.getName()).build())
                .build();
    }
}
