/*
 * File: SequenceFactoryContext.java
 * Author: Norman Babiak
 * Description: Shared context for all sequence diagram sub-factories
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.factory;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.utils.NodeGap;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

public class SequenceFactoryContext {

    private final SequenceModel model;
    private final Map<String, Double> centre = new HashMap<>(); // Map to store the middle of all nodes for lifeline
    private final Map<String, Double> halfWidth = new HashMap<>(); // Half size of nodes for created node arrows
    private final List<GModelElement> elements = new ArrayList<>();
    private final List<Double> messagesYPos = new ArrayList<>();
    private final List<Double> lifeEventYPos = new ArrayList<>();
    private double cursor = 40;

    private NodeGap gapCalculator;

    public static final int lineHeight = 14;
    public static final int defPadding = 10;
    public static final double nodeY = 30;

    public SequenceFactoryContext(SequenceModel model) {
        this.model = model;
    }

    public SequenceModel getModel() {
        return model;
    }

    public Map<String, Double> getCentre() {
        return centre;
    }

    public Map<String, Double> getHalfWidth() {
        return halfWidth;
    }

    public List<GModelElement> getElements() {
        return elements;
    }

    public List<Double> getMessagesYPos() {
        return messagesYPos;
    }

    public List<Double> getLifeEventYPos() {
        return lifeEventYPos;
    }

    public double getCursor() {
        return cursor;
    }

    public void setCursor(double cursor) {
        this.cursor = cursor;
    }

    public NodeGap getGapCalculator() {
        return gapCalculator;
    }

    public void setGapCalculator(NodeGap gapCalculator) {
        this.gapCalculator = gapCalculator;
    }

    /**
     * Calculates the pixel height of a multi-line label based on line splits.
     */
    public static int calculateHeaderHeight(String label, int padding) {
        int lineCount = label.split("<br>").length;
        return lineCount * lineHeight + padding;
    }
}