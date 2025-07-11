package com.GLSPPlantUML.factory.SequenceParts;

import com.GLSPPlantUML.builders.LifeEventBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;
import java.util.Map;

public class SequenceLifeEventFactory {
    private final SequenceModel model;

    private final List<Double> lifeEventYPos;
    private final Map<String, Double> centre;
    private final List<GModelElement> elements;
    private final List<Double> messagesYPos;

    private LifeEventBuild leBuild;

    public SequenceLifeEventFactory(SequenceModel model, List<Double> lifeEventYPos,
                                    Map<String, Double> centre, List<GModelElement> elements, List<Double> messagesYPos) {
        this.model = model;
        this.lifeEventYPos = lifeEventYPos;
        this.centre = centre;
        this.elements = elements;
        this.messagesYPos = messagesYPos;
    }

    public void createSequenceLifeEvents() {
        this.leBuild = new LifeEventBuild();

        for (SequenceNode node : model.participants) {
            for (SequenceLifeEvent le : node.getLifeEvents()) {
                double center = centre.get(node.getId()) - 3;
                double shift = 4 * le.getLevel();
                double y1 = lifeEventYPos.get(le.getStartMessage());
                double y2 = lifeEventYPos.get(le.getEndMessage());

                double x = center + shift;

                elements.add(leBuild.buildLifeEvent(node, le, x, y1, y2));
            }

            createDestroyCross(node, node.getDestroyIndex());
        }
    }

    private void createDestroyCross(SequenceNode node, int destroyIndex) {
        if (destroyIndex == -1) return;

        elements.add(leBuild.buildDestroyCross(node, centre.get(node.getId()),
                                                messagesYPos.get(destroyIndex), destroyIndex));
    }
}
