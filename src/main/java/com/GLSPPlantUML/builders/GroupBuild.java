package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

import java.util.List;

public class GroupBuild {
    public GModelElement buildGroupOutline(SequenceGroup seqGroup, double x1, double x2, double y1,
                                           double y2, List<Double> separatorYPos, double labelWidth) {
        return new GEdgeBuilder("group")
                .id("group-" + seqGroup.getLevel())
                .sourceId("[")
                .targetId("]")
                .addArgument("x1", x1)
                .addArgument("x2", x2)
                .addArgument("y1", y1)
                .addArgument("y2", y2)
                .addArgument("labelWidth", labelWidth)
                .addArgument("separators", separatorYPos)
                .build();
    }

    public GModelElement buildGroupLabel(SequenceGroup group, double x1, double y1) {
        return new GLabelBuilder("label:html")
                .id("group-label-" + group.getLevel())
                .text(group.getLabel())
                .size(10, 10)
                .position(x1, y1 + 5.7)
                .addArgument("selectable", true)
                .build();
    }

    public GModelElement buildGroupComment(SequenceGroup group, double x1, double y1) {
        return new GLabelBuilder("label:html")
                .id("group-comment-" + group.getLevel())
                .text(group.getComment())
                .size(10, 10)
                .position(x1, y1 + 5.7)
                .addArgument("selectable", true)
                .build();
    }

    public GModelElement buildSeparatorLabel(String label, double x1, double y1) {
        return new GLabelBuilder("label:html")
                .id("group-separator-" + y1)
                .text(label)
                .size(10, 10)
                .position(x1, y1 + 6.5)
                .addArgument("selectable", true)
                .build();
    }
}
