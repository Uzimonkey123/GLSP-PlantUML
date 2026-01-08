package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;

public class LinkBuild {
    public GModelElement buildLink(ClassLink link) {
        GEdgeBuilder edge = new GEdgeBuilder("link")
                .id(link.getLinkId())
                .sourceId(link.getEntity1().getId())
                .targetId(link.getEntity2().getId());

        return edge.build();
    }
}
