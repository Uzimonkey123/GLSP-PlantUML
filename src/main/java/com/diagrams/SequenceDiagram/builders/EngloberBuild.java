/*
 * File: EngloberBuild.java
 * Author: Norman Babiak
 * Description: GModelElement builder for englober boxes
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.builders;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceEnglober;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class EngloberBuild {
    public GModelElement buildEngloberBox(SequenceEnglober box, double x1, double x2, double y1,
                                          double y2, double labelOffset, double highNode) {
        double padding = 10;

        GLabelBuilder label = new GLabelBuilder("label:html")
                            .id("englober-label-" + box.getId())
                            .position(0, 0)
                            .text(box.getLabel());

        return new GNodeBuilder("participant-englober")
                .id(box.getId())
                .position(x1 - padding, y1 + box.getLevel() * (3 * padding) - highNode)
                .size(x2 - x1 + 2 * padding, y2 - box.getLevel() * (3 * padding) + labelOffset + 2 * highNode)
                .addArgument("color", box.getColor())
                .addCssClass("non-interactive")
                .add(label.build())
                .build();
    }
}
