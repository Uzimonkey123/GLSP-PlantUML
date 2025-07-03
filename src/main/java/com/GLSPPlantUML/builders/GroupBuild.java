package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;

import java.util.List;

public class GroupBuild {
    public GModelElement buildGroupOutline(SequenceGroup seqGroup, double x1, double x2, double y1,
                                           double y2, List<Double> separatorYPos) {
        return new GEdgeBuilder("group")
                .id("group-" + seqGroup.getLevel())
                .sourceId("[")
                .targetId("]")
                .addArgument("x1", x1)
                .addArgument("x2", x2)
                .addArgument("y1", y1)
                .addArgument("y2", y2)
                .addArgument("separators", separatorYPos)
                .build();
    }

    public GModelElement buildGroupLabel() {
        return null; // TODO
    }

    public GModelElement buildGroupComment() {
        return null; // TODO
    }
}
