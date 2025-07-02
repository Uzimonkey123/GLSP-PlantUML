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
    private final double extraOffset;
    private final boolean showFoot;

    public NodeBuild(SequenceNode node, double cursor, double nodeY, double totalHeight,
                     double extraOffset, boolean showFoot) {
        this.node = node;
        this.cursor = cursor;
        this.nodeY = nodeY;
        this.totalHeight = totalHeight;
        this.extraOffset = extraOffset;
        this.showFoot = showFoot;
    }

    public double getCenter() {
        return cursor + nodeWidth / 2;
    }

    public double getHalfWidth() {
        return nodeWidth / 2;
    }

    public double getNodeWidth() {
        return nodeWidth;
    }

    private int getMaxLength(String lines) {
        return Arrays.stream(lines.split("<br>"))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private StringBuilder removeSpecialChar() {
        // Get the lines of the original name
        String[] lines = node.getName().split("<br>");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // If there is a special char in stereotype, remove it
            if (i == 0 && node.getStereotypeChar() != '-') {
                line = line.substring(1).trim();
            }

            // Add the line to the string
            result.append(line);

            // If not last line, add br to indicate new line
            if (i < lines.length - 1) {
                result.append("<br>");
            }
        }

        return result;
    }

    @Override
    public GModelElement build() {
        // Get the label of the node, in case of stereotype check for first char
        String label = node.isStereotype()
                ? this.removeSpecialChar().toString()
                : node.getName();

        int lengthOnLine = getMaxLength(node.getName());
        this.nodeWidth = lengthOnLine * 8 + 20;

        double yOffset = node.isCreatedNode() ? extraOffset - 24 : extraOffset;
        double nodeStartY = nodeY + yOffset;

        int lineCount = label.split("<br>").length;
        int lineHeight = 14;
        int headerHeight = lineCount * lineHeight + 10;

        GLabelBuilder labelBuilder = new GLabelBuilder("label:participant")
                .text(label)
                .addArgument("width", nodeWidth);

        // Add stereotype char and color of its background if present
        if (node.isStereotype()) {
            labelBuilder.addArgument("stereotypeChar", String.valueOf(node.getStereotypeChar()));
            labelBuilder.addArgument("stereotypeCharColor", node.getCharColor());
        }

        return new GNodeBuilder(node.getType())
                .id(node.getName())
                .layout("vbox")
                .position(cursor, nodeStartY - headerHeight)
                .addArgument("background", node.getBackground())
                .addArgument("showFoot", showFoot)
                .addArgument("name", label)
                .size(nodeWidth, totalHeight - yOffset + 2 * headerHeight)
                .addArgument("headerHeight", headerHeight)
                .add(labelBuilder.build())
                .build();
    }
}