/*
 * File: LifeEventBuild.java
 * Author: Norman Babiak
 * Description: GModelElement builder for life events and destroy crosses
 * Date: 6.5.2026
 */

package com.diagrams.SequenceDiagram.builders;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceLifeEvent;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class LifeEventBuild {
    /**
     * Builds a GNode for the life event bar that is placed over the lifeline of the given object
     */
    public GModelElement buildLifeEvent(SequenceNode node, SequenceLifeEvent le, double x, double y1, double y2) {
        return new GNodeBuilder("lifeEvent")
                .id("act-" + node.getId() + "-" + le.getStartMessage())
                .position(x, y1)
                .size(6, y2 - y1)
                .addArgument("background", le.getBackground())
                .build();
    }

    /**
     * Builds a GNode for the destroy keyword, placing a cross over the lifeline of the given object
     */
    public GModelElement buildDestroyCross(SequenceNode node, double x, double y, int destroyIndex) {
        return new GNodeBuilder("destroy")
                .id("dest-" + node.getId() + "-" + destroyIndex)
                .position(x, y)
                .size(1, 1)
                .build();
    }
}
