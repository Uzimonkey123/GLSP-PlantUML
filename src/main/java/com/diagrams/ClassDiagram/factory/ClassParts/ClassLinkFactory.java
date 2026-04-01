/*
 * File: ClassLinkFactory.java
 * Author: Norman Babiak
 * Description: Creates GModel edge elements and positions labels for class diagram links.
 * Date: 31.3.2026
 */

package com.diagrams.ClassDiagram.factory.ClassParts;

import com.GLSPPlantUML.utils.WidthCalculator;
import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.utils.GeometryUtils.*;
import com.diagrams.ClassDiagram.utils.LinkGeometry;
import com.diagrams.ClassDiagram.utils.LinkGeometry.EdgeGeometry;
import com.diagrams.ClassDiagram.utils.NoteCalculator;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.GEdge;
import org.eclipse.glsp.graph.GModelElement;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final Map<String, List<ClassLink>> parallelGroups = new HashMap<>();
    private final Map<String, List<ClassLink>> selfLoopGroups = new HashMap<>();


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
        buildParallelGroups();

        for (ClassLink link : model.links) {
            if (!link.getType().equals("INVISIBLE")) {
                GModelElement element = linkBuild.buildLink(link);
                int[] pInfo = getParallelInfo(link);

                // Attach parallel/self-loop indices so the frontend knows how to offset overlapping edges
                if (pInfo != null && element instanceof GEdge gEdge) {
                    gEdge.getArgs().put("parallelIndex", pInfo[0]);
                    gEdge.getArgs().put("parallelTotal", pInfo[1]);
                }

                int[] selfLoopInfo = getSelfLoopInfo(link);
                if (selfLoopInfo != null && element instanceof GEdge gEdge) {
                    gEdge.getArgs().put("selfLoopIndex", selfLoopInfo[0]);
                    gEdge.getArgs().put("selfLoopTotal", selfLoopInfo[1]);
                }

                elements.add(element);
            }
        }

        createTipLinks();
        createQuantifierLabels();
        createQualifierLabels();
        createMessageLabels();
        createLinkNotes();
    }

    /**
     * Groups links by entity pair or by single entity so each edge can be assigned an index for offset positioning.
     */
    private void buildParallelGroups() {
        parallelGroups.clear();
        selfLoopGroups.clear();

        for (ClassLink link : model.links) {
            if (link.getType().equals("INVISIBLE")) continue;
            if (link.getEntity1() == null || link.getEntity2() == null) continue;

            // Self-loops go into their own group
            if (link.getEntity1().getId().equals(link.getEntity2().getId())) {
                String entityId = link.getEntity1().getId();
                selfLoopGroups.computeIfAbsent(entityId, k -> new ArrayList<>()).add(link);

                continue;
            }

            // Member links are dealt with separately
            boolean hasMembers = (link.getSourceMember() != null && !link.getSourceMember().isEmpty())
                    || (link.getTargetMember() != null && !link.getTargetMember().isEmpty());

            if (hasMembers) continue;

            String key = canonicalPairKey(link.getEntity1().getId(), link.getEntity2().getId());
            parallelGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(link);
        }
    }

    private String canonicalPairKey(String id1, String id2) {
        return id1.compareTo(id2) <= 0 ? id1 + "::" + id2 : id2 + "::" + id1;
    }

    private int[] getParallelInfo(ClassLink link) {
        boolean hasMembers = (link.getSourceMember() != null && !link.getSourceMember().isEmpty())
                || (link.getTargetMember() != null && !link.getTargetMember().isEmpty());

        if (hasMembers) return null;
        if (link.getEntity1() == null || link.getEntity2() == null) return null;
        if (link.getEntity1().getId().equals(link.getEntity2().getId())) return null;

        String key = canonicalPairKey(link.getEntity1().getId(), link.getEntity2().getId());
        List<ClassLink> group = parallelGroups.get(key);

        // Single link no need for offset
        if (group == null || group.size() <= 1) return null;

        int index = group.indexOf(link);
        return new int[]{ index, group.size() };
    }

    private int[] getSelfLoopInfo(ClassLink link) {
        if (link.getEntity1() == null || link.getEntity2() == null) return null;
        if (!link.getEntity1().getId().equals(link.getEntity2().getId())) return null;

        String entityId = link.getEntity1().getId();
        List<ClassLink> group = selfLoopGroups.get(entityId);

        if (group == null) return null;

        int index = group.indexOf(link);
        return new int[]{ index, group.size() };
    }

    private void createTipLinks() {
        for (ClassEntityFactory.TipInfo tipInfo : entityFactory.tipInfoList) {
            elements.add(linkBuild.buildTipLinks(tipInfo));
        }
    }

    /**
     * Places qualifier label boxes on the entity boundary closest to the opposite entity, then offsets outward
     * from the nearest edge.
     */
    private void createQuantifierLabels() {
        for (ClassLink link : model.links) {
            if (isEmpty(link.getQuantifier1().getLabel()) && isEmpty(link.getQuantifier2().getLabel())) {
                continue;
            }

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            if (!isEmpty(link.getQuantifier1().getLabel())) {
                Point pos;
                if (!link.getQuantifier1().isModified()) {
                    double headSize = getHeadSize(link.getDecorator2());
                    pos = geometry.getQuantifierPosition(true, headSize);

                // If user moved label, keep it where it is, no new calculation
                } else {
                    pos = new Point(link.getQuantifier1().getX(), link.getQuantifier1().getY());
                }

                link.getQuantifier1().setX(pos.x());
                link.getQuantifier1().setY(pos.y());

                model.labels.add(link.getQuantifier1());
                elements.add(linkBuild.buildLinkQuantifier(link.getQuantifier1()));
            }

            if (!isEmpty(link.getQuantifier2().getLabel())) {
                Point pos;
                if (!link.getQuantifier2().isModified()) {
                    double headSize = getHeadSize(link.getDecorator1());
                    pos = geometry.getQuantifierPosition(false, headSize);

                // If user moved label, keep it where it is, no new calculation
                } else {
                    pos = new Point(link.getQuantifier2().getX(), link.getQuantifier2().getY());
                }

                link.getQuantifier2().setX(pos.x());
                link.getQuantifier2().setY(pos.y());

                model.labels.add(link.getQuantifier2());
                elements.add(linkBuild.buildLinkQuantifier(link.getQuantifier2()));
            }
        }
    }

    private void createQualifierLabels() {
        for (ClassLink link : model.links) {
            boolean hasSrc = !isEmpty(link.getSourceQualifier());
            boolean hasTgt = !isEmpty(link.getTargetQualifier());
            if (!hasSrc && !hasTgt) continue;

            ClassLayout.Size srcSize = dimensions.get(link.getEntity1().getId());
            ClassLayout.Size tgtSize = dimensions.get(link.getEntity2().getId());
            if (srcSize == null || tgtSize == null) continue;

            double src1CenterX = link.getEntity1().getX() + srcSize.width / 2;
            double src1CenterY = link.getEntity1().getY() + srcSize.height / 2;
            double src2CenterX = link.getEntity2().getX() + tgtSize.width / 2;
            double src2CenterY = link.getEntity2().getY() + tgtSize.height / 2;

            if (hasSrc) {
                // Find where the line exits source
                Point anchor = entityBoundaryAnchor(
                        link.getEntity1().getX(), link.getEntity1().getY(), srcSize.width, srcSize.height,
                        src2CenterX, src2CenterY);

                Point center = qualifierBoxCenter(
                        link.getSourceQualifier(), anchor,
                        link.getEntity1().getX(), link.getEntity1().getY(), srcSize.width, srcSize.height);

                ClassLabel label = link.getSourceQualifierLabel();
                label.setX(center.x());
                label.setY(center.y());

                model.labels.add(label);
                elements.add(linkBuild.buildLinkLabel(label));
            }

            if (hasTgt) {
                // same search but for target side
                Point anchor = entityBoundaryAnchor(
                        link.getEntity2().getX(), link.getEntity2().getY(), tgtSize.width, tgtSize.height,
                        src1CenterX, src1CenterY);

                Point center = qualifierBoxCenter(
                        link.getTargetQualifier(), anchor,
                        link.getEntity2().getX(), link.getEntity2().getY(), tgtSize.width, tgtSize.height);

                ClassLabel label = link.getTargetQualifierLabel();
                label.setX(center.x());
                label.setY(center.y());
                model.labels.add(label);
                elements.add(linkBuild.buildLinkLabel(label));
            }
        }
    }

    private static Point entityBoundaryAnchor(double entityX, double entityY, double entityW, double entityH,
                                              double refX, double refY) {
        double centerX = entityX + entityW / 2;
        double centerY = entityY + entityH / 2;
        double dx = refX - centerX;
        double dy = refY - centerY;

        // Ref is at the center, default is right edge
        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
            return new Point(entityX + entityW, centerY);
        }

        double halfW = entityW / 2;
        double halfH = entityH / 2;
        double t;

        // Pick the axis where the ray hits the boundary first, if the horizontal displacement is larger,
        // the ray exits through the left/right edge, otherwise through top/bottom
        if (Math.abs(dx) / halfW > Math.abs(dy) / halfH) {
            t = (dx > 0 ? halfW : -halfW) / dx;

        } else {
            t = (dy > 0 ? halfH : -halfH) / dy;
        }

        return new Point(centerX + t * dx, centerY + t * dy);
    }

    /**
     * Centers the qualifier box on the entity edge nearest to the anchor
     */
    private static Point qualifierBoxCenter(String text, Point anchor,
                                            double entityX, double entityY,
                                            double entityW, double entityH) {
        final double padding = 4.0;
        final double lineHeight = 14.0;
        final double charWidth = 6.5;
        final double gap = 0.0;

        double boxW = text.length() * charWidth + padding * 2;
        double boxH = lineHeight + padding * 2;

        // Find the closest anchor to edge entity
        double distToLeft = Math.abs(anchor.x() - entityX);
        double distToRight = Math.abs(anchor.x() - (entityX + entityW));
        double distToTop = Math.abs(anchor.y() - entityY);
        double distToBottom = Math.abs(anchor.y() - (entityY + entityH));

        double minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToTop, distToBottom));

        // Place box on the outside of the nearest edge
        double boxX, boxY;
        if (minDist == distToRight) {
            boxX = anchor.x() + gap;
            boxY = anchor.y() - boxH / 2;

        } else if (minDist == distToLeft) {
            boxX = anchor.x() - boxW - gap;
            boxY = anchor.y() - boxH / 2;

        } else if (minDist == distToBottom) {
            boxX = anchor.x() - boxW / 2;
            boxY = anchor.y() + gap;

        } else {
            boxX = anchor.x() - boxW / 2;
            boxY = anchor.y() - boxH - gap;
        }

        return new Point(boxX + boxW / 2, boxY + boxH / 2);
    }

    private void createMessageLabels() {
        for (ClassLink link : model.links) {
            if (isEmpty(link.getMessage().getLabel())) continue;

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            Point labelPos;

            if (!link.getMessage().isModified()) {
                labelPos = geometry.getLabelPosition(link.getMessage().getLabel().length());

            // Manually moved label, no recalculation
            } else {
                labelPos = new Point(link.getMessage().getX(), link.getMessage().getY());
            }

            // Push label accordingly if note on link is present
            if (link.hasNoteOnLink()) {
                Dimensions noteDim = noteCalculator.calculateNoteDimensions(link.getNoteOnLink().getName());
                double linkXAtLabelY = geometry.getLinkXAtY(labelPos.y());
                labelPos = noteCalculator.adjustLabelForNote(
                        labelPos, link.getMessage().getLabel(), noteDim, link.getNotePosition(), linkXAtLabelY);
            }

            link.getMessage().setX(labelPos.x());
            link.getMessage().setY(labelPos.y());

            model.labels.add(link.getMessage());
            elements.add(linkBuild.buildLinkLabel(link.getMessage()));
        }
    }

    private void createLinkNotes() {
        for (ClassLink link : model.links) {
            if (!link.hasNoteOnLink()) continue;

            EdgeGeometry geometry = createEdgeGeometry(link);
            if (geometry == null) continue;

            Dimensions noteDim = noteCalculator.calculateNoteDimensions(link.getNoteOnLink().getName());
            Point labelPos = geometry.getLabelPosition(
                    link.getMessage() != null ? link.getMessage().getLabel().length() : 0);

            // Prevent crossing line for label
            double linkXAtLabelY = geometry.getLinkXAtY(labelPos.y());
            Point notePos = noteCalculator.calculateNotePosition(
                    labelPos, link.getMessage().getLabel(), noteDim,
                    link.getNotePosition(), linkXAtLabelY
            );

            String noteId = "note-" + link.getLinkId();

            ClassEntity noteEntity = model.notes.stream()
                    .filter(n -> n.getId().equals(noteId))
                    .findFirst()
                    .orElse(null);

            if (noteEntity == null) continue;
            if (noteEntity.getX() == 0 && noteEntity.getY() == 0) {
                noteEntity.setX(notePos.x());
                noteEntity.setY(notePos.y());
            }

            double entityWidth = WidthCalculator.calculateWidth(noteEntity.getName(), 20) + 20;
            double entityHeight = calculateNoteHeight(noteEntity.getName());

            noteEntity.setBackground(noteEntity.getBackground());

            elements.add(entityBuild.buildNoteEntity(noteEntity, entityWidth, entityHeight));
        }
    }

    /**
     * Selects the correct EdgeGeometry implementation based on link type
     */
    private EdgeGeometry createEdgeGeometry(ClassLink link) {
        if (link.getEntity1() == null || link.getEntity2() == null) return null;

        ClassLayout.Size srcSize = dimensions.get(link.getEntity1().getId());
        if (srcSize == null) return null;

        if (link.getEntity1().getId().equals(link.getEntity2().getId())) {
            int[] selfLoopInfo = getSelfLoopInfo(link);
            int index = selfLoopInfo != null ? selfLoopInfo[0] : 0;
            int total = selfLoopInfo != null ? selfLoopInfo[1] : 1;
            return linkGeometry.createSelfLoop(link, srcSize, index, total);
        }

        ClassLayout.Size tgtSize = dimensions.get(link.getEntity2().getId());
        if (tgtSize == null) return null;

        // Multiple links between same pair offset as parallel curve
        int[] pInfo = getParallelInfo(link);
        if (pInfo != null) {
            int index = pInfo[0];
            int totalCount = pInfo[1];
            boolean flipCurve = (index - (totalCount - 1) / 2.0) > 0;
            return linkGeometry.createParallel(link, srcSize, tgtSize, flipCurve, index, totalCount);
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

    private double calculateNoteHeight(String text) {
        int lineHeight = 14;
        int padding = 15;
        int lines = text.split("<br>").length;
        return lines * lineHeight + padding * 2;
    }
}