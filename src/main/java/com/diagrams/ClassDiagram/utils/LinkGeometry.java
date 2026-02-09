package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;

public class LinkGeometry {

    private final double labelPerpendicularOffset = 15.0;

    private final CurvedEdgeCalculator curvedCalculator = new CurvedEdgeCalculator();

    public interface EdgeGeometry {
        GeometryUtils.Point getLabelPosition(int messageLengthChars);
        GeometryUtils.Point getQuantifierPosition(boolean isSource, double headSize);
        GeometryUtils.Point getMidpoint();
        GeometryUtils.Vector getDirection();
        double getLinkXAtY(double y);
    }

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

            if (Math.abs(direction.dy) > Math.abs(direction.dx)) {
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
            return new GeometryUtils.Point(
                    (srcCenter.x() + tgtCenter.x()) / 2,
                    (srcCenter.y() + tgtCenter.y()) / 2
            );
        }

        @Override
        public GeometryUtils.Vector getDirection() {
            return direction;
        }

        @Override
        public double getLinkXAtY(double y) {
            double dy = tgtCenter.y() - srcCenter.y();

            if (Math.abs(dy) < 0.01) {
                return srcCenter.x();
            }

            double t = (y - srcCenter.y()) / dy;
            return srcCenter.x() + (tgtCenter.x() - srcCenter.x()) * t;
        }
    }

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

    public EdgeGeometry create(ClassLink link, ClassLayout.Size srcSize, ClassLayout.Size tgtSize) {
        boolean hasMembers = (link.getSourceMember() != null && !link.getSourceMember().isEmpty())
                || (link.getTargetMember() != null && !link.getTargetMember().isEmpty());

        if (hasMembers) {
            return new CurvedEdge(link, srcSize, tgtSize);

        } else {
            return new StraightEdge(link.getEntity1(), link.getEntity2(), srcSize, tgtSize);
        }
    }
}