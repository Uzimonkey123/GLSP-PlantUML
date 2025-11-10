package com.diagrams.SequenceDiagram.builders;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceLifeEvent;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class LifeEventBuild {
    public GModelElement buildLifeEvent(SequenceNode node, SequenceLifeEvent le, double x, double y1, double y2) {
        return new GNodeBuilder("lifeEvent")
                .id("act-" + node.getId() + "-" + le.getStartMessage())
                .position(x, y1)
                .size(6, y2 - y1)
                .addArgument("background", le.getBackground())
                .build();
    }

    public GModelElement buildDestroyCross(SequenceNode node, double x, double y, int destroyIndex) {
        return new GNodeBuilder("destroy")
                .id("dest-" + node.getId() + "-" + destroyIndex)
                .position(x, y)
                .size(1, 1)
                .build();
    }
}
