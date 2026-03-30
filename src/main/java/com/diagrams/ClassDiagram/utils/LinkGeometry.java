/*
 * File: LinkGeometry.java
 * Author: Norman Babiak
 * Description: Strategy-based edge geometry for link label and quantifier positioning.
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;

public class LinkGeometry {

    private final double labelPerpendicularOffset = 15.0;

    private final CurvedEdgeCalculator curvedCalculator = new CurvedEdgeCalculator();

    /**
     * Interface for calculating label, quantifier, midpoint positions on different edge types
     */
    public interface EdgeGeometry {
        GeometryUtils.Point getLabelPosition(int messageLengthChars);
        GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize);
        GeometryUtils.Point getMidpoint();
        GeometryUtils.Vector getDirection();
        double getLinkXAtY(double y);
    }

    /**
     * Direct line between entities, no members specified either. Label is positioned from midpoint of the line
     */
    public class StraightEdge implements EdgeGeometry {
        private final GeometryUtils.Point srcCenter;
        private final GeometryUtils.Point tgtCenter;
        private final GeometryUtils.Vector direction;
        private final GeometryUtils.Vector perpendicular;
        private final ClassLayout.Size srcSize;
        private final ClassLayout.Size tgtSize;

        public StraightEdge(ClassEntity src, ClassEntity tgt,
                            ClassLayout.Size srcSize, ClassLayout.Size tgtSize) {
            this.srcSize = srcSize;
            this.tgtSize = tgtSize;
            this.srcCenter = new GeometryUtils.Point(
                    src.getX() + srcSize.width / 2,
                    src.getY() + srcSize.height / 2
            );
            this.tgtCenter = new GeometryUtils.Point(
                    tgt.getX() + tgtSize.width / 2,
                    tgt.getY() + tgtSize.height / 2
            );
            this.direction = new GeometryUtils.Vector(srcCenter, tgtCenter).normalize();
            this.perpendicular = direction.perpendicular();
        }

        @Override
        public GeometryUtils.Point getLabelPosition(int messageLengthChars) {
            GeometryUtils.Point mid = getMidpoint();
            double offset = labelPerpendicularOffset;

            // Mostly-vertical edges need extra offset so the label clears the line
            if (Math.abs(direction.dy()) > Math.abs(direction.dx())) {
                double verticalLabelExtraOffset = 2.5;
                offset += messageLengthChars * verticalLabelExtraOffset;
            }

            return mid.offset(perpendicular.negate(), offset);
        }

        @Override
        public GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize) {
            GeometryUtils.Point anchor = isSource ? srcCenter : tgtCenter;
            ClassLayout.Size size = isSource ? srcSize : tgtSize;

            double quantifierLineOffset = 15.0;
            double distance = (size.width / 2) + headSize + quantifierLineOffset;
            GeometryUtils.Vector dir = isSource ? direction : direction.negate();

            double quantifierPerpendicularOffset = 15.0;
            return anchor
                    .offset(dir, distance)
                    .offset(perpendicular, quantifierPerpendicularOffset);
        }

        @Override
        public GeometryUtils.Point getMidpoint() {
            return srcCenter.midpoint(tgtCenter);
        }

        @Override
        public GeometryUtils.Vector getDirection() {
            return direction;
        }

        @Override
        public double getLinkXAtY(double y) {
            GeometryUtils.Vector delta = new GeometryUtils.Vector(srcCenter, tgtCenter);

            if (Math.abs(delta.dy()) < 0.01) {
                return srcCenter.x();
            }

            double t = (y - srcCenter.y()) / delta.dy();
            return srcCenter.x() + delta.dx() * t;
        }
    }

    /**
     * Bézier curve anchored to specific members in member-to-member links. Curves alternate if between the two same entities
     * from right and left. Label again at the midpoint and pushed
     */
    public class CurvedEdge implements EdgeGeometry {
        private final GeometryUtils.Point srcAnchor;
        private final GeometryUtils.Point tgtAnchor;
        private final CurvedEdgeCalculator.CurveData curve;

        public CurvedEdge(ClassLink link, ClassLayout.Size srcSize, ClassLayout.Size tgtSize) {
            ClassEntity src = link.getEntity1();
            ClassEntity tgt = link.getEntity2();

            boolean flip = curvedCalculator.determineCurveDirection(
                    src, tgt, link.getSourceMember(), link.getTargetMember());

            boolean curveToRight = curvedCalculator.shouldCurveToRight(
                    src.getY(), tgt.getY(), srcSize, tgtSize, flip);

            this.srcAnchor = curvedCalculator.calculateMemberAnchorPoint(
                    src.getX(), src.getY(), srcSize,
                    src.getStereotypeName() != null,
                    src.getFields(), src.getMethods(),
                    link.getSourceMember(), curveToRight);

            this.tgtAnchor = curvedCalculator.calculateMemberAnchorPoint(
                    tgt.getX(), tgt.getY(), tgtSize,
                    tgt.getStereotypeName() != null,
                    tgt.getFields(), tgt.getMethods(),
                    link.getTargetMember(), curveToRight);

            this.curve = curvedCalculator.calculateCurve(srcAnchor, tgtAnchor, flip);
        }

        @Override
        public GeometryUtils.Point getLabelPosition(int messageLengthChars) {
            return curvedCalculator.calculateLabelPosition(curve, labelPerpendicularOffset);
        }

        @Override
        public GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize) {
            GeometryUtils.Point anchor = isSource ? srcAnchor : tgtAnchor;
            return curvedCalculator.calculateQuantifierPosition(
                    anchor, srcAnchor, tgtAnchor, headSize, isSource);
        }

        @Override
        public GeometryUtils.Point getMidpoint() {
            return curve.midPoint();
        }

        @Override
        public GeometryUtils.Vector getDirection() {
            return new GeometryUtils.Vector(curve.midTangentX(), curve.midTangentY());
        }

        @Override
        public double getLinkXAtY(double y) {
            return curve.midPoint().x();
        }
    }

    /**
     * Curved and offset-ed edge for multiple links between the same entities. Start and end are shifted according to
     * how many lines are between the entities, while they as well change in matter of curved/straight depending on where
     * they are
     */
    public class ParallelEdge implements EdgeGeometry {
        private final GeometryUtils.Point srcAnchor;
        private final GeometryUtils.Point tgtAnchor;
        private final CurvedEdgeCalculator.CurveData curve;
        private final int index;
        private final int totalCount;

        public ParallelEdge(ClassLink link, ClassLayout.Size srcSize, ClassLayout.Size tgtSize,
                            boolean flipCurve, int index, int totalCount) {
            this.index = index;
            this.totalCount = totalCount;

            ClassEntity src = link.getEntity1();
            ClassEntity tgt = link.getEntity2();

            double srcCx = src.getX() + srcSize.width  / 2.0;
            double srcCy = src.getY() + srcSize.height / 2.0;
            double tgtCx = tgt.getX() + tgtSize.width  / 2.0;
            double tgtCy = tgt.getY() + tgtSize.height / 2.0;

            // Unit perpendicular to the centre→centre direction
            GeometryUtils.Point srcCenter = new GeometryUtils.Point(srcCx, srcCy);
            GeometryUtils.Point tgtCenter = new GeometryUtils.Point(tgtCx, tgtCy);

            GeometryUtils.Vector dir = new GeometryUtils.Vector(srcCenter, tgtCenter).normalize();
            GeometryUtils.Vector perp = dir.perpendicular();

            double spacing = 25.0;
            double middle = (totalCount - 1) / 2.0;
            double offset = (index - middle) * spacing;

            // Offset centres
            GeometryUtils.Point srcOffsetCenter = srcCenter.offset(perp, offset);
            GeometryUtils.Point tgtOffsetCenter = tgtCenter.offset(perp, offset);

            double srcOffCx = srcOffsetCenter.x();
            double srcOffCy = srcOffsetCenter.y();
            double tgtOffCx = tgtOffsetCenter.x();
            double tgtOffCy = tgtOffsetCenter.y();

            // Intersect offset ray with entity rectangle boundary
            this.srcAnchor = entityBoundaryPoint(
                    src.getX(), srcSize.width, srcSize.height, srcOffCy, tgtOffCx, tgtOffCy);

            this.tgtAnchor = entityBoundaryPoint(
                    tgt.getX(), tgtSize.width, tgtSize.height, tgtOffCy, srcOffCx, srcOffCy);

            this.curve = curvedCalculator.calculateCurve(srcAnchor, tgtAnchor, flipCurve);
        }

        private GeometryUtils.Point entityBoundaryPoint(double entityX, double w, double h, double fromCy, double toCx,
                                                        double toCy) {
            GeometryUtils.Rectangle rect = new GeometryUtils.Rectangle(entityX, fromCy - h / 2.0, w, h);
            GeometryUtils.Point target = new GeometryUtils.Point(toCx, toCy);

            return rect.boundaryIntersection(target);
        }

        @Override
        public GeometryUtils.Point getLabelPosition(int messageLengthChars) {
            double spacing = 25.0;
            double middle = (totalCount - 1) / 2.0;
            double signedPos = (index - middle) * spacing;

            double parallelLabelPerpendicularOffset = 65.0;
            double labelOffset = parallelLabelPerpendicularOffset + Math.abs(signedPos) * 0.4;
            double sign = signedPos >= 0 ? 1.0 : -1.0;
            return curvedCalculator.calculateLabelPosition(curve, sign * labelOffset);
        }

        @Override
        public GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize) {
            GeometryUtils.Point anchor = isSource ? srcAnchor : tgtAnchor;
            GeometryUtils.Point base = curvedCalculator.calculateQuantifierPosition(
                    anchor, srcAnchor, tgtAnchor, headSize, isSource);

            double spacing = 25.0;
            double middle = (totalCount - 1) / 2.0;
            double signedPos = (index - middle) * spacing;
            double extraOffset = Math.abs(signedPos) * 0.4;
            double sign = signedPos >= 0 ? 1.0 : -1.0;

            GeometryUtils.Vector perp =
                    new GeometryUtils.Vector(curve.midTangentX(), curve.midTangentY()).perpendicular();

            return base.offset(perp, sign * extraOffset);
        }

        @Override
        public GeometryUtils.Point getMidpoint() {
            return curve.midPoint();
        }

        @Override
        public GeometryUtils.Vector getDirection() {
            return new GeometryUtils.Vector(curve.midTangentX(), curve.midTangentY());
        }

        @Override
        public double getLinkXAtY(double y) {
            return curve.midPoint().x();
        }
    }

    /**
     * Link between the same entity, made as a loop to the right
     */
    public class SelfLoopEdge implements EdgeGeometry {
        private final GeometryUtils.Point startAnchor;
        private final GeometryUtils.Point endAnchor;
        private final GeometryUtils.Point controlPoint1;
        private final double bulge;

        public SelfLoopEdge(ClassLink link, ClassLayout.Size size, int index, int totalCount) {
            ClassEntity entity = link.getEntity1();
            double x = entity.getX();
            double y = entity.getY();
            double width = size.width;
            double height = size.height;

            String message = link.getMessage() != null ? link.getMessage().getLabel() : "";
            double charWidth = 7;
            double labelPadding = 12;
            double labelWidth = !message.isEmpty()
                    ? message.length() * charWidth + labelPadding
                    : 40;

            double baseBulge = 25;
            double bulgeSpacing = labelWidth + 8;
            this.bulge = baseBulge + (totalCount - 1 - index) * bulgeSpacing;

            double baseSpread = Math.min(height * 0.15, 12);
            double spreadStep = 8;
            double spread = baseSpread + (totalCount - 1 - index) * spreadStep;

            double centerY = y + height / 2;
            double anchorY1 = centerY - spread;
            double anchorY2 = centerY + spread;
            double anchorX = x + width;

            this.startAnchor = new GeometryUtils.Point(anchorX, anchorY1);
            this.endAnchor = new GeometryUtils.Point(anchorX, anchorY2);
            this.controlPoint1 = new GeometryUtils.Point(anchorX + bulge, anchorY1);
        }

        @Override
        public GeometryUtils.Point getLabelPosition(int messageLengthChars) {
            double midY = (startAnchor.y() + endAnchor.y()) / 2;
            double labelX = startAnchor.x() + bulge - (bulge - 25) / 2;

            return new GeometryUtils.Point(labelX, midY);
        }

        @Override
        public GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize) {
            GeometryUtils.Point anchor = isSource ? startAnchor : endAnchor;
            double offsetX = Math.min(bulge * 0.4, 20);
            double offsetY = isSource ? -10 : 10;

            return new GeometryUtils.Point(anchor.x() + offsetX, anchor.y() + offsetY);
        }

        @Override
        public GeometryUtils.Point getMidpoint() {
            double midY = (startAnchor.y() + endAnchor.y()) / 2;

            return new GeometryUtils.Point(controlPoint1.x(), midY);
        }

        @Override
        public GeometryUtils.Vector getDirection() {
            return new GeometryUtils.Vector(0, 1);
        }

        @Override
        public double getLinkXAtY(double y) {
            return controlPoint1.x();
        }
    }

    public EdgeGeometry createSelfLoop(ClassLink link, ClassLayout.Size size, int index, int totalCount) {
        return new SelfLoopEdge(link, size, index, totalCount);
    }

    public EdgeGeometry create(ClassLink link, ClassLayout.Size srcSize, ClassLayout.Size tgtSize) {
        boolean hasMembers = (link.getSourceMember() != null && !link.getSourceMember().isEmpty())
                || (link.getTargetMember() != null && !link.getTargetMember().isEmpty());

        if (hasMembers) {
            return new CurvedEdge(link, srcSize, tgtSize);

        } else {
            return new StraightEdge(link.getEntity1(), link.getEntity2(), srcSize, tgtSize);
        }
    }

    public EdgeGeometry createParallel(ClassLink link, ClassLayout.Size srcSize, ClassLayout.Size tgtSize,
                                       boolean flipCurve, int index, int totalCount) {
        return new ParallelEdge(link, srcSize, tgtSize, flipCurve, index, totalCount);
    }
}