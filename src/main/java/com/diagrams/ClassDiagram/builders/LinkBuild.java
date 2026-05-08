/*
 * File: LinkBuild.java
 * Author: Norman Babiak
 * Description: Builder for links between entities as GEdges
 * Date: 4.5.2026
 */

package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.factory.ClassParts.ClassEntityFactory;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.*;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

public class LinkBuild {

    /**
     * Builds a GEdge for a class link, using "link:note" type for note links and "link" for all others
     *
     * @param link the parsed class link
     * @return the built GEdge element
     */
    public GModelElement buildLink(ClassLink link) {
        String edgeType = link.isNoteLink() ? "link:note" : "link";

        GEdgeBuilder edge = new GEdgeBuilder(edgeType)
                .id(link.getLinkId())
                .sourceId(link.getEntity1().getId())
                .targetId(link.getEntity2().getId());

        addLinkArguments(link, edge);
        return edge.build();
    }

    /**
     * Adds all visual and semantic arguments to an edge builder
     *
     * @param link the source link with all parsed properties
     * @param edge the edge builder to add arguments to
     */
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

    /**
     * Builds a movable label for the link message text, placed at the label's stored position
     *
     * @param linkLabel the parsed label with position and text
     * @return the built GLabel element
     */
    public GModelElement buildLinkLabel(ClassLabel linkLabel) {
        GLabelBuilder label = new GLabelBuilder("label:link")
                .id(linkLabel.getLabelId())
                .position(linkLabel.getX(), linkLabel.getY())
                .size(0, 0)
                .text(linkLabel.getLabel());

        return label.build();
    }

    /**
     * Builds a movable label for a quantifier on a link endpoint
     *
     * @param quantifierLabel the parsed quantifier label with position and text
     * @return the built GLabel element
     */
    public GModelElement buildLinkQuantifier(ClassLabel quantifierLabel) {
        GLabelBuilder label = new GLabelBuilder("label:link")
                .id(quantifierLabel.getLabelId())
                .position(quantifierLabel.getX(), quantifierLabel.getY())
                .size(0, 0)
                .text(quantifierLabel.getLabel());

        return label.build();
    }

    /**
     * Builds a note-style edge connecting a tip to its parent entity,
     *
     * @param tipInfo the tip info containing parent entity ID, tip ID, and member name
     * @return the built GEdge element
     */
    public GModelElement buildTipLinks(ClassEntityFactory.TipInfo tipInfo) {
        GEdgeBuilder edge = new GEdgeBuilder("link:note")
                .id("edge-" + tipInfo.tipId)
                .sourceId(tipInfo.parentEntityId)
                .targetId(tipInfo.tipId)
                .addArgument("memberName", tipInfo.memberName);

        return edge.build();
    }
}
