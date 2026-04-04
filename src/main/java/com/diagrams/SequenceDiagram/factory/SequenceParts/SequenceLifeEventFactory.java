/*
 * File: SequenceLifeEventFactory.java
 * Author: Norman Babiak
 * Description: Factory for creating life event bars
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.factory.SequenceParts;

import com.diagrams.SequenceDiagram.builders.LifeEventBuild;
import com.diagrams.SequenceDiagram.factory.SequenceFactoryContext;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceLifeEvent;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;
import java.util.Map;

public class SequenceLifeEventFactory {
    private final SequenceFactoryContext ctx;
    private final LifeEventBuild leBuild;
    private final int levelOffset = 3;

    public SequenceLifeEventFactory(SequenceFactoryContext ctx) {
        this.ctx = ctx;
        this.leBuild = new LifeEventBuild();
    }

    public void createSequenceLifeEvents() {
        Map<String, Double> centre = ctx.getCentre();
        List<Double> lifeEventYPos = ctx.getLifeEventYPos();
        List<Double> messagesYPos = ctx.getMessagesYPos();
        List<GModelElement> elements = ctx.getElements();

        for (SequenceNode node : ctx.getModel().participants) {
            for (SequenceLifeEvent le : node.getLifeEvents()) {
                double center = centre.get(node.getId()) - levelOffset;
                double shift = 4 * le.getLevel();
                double y1 = lifeEventYPos.get(le.getStartMessage());
                double y2 = lifeEventYPos.get(le.getEndMessage());

                double x = center + shift;

                elements.add(leBuild.buildLifeEvent(node, le, x, y1, y2));
            }

            if (node.getDestroyIndex() != -1) {
                elements.add(leBuild.buildDestroyCross(node, centre.get(node.getId()),
                        messagesYPos.get(node.getDestroyIndex()), node.getDestroyIndex()));
            }
        }
    }
}
