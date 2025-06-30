package com.GLSPPlantUML.attributes;

import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

import java.util.Arrays;

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
        int lengthOnLine = Arrays.stream(node.getName().split("<br>"))
                .mapToInt(String::length)
                .max()
                .orElse(0);

        this.nodeWidth = lengthOnLine * 8 + 20;

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
        int lengthOnLine = Arrays.stream(node.getName().split("<br>"))
                .mapToInt(String::length)
                .max()
                .orElse(0);

        this.nodeWidth = lengthOnLine * 8 + 20;

        double yOffset = creationIndex * 35 + extraOffset;
        double nodeStartY = nodeY + yOffset;

        String label = node.getName();
        int lineCount = label.split("<br>").length;
        int lineHeight = 14;
        int headerHeight = lineCount * lineHeight + 10;

        return new GNodeBuilder(node.getType())
                .id(node.getName())
                .layout("vbox")
                .position(cursor, nodeStartY - headerHeight)
                .addArgument("background", node.getBackground())
                .addArgument("showFoot", showFoot)
                .addArgument("name", node.getName())
                .size(nodeWidth, totalHeight - yOffset + 2*headerHeight)
                .addArgument("headerHeight", headerHeight)
                .add(new GLabelBuilder("label:participant")
                        .text(node.getName())
                        .addArgument("width", nodeWidth)
                        .build())
                .build();
    }
}
