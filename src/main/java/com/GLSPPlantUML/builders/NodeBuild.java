package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

import java.util.List;
import java.util.Map;

public class NodeBuild {
    public void buildInvisibleNodes(List<GModelElement> elements, double totalHeight, double cursor, double nodeY) {
        elements.add(new GNodeBuilder()
                .id("[")
                .layout("vbox")
                .position(0, nodeY)
                .size(0, totalHeight)
                .build());

        elements.add(new GNodeBuilder()
                .id("]")
                .layout("vbox")
                .position(cursor * 2, nodeY)
                .size(0, totalHeight)
                .build());
    }

    public void buildPageDetails(List<GModelElement> elements, SequenceModel model,
                                 double totalHeight, Map<String, Double> centre,
                                 double highestNode, boolean isHighNodePresent, double biggestHeight) {

        double firstCentre = centre.get(model.participants.getFirst().getId());
        double lastCentre = centre.get(model.participants.getLast().getId());

        double titleY = isHighNodePresent ? highestNode - 70 : highestNode - 20;
        titleY -= yOffset(model.title);

        double footerY = isHighNodePresent ? biggestHeight + 70 : biggestHeight + 20;
        //footerY += yOffset(model.footer);

        elements.add(new GLabelBuilder("label:html")
                .id("header")
                .size(WidthCalculator.calculateWidth(model.header, 0), yOffset(model.header))
                .position(lastCentre, titleY - 20 - yOffset(model.header))
                .text(model.header)
                .build());

        elements.add(new GLabelBuilder("label:html")
                .id("title")
                .size(WidthCalculator.calculateWidth(model.title, 0), yOffset(model.title))
                .position((firstCentre + lastCentre) / 2, titleY)
                .text(model.title)
                .build());

        elements.add(new GLabelBuilder("label:html")
                .id("footer")
                .size(WidthCalculator.calculateWidth(model.footer, 0), yOffset(model.footer))
                .position((firstCentre + lastCentre) / 2, footerY)
                .text(model.footer)
                .build());
    }

    public GModelElement buildNode(SequenceNode node, double cursor, double nodeWidth, double headerHeight,
                                   double height, String label, double nodeStart, boolean showFoot) {

        GLabelBuilder labelBuilder = new GLabelBuilder("label:participant")
                .id(node.getId() + "-label")
                .text(label)
                .addArgument("width", nodeWidth);

        // Add stereotype char and color of its background if present
        if (node.isStereotype()) {
            labelBuilder.addArgument("stereotypeChar", String.valueOf(node.getStereotypeChar()));
            labelBuilder.addArgument("stereotypeCharColor", node.getCharColor());
        }

        return new GNodeBuilder(node.getType())
                .id(node.getId())
                .layout("vbox")
                .position(cursor, nodeStart)
                .addArgument("background", node.getBackground())
                .addArgument("showFoot", showFoot)
                .addArgument("name", label)
                .size(nodeWidth, height)
                .addArgument("headerHeight", headerHeight)
                .add(labelBuilder.build())
                .build();
    }

    public void buildAnchorPoints(List<GModelElement> elements, SequenceAnchor anchor, double xCoord, double y,
                                  String from, Map<String, Double> halfWidth) {
        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-top")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), anchor.getTopY())
                .size(0, 0)
                .build());

        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-bottom")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), y)
                .size(0, 0)
                .build());
    }

    public GModelElement buildMainframe(SequenceModel model, double x, double y, double width, double height,
                                        double labelWidth, double labelHeight) {
        GLabelBuilder label = new GLabelBuilder("label:html")
                .id("mainframe-label")
                .text(model.mainframe);

        return new GNodeBuilder("mainframe")
                .id("mainframe")
                .layout("vbox")
                .position(x, y)
                .size(width, height)
                .add(label.build())
                .addArgument("labelWidth", labelWidth)
                .addArgument("labelHeight", labelHeight)
                .build();
    }

    private double yOffset(String lines) {
        int labelHeight = 14;

        int lineCount = lines.split("<br>").length;
        return lineCount > 1 ? lineCount * labelHeight : 6;
    }
}