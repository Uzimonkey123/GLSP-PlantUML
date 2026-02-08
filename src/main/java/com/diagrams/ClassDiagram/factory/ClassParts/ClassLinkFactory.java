package com.diagrams.ClassDiagram.factory.ClassParts;

import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;

import java.util.List;
import java.util.Map;

public class ClassLinkFactory {
    private final ClassModel model;
    private final LinkBuild linkBuild;

    private final List<GModelElement> elements;
    private final ClassEntityFactory entityFactory;
    private final EntityBuild entityBuild;

    public ClassLinkFactory(ClassModel model, List<GModelElement> elements, ClassEntityFactory entityFactory, EntityBuild entityBuild) {
        this.model = model;
        this.elements = elements;
        this.linkBuild = new LinkBuild();
        this.entityFactory = entityFactory;
        this.entityBuild = entityBuild;
    }

    public void createLinks() {
        for (ClassLink link : model.links) {
            if(link.getType().equals("INVISIBLE")) {
                continue;
            }

            elements.add(linkBuild.buildLink(link));
        }

        createTipLinks();
        createLinkNotes();
    }

    private void createTipLinks() {
        for (ClassEntityFactory.TipInfo tipInfo : entityFactory.tipInfoList) {
            GEdgeBuilder edge = new GEdgeBuilder("link:note")
                    .id("edge-" + tipInfo.tipId)
                    .sourceId(tipInfo.parentEntityId)
                    .targetId(tipInfo.tipId)
                    .addArgument("memberName", tipInfo.memberName);

            elements.add(edge.build());
        }
    }

    private void createLinkNotes() {
        Map<String, ClassLayout.Size> dimensions = entityFactory.getDimensions();

        for (ClassLink link : model.links) {
            if (link.getNoteOnLink() == null || link.getNoteOnLink().isEmpty()) {
                continue;
            }

            // Calculate note dimensions
            String noteText = link.getNoteOnLink();
            String notePosition = link.getNotePosition() != null ? link.getNotePosition() : "RIGHT";

            String[] lines = noteText.split("<br>");
            int maxLineLength = 0;
            for (String line : lines) {
                maxLineLength = Math.max(maxLineLength, line.length());
            }

            double charWidth = 6.5;
            double lineHeight = 14;
            double padding = 10;

            double noteWidth = Math.max(maxLineLength * charWidth + padding * 2, 100);
            double noteHeight = lines.length * lineHeight + padding * 2;

            // Get actual entity dimensions
            ClassLayout.Size entity1Size = dimensions.get(link.getEntity1().getId());
            ClassLayout.Size entity2Size = dimensions.get(link.getEntity2().getId());

            if (entity1Size == null || entity2Size == null) {
                continue;
            }

            // Calculate entity centers
            double entity1CenterX = link.getEntity1().getX() + entity1Size.width / 2;
            double entity1CenterY = link.getEntity1().getY() + entity1Size.height / 2;
            double entity2CenterX = link.getEntity2().getX() + entity2Size.width / 2;
            double entity2CenterY = link.getEntity2().getY() + entity2Size.height / 2;

            double midX = (entity1CenterX + entity2CenterX) / 2;
            double midY = (entity1CenterY + entity2CenterY) / 2;

            double dx = entity2CenterX - entity1CenterX;
            double dy = entity2CenterY - entity1CenterY;
            double length = Math.sqrt(dx * dx + dy * dy);

            double perpX = -dy / length;
            double perpY = dx / length;

            double perpOffset = 15;
            double labelX = midX - perpX * perpOffset;
            double labelY = midY - perpY * perpOffset;

            String message = link.getMessage() != null ? link.getMessage() : "";
            double labelWidth = message.length() * charWidth;

            // Calculate note position based on notePosition
            double noteX, noteY;
            double horizontalOffset = 20;
            double verticalOffset = 10;
            double safetyMargin = 30;

            // Calculate where the link line is at the label's Y position
            double linkXAtLabelY = midX;
            if (Math.abs(dy) > 0.01) {
                double t = (labelY - entity1CenterY) / dy;
                linkXAtLabelY = entity1CenterX + dx * t;
            }

            switch (notePosition) {
                case "TOP":
                    noteX = labelX - noteWidth / 2;
                    noteY = labelY - noteHeight - verticalOffset;

                    if (noteX < linkXAtLabelY) {
                        double shift = linkXAtLabelY - noteX + safetyMargin;
                        noteX += shift;
                    }
                    break;

                case "BOTTOM":
                    noteX = labelX - noteWidth / 2;
                    noteY = labelY + verticalOffset;

                    if (noteX < linkXAtLabelY) {
                        double shift = linkXAtLabelY - noteX + safetyMargin;
                        noteX += shift;
                    }
                    break;

                case "LEFT":
                    noteX = labelX - labelWidth / 2 - horizontalOffset - noteWidth;
                    noteY = labelY - noteHeight / 2;

                    // Ensure note doesn't cross link line
                    if (noteX < linkXAtLabelY) {
                        double shift = linkXAtLabelY - noteX + safetyMargin;
                        noteX += shift;
                    }
                    break;

                case "RIGHT":
                default:
                    noteX = labelX + labelWidth / 2 + horizontalOffset;
                    noteY = labelY - noteHeight / 2;

                    // Ensure label left edge doesn't cross link line
                    double labelLeftEdge = labelX - labelWidth / 2;
                    if (labelLeftEdge < linkXAtLabelY) {
                        double shift = linkXAtLabelY - labelLeftEdge + safetyMargin;
                        noteX += shift;
                    }
                    break;
            }

            noteY -= 15;

            // Create note entity
            String noteId = link.getLinkId() + "-note";
            ClassEntity noteEntity = new ClassEntity((int) noteX, (int) noteY, noteId, noteText, "NOTE");
            elements.add(entityBuild.buildNoteEntity(noteEntity, noteWidth, noteHeight));
        }
    }
}