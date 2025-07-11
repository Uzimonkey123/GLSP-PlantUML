package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceEnglober;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

public class EngloberBuild {
    public GModelElement buildEngloberBox(SequenceEnglober box, double x1, double x2, double y1,
                                          double y2, double labelOffset) {
        double padding = 10;

        GLabelBuilder label = new GLabelBuilder("label:html")
                            .id("englober-label-" + box.getId())
                            .position(0, 0)
                            .text(box.getLabel());

        return new GNodeBuilder("participant-englober")
                .id(box.getId())
                .position(x1 - padding, y1 + box.getLevel() * (3 * padding))
                .size(x2 - x1 + 2 * padding, y2 - box.getLevel() * (3 * padding) + labelOffset)
                .addArgument("color", box.getColor())
                .addCssClass("non-interactive")
                .add(label.build())
                .build();
    }
}
