package com.GLSPPlantUML.factory.SequenceParts;

import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.NodeGap;
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
            double createdOffset = 0.0;

            if (node.isCreatedNode()) {
                createdOffset = messagesYPos.get(node.getCreatedIndex()) - nodeY - 24;
            }

            if (node.getType().equals("ACTOR") || node.getType().equals("DATABASE")) {
                isHighNodePresent = true;
            }

            double nodeWidth = WidthCalculator.calculateWidth(currentNode.getName(), 20);
            String label = getLabel();
            int headerHeight = calculateHeaderHeight(label);
            double nodeStart = nodeY + createdOffset - headerHeight;
            highestNode = Math.min(highestNode, nodeStart);
            double height = totalHeight - createdOffset + 2 * headerHeight;

            double footerHeight = nodeStart + height;
            biggestHeight = Math.max(biggestHeight, footerHeight);

            System.err.println("Height: " + height);
            System.err.println("Nodestart: " + nodeStart);
            System.err.println("HeaderHeight: " + headerHeight);

            GModelElement newNode = nodeBuild.buildNode(currentNode, cursor, nodeWidth, headerHeight,
                                                        height,  label, nodeStart, model.showFoot);

            elements.add(newNode);
            centre.put(node.getId(), getNodeCenter(nodeWidth));
            halfWidth.put(node.getId(), getNodeHalfWidth(nodeWidth));

            String nextName = model.getNextParticipant(node.getId());
            if (!nextName.equals(node.getId())) {
                double gap = gapCalculator.getGaps(node.getId(), nextName);
                cursor += nodeWidth + gap + getNodeHalfWidth(nodeWidth);
            }
        }
        System.err.println("Foot: " + biggestHeight);
        System.err.println("HighestNode: " + highestNode);
        System.err.println("IsHighNodePresent: " + isHighNodePresent);

        cursor += halfWidth.get(model.participants.getLast().getId()) + 40;

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

        return lineCount * lineHeight + 10;
    }

    private double getNodeCenter(double nodeWidth) {
        return this.cursor + nodeWidth / 2;
    }

    private double getNodeHalfWidth(double nodeWidth) {
        return nodeWidth / 2;
    }

    private StringBuilder removeSpecialChar() {
        // Get the lines of the original name
        String[] lines = currentNode.getName().split("<br>");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // If there is a special char in stereotype, remove it
            if (i == 0 && currentNode.getStereotypeChar() != '-') {
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
