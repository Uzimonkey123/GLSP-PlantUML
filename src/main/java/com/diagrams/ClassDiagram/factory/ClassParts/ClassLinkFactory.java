package com.diagrams.ClassDiagram.factory.ClassParts;

import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.utils.GeometryUtils.*;
import com.diagrams.ClassDiagram.utils.LinkGeometry;
import com.diagrams.ClassDiagram.utils.LinkGeometry.EdgeGeometry;
import com.diagrams.ClassDiagram.utils.NoteCalculator;
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
    private final Map<String, ClassLayout.Size> dimensions;
    private final LinkGeometry linkGeometry;
    private final NoteCalculator noteCalculator;

    public ClassLinkFactory(ClassModel model, List<GModelElement> elements,
                            ClassEntityFactory entityFactory, EntityBuild entityBuild) {
        this.model = model;
        this.elements = elements;
        this.linkBuild = new LinkBuild();
        this.entityFactory = entityFactory;
        this.entityBuild = entityBuild;
        this.dimensions = entityFactory.getDimensions();
        this.linkGeometry = new LinkGeometry();
        this.noteCalculator = new NoteCalculator();
    }

    public void createLinks() {
        for (ClassLink link : model.links) {
            if (!link.getType().equals("INVISIBLE")) {
                elements.add(linkBuild.buildLink(link));
            }
        }

        createTipLinks();
        createQuantifierLabels();
        createMessageLabels();
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

    private void createQuantifierLabels() {
        for (ClassLink link : model.links) {
            if (isEmpty(link.getQuantifier1()) && isEmpty(link.getQuantifier2())) {
                continue;
            }

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            if (!isEmpty(link.getQuantifier1())) {
                double headSize = getHeadSize(link.getDecorator2());
                Point pos = geometry.getQuantifierPosition(true, headSize);
                elements.add(linkBuild.buildLinkQuantifier(
                        link.getLinkId(), "1", link.getQuantifier1(), pos.x(), pos.y()));
            }

            if (!isEmpty(link.getQuantifier2())) {
                double headSize = getHeadSize(link.getDecorator1());
                Point pos = geometry.getQuantifierPosition(false, headSize);
                elements.add(linkBuild.buildLinkQuantifier(
                        link.getLinkId(), "2", link.getQuantifier2(), pos.x(), pos.y()));
            }
        }
    }

    private void createMessageLabels() {
        for (ClassLink link : model.links) {
            if (isEmpty(link.getMessage())) continue;

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            Point labelPos = geometry.getLabelPosition(link.getMessage().length());

            if (link.hasNoteOnLink()) {
                Dimensions noteDim = noteCalculator.calculateNoteDimensions(link.getNoteOnLink());
                double linkXAtLabelY = geometry.getLinkXAtY(labelPos.y());
                labelPos = noteCalculator.adjustLabelForNote(
                        labelPos, link.getMessage(), noteDim,
                        link.getNotePosition(), linkXAtLabelY
                );
            }

            elements.add(linkBuild.buildLinkLabel(
                    link.getLinkId(), link.getMessage(), labelPos.x(), labelPos.y()));
        }
    }

    private void createLinkNotes() {
        for (ClassLink link : model.links) {
            if (!link.hasNoteOnLink()) continue;

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            Dimensions noteDim = noteCalculator.calculateNoteDimensions(link.getNoteOnLink());
            Point labelPos = geometry.getLabelPosition(
                    link.getMessage() != null ? link.getMessage().length() : 0);

            double linkXAtLabelY = geometry.getLinkXAtY(labelPos.y());
            Point notePos = noteCalculator.calculateNotePosition(
                    labelPos, link.getMessage(), noteDim,
                    link.getNotePosition(), linkXAtLabelY
            );

            String noteId = link.getLinkId() + "-note";
            ClassEntity noteEntity = new ClassEntity(
                    (int) notePos.x(), (int) notePos.y(), noteId, link.getNoteOnLink(), "NOTE");
            noteEntity.setBackground(link.getNoteColor());
            elements.add(entityBuild.buildNoteEntity(noteEntity, noteDim.width(), noteDim.height()));
        }
    }

    private EdgeGeometry createEdgeGeometry(ClassLink link) {
        ClassLayout.Size srcSize = dimensions.get(link.getEntity1().getId());
        ClassLayout.Size tgtSize = dimensions.get(link.getEntity2().getId());

        if (srcSize == null || tgtSize == null) {
            return null;
        }

        return linkGeometry.create(link, srcSize, tgtSize);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private double getHeadSize(String decorator) {
        if (decorator == null) return 0;
        return switch (decorator) {
            case "EXTENDS", "COMPOSITION", "AGREGATION", "ARROW", "CROWFOOT" -> 8.0;
            case "SQUARE", "PLUS" -> 4.0;
            case "NOT_NAVIGABLE" -> 10.0;
            default -> 0.0;
        };
    }
}