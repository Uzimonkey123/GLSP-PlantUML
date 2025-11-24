package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.diagrams.SequenceDiagram.utils.NodeGap;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

public class SequenceNodeFactory {
    private final int nodeY = 30;
    private double cursor = 40;

    private final SequenceModel model;
    private final NodeBuild nodeBuild;
    private final NodeGap gapCalculator;
    private final List<Double> messagesYPos;

    private final double totalHeight;
    private final List<GModelElement> elements = new ArrayList<>();
    private final Map<String, Double> centre = new HashMap<>();
    private final Map<String, Double> halfWidth = new HashMap<>();

    private SequenceNode currentNode;
    private final int padding = 20;
    private final int nodeOffset = 24;

    public SequenceNodeFactory(SequenceModel model, NodeBuild nodeBuild, double totalHeight,
                               List<Double> messagesYPos, NodeGap gapCalculator) {
        this.model = model;
        this.nodeBuild = nodeBuild;
        this.totalHeight = totalHeight;
        this.messagesYPos = messagesYPos;
        this.gapCalculator = gapCalculator;
    }

    public void createNodes() {
        double highestNode = Double.MAX_VALUE;
        double biggestHeight = Double.MIN_VALUE;
        boolean isHighNodePresent = false;

        for (SequenceNode node : model.participants) {
            this.currentNode = node;
            double createdOffset = 0;

            if (node.isCreatedNode()) {
                createdOffset = messagesYPos.get(node.getCreatedIndex()) - nodeY - nodeOffset;
            }

            if (node.getType().equals("ACTOR") || node.getType().equals("DATABASE")) {
                isHighNodePresent = true;
            }

            double nodeWidth = WidthCalculator.calculateWidth(currentNode.getName(), padding);
            String label = getLabel();
            int headerHeight = calculateHeaderHeight(label);
            double nodeStart = nodeY + createdOffset - headerHeight;
            highestNode = Math.min(highestNode, nodeStart);
            double height = totalHeight - createdOffset + 2 * headerHeight;

            double footerHeight = nodeStart + height;
            biggestHeight = Math.max(biggestHeight, footerHeight);

            GModelElement newNode = nodeBuild.buildNode(currentNode, cursor, nodeWidth, headerHeight,
                                                        height, label, nodeStart, model.showFoot);

            elements.add(newNode);
            centre.put(node.getId(), getNodeCenter(nodeWidth));
            halfWidth.put(node.getId(), getNodeHalfWidth(nodeWidth));

            String nextName = model.getNextParticipant(node.getId());
            if (!nextName.equals(node.getId())) {
                double gap = gapCalculator.getGaps(node.getId(), nextName);
                cursor += nodeWidth + gap + getNodeHalfWidth(nodeWidth);
            }
        }

        cursor += halfWidth.get(model.participants.getLast().getId()) + 2 * padding;

        // Add invisible nodes for incoming or outgoing messages
        createInvisibleNodes();

        // Add page details like header, title, footer
        createPageDetails(highestNode, isHighNodePresent, biggestHeight);
    }

    public List<GModelElement> getElements() {
        return elements;
    }

    public Map<String, Double> getCentre() {
        return centre;
    }

    public Map<String, Double> getHalfWidth() {
        return halfWidth;
    }

    public double getCursor() {
        return cursor;
    }

    private int calculateHeaderHeight(String label) {
        int lineCount = label.split("<br>").length;
        int lineHeight = 14;

        return lineCount * lineHeight + padding / 2;
    }

    private double getNodeCenter(double nodeWidth) {
        return this.cursor + nodeWidth / 2;
    }

    private double getNodeHalfWidth(double nodeWidth) {
        return nodeWidth / 2;
    }

    private StringBuilder removeSpecialChar() {
        String name = currentNode.getName();
        char stereotypeCharc = currentNode.getStereotypeChar();

        if (currentNode.isStereotype() && name.startsWith(stereotypeCharc + " ")) {
            name = name.substring(2);
        }

        // Remove the first line split so there is no empty line
        name = name.replaceFirst("^<br>", "");

        String[] lines = name.split("<br>");
        return new StringBuilder(String.join("<br>", lines));
    }

    private String getLabel() {
        // Get the label of the node, in case of stereotype check for first char
        return currentNode.isStereotype()
                ? this.removeSpecialChar().toString()
                : currentNode.getName();
    }

    private void createPageDetails(double highestNode, boolean isHighNodePresent, double biggestHeight) {
        nodeBuild.buildPageDetails(elements, model, totalHeight, centre, highestNode, isHighNodePresent, biggestHeight);
    }

    private void createInvisibleNodes() {
        nodeBuild.buildInvisibleNodes(elements, totalHeight, cursor, nodeY);
        centre.put("[", 0.0);
        centre.put("]", cursor);
    }
}
