package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class LifeEventBuild {
    public GModelElement buildLifeEvent(SequenceNode node, SequenceLifeEvent le, double x, double y1, double y2) {
        return new GNodeBuilder("lifeEvent")
                .id("act-" + node.getName() + "-" + le.getStartMessage())
                .position(x, y1)
                .size(6, y2 - y1)
                .addArgument("background", le.getBackground())
                .build();
    }

    public GModelElement buildDestroyCross(SequenceNode node, double x, double y, int destroyIndex) {
        return new GNodeBuilder("destroy")
                .id("dest-" + node.getName() + "-" + destroyIndex)
                .position(x, y)
                .size(1, 1)
                .build();
    }
}
