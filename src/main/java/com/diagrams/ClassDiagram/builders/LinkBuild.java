package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.*;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

public class LinkBuild {

    public GModelElement buildLink(ClassLink link) {
        String edgeType = link.isNoteLink() ? "link:note" : "link";

        GEdgeBuilder edge = new GEdgeBuilder(edgeType)
                .id(link.getLinkId())
                .sourceId(link.getEntity1().getId())
                .targetId(link.getEntity2().getId());

        addLinkArguments(link, edge);
        addLinkLabels(link, edge, link.getEntity1(), link.getEntity2());
        return edge.build();
    }

    private void addLinkArguments(ClassLink link, GEdgeBuilder edge) {
        edge.addArgument("headStart", link.getDecorator2());
        edge.addArgument("headEnd", link.getDecorator1());
        edge.addArgument("style", link.getType());
        edge.addArgument("color", link.getColor());
        edge.addArgument("thickness", link.getThickness());

        if (link.getSourceMember() != null && !link.getSourceMember().isEmpty()) {
            edge.addArgument("sourceMember", link.getSourceMember());
        }

        if (link.getTargetMember() != null && !link.getTargetMember().isEmpty()) {
            edge.addArgument("targetMember", link.getTargetMember());
        }

        if (link.getSourceQualifier() != null && !link.getSourceQualifier().isEmpty()) {
            edge.addArgument("sourceQualifier", link.getSourceQualifier());
        }
        if (link.getTargetQualifier() != null && !link.getTargetQualifier().isEmpty()) {
            edge.addArgument("targetQualifier", link.getTargetQualifier());
        }
    }

    private void addLinkLabels(ClassLink link, GEdgeBuilder edge, ClassEntity sourceNode, ClassEntity targetNode) {
        GLabel quantifier1 = new GLabelBuilder("label:invis")
                .id("quant1-" + link.getLinkId())
                .text(link.getQuantifier1())
                .build();

        edge.add(quantifier1);

        GLabel quantifier2 = new GLabelBuilder("label:invis")
                .id("quant2-" + link.getLinkId())
                .text(link.getQuantifier2())
                .build();

        edge.add(quantifier2);

        GLabel label = new GLabelBuilder("label:invis")
                .id("label-" + link.getLinkId())
                .text(link.getMessage())
                .build();

        edge.add(label);
    }
}