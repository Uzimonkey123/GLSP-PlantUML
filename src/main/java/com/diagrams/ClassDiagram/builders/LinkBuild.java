package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
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

        if (link.getNoteOnLink() != null) {
            edge.addArgument("noteText", link.getNoteOnLink());
            edge.addArgument("notePosition", link.getNotePosition());
        }
    }

    public GModelElement buildLinkLabel(ClassLabel linkLabel) {
        GLabelBuilder label = new GLabelBuilder("label:link")
                .id(linkLabel.getLabelId())
                .position(linkLabel.getX(), linkLabel.getY())
                .size(0, 0)
                .text(linkLabel.getLabel());

        return label.build();
    }

    public GModelElement buildLinkQuantifier(ClassLabel quantifierLabel) {
        GLabelBuilder label = new GLabelBuilder("label:link")
                .id(quantifierLabel.getLabelId())
                .position(quantifierLabel.getX(), quantifierLabel.getY())
                .size(0, 0)
                .text(quantifierLabel.getLabel());

        return label.build();
    }
}