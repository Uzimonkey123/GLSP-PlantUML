/*
 * File: SequenceNodeFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating participant nodes, invisible nodes,and page-level details
 * Date: 7.5.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

import static com.diagrams.SequenceDiagram.factory.SequenceFactoryContext.*;

public class SequenceNodeFactory {
    private final SequenceFactoryContext ctx;
    private final NodeBuild nodeBuild;
    private final double totalHeight;

    private SequenceNode currentNode;

    public SequenceNodeFactory(SequenceFactoryContext ctx, NodeBuild nodeBuild, double totalHeight) {
        this.ctx = ctx;
        this.nodeBuild = nodeBuild;
        this.totalHeight = totalHeight;
    }

    /**
     * Creates all participant nodes, computes their centres, and adds invisible/page elements
     */
    public void createNodes() {
        SequenceModel model = ctx.getModel();
        Map<String, Double> centre = ctx.getCentre();
        Map<String, Double> halfWidth = ctx.getHalfWidth();
        List<GModelElement> elements = ctx.getElements();
        List<Double> messagesYPos = ctx.getMessagesYPos();

        double highestNode = Double.MAX_VALUE;
        double biggestHeight = Double.MIN_VALUE;
        boolean isHighNodePresent = false;
        double cursor = ctx.getCursor();

        int padding = 20;
        for (SequenceNode node : model.participants) {
            this.currentNode = node;
            double createdOffset = 0;

            if (node.isCreatedNode()) {
                int nodeOffset = 24;
                createdOffset = messagesYPos.get(node.getCreatedIndex()) - nodeY - nodeOffset;
            }

            if (node.getType().equals("ACTOR") || node.getType().equals("DATABASE")) {
                isHighNodePresent = true;
            }

            double nodeWidth = WidthCalculator.calculateWidth(currentNode.getName(), padding);
            String label = getLabel();
            int headerHeight = calculateHeaderHeight(label, padding / 2);
            double nodeStart = nodeY + createdOffset - headerHeight;
            highestNode = Math.min(highestNode, nodeStart);
            double height = totalHeight - createdOffset + 2 * headerHeight;

            double footerHeight = nodeStart + height;
            biggestHeight = Math.max(biggestHeight, footerHeight);

            GModelElement newNode = nodeBuild.buildNode(currentNode, cursor, nodeWidth, headerHeight,
                                                        height, label, nodeStart, model.showFoot);

            elements.add(newNode);
            centre.put(node.getId(), cursor + nodeWidth / 2);
            halfWidth.put(node.getId(), nodeWidth / 2);

            String nextName = model.getNextParticipant(node.getId());
            if (!nextName.equals(node.getId())) {
                double gap = ctx.getGapCalculator().getGaps(node.getId(), nextName);
                cursor += nodeWidth + gap + nodeWidth / 2;
            }
        }

        cursor += halfWidth.get(model.participants.getLast().getId()) + 2 * padding;
        ctx.setCursor(cursor);

        // Add invisible nodes for incoming or outgoing messages
        nodeBuild.buildInvisibleNodes(elements, totalHeight, cursor, nodeY);
        centre.put("[", 0.0);
        centre.put("]", cursor);

        // Add page details like header, title, footer
        nodeBuild.buildPageDetails(elements, model, totalHeight, centre, highestNode, isHighNodePresent, biggestHeight);
    }

    /**
     * Strips the stereotype character prefix from the node name
     */
    private StringBuilder removeSpecialChar() {
        String name = currentNode.getName();
        char stereotypeChar = currentNode.getStereotypeChar();

        if (currentNode.isStereotype() && name.startsWith(stereotypeChar + " ")) {
            name = name.substring(2);
        }

        // Remove the first line split so there is no empty line
        name = name.replaceFirst("^<br>", "");

        String[] lines = name.split("<br>");
        return new StringBuilder(String.join("<br>", lines));
    }

    /**
     * Returns the display label, handling stereotype prefix removal
     */
    private String getLabel() {
        // Get the label of the node, in case of stereotype check for first char
        return currentNode.isStereotype()
                ? this.removeSpecialChar().toString()
                : currentNode.getName();
    }
}
