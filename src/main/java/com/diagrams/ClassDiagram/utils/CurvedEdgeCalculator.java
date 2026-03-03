package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurvedEdgeCalculator {

    private final Map<String, Boolean> curveDirectionMap = new HashMap<>();

    public record CurveData(GeometryUtils.Point midPoint, double midTangentX, double midTangentY) {
        public GeometryUtils.Vector tangent() {
            return new GeometryUtils.Vector(midTangentX, midTangentY);
        }

        public GeometryUtils.Vector perpendicular() {
            return new GeometryUtils.Vector(-midTangentY, midTangentX);
        }
    }

    public double getMemberYOffset(ClassLayout.Size size, boolean hasStereotype,
                                   List<EntityMethod> fields, List<EntityMethod> methods,
                                   String memberName) {
        if (memberName == null || memberName.trim().isEmpty()) {
            return size.height / 2;
        }

        double headerWithStereotype = 44;
        double headerWithoutStereotype = 30;
        double headerH = hasStereotype ? headerWithStereotype : headerWithoutStereotype;

        int fieldIndex = 0;
        double lineHeight = 14;
        double padding = 5;
        for (EntityMethod field : fields) {
            String clean = cleanMemberName(field.getMethodName());

            if (matchesMember(clean, memberName)) {
                return headerH + padding + fieldIndex * lineHeight + lineHeight / 2;
            }
            fieldIndex++;
        }

        double emptyFieldHeight = 10;
        double fieldH = fields.isEmpty() ? emptyFieldHeight : fields.size() * lineHeight + padding * 2;

        int methodIndex = 0;
        for (EntityMethod method : methods) {
            String clean = cleanMemberName(method.getMethodName());

            if (matchesMethod(clean, memberName)) {
                return headerH + fieldH + padding + methodIndex * lineHeight + lineHeight / 2;
            }
            methodIndex++;
        }

        return size.height / 2;
    }

    public GeometryUtils.Point calculateMemberAnchorPoint(double entityX, double entityY, ClassLayout.Size size,
                                                          boolean hasStereotype, List<EntityMethod> fields,
                                                          List<EntityMethod> methods, String member,
                                                          boolean curveToRight) {
        double x = curveToRight ? entityX + size.width : entityX;
        double y = entityY + getMemberYOffset(size, hasStereotype, fields, methods, member);

        return new GeometryUtils.Point(x, y);
    }

    public boolean determineCurveDirection(ClassEntity source, ClassEntity target,
                                           String sourceMember, String targetMember) {
        String sourceId = source.getId();
        String targetId = target.getId();

        String id1 = sourceId.compareTo(targetId) < 0 ? sourceId : targetId;
        String id2 = sourceId.compareTo(targetId) < 0 ? targetId : sourceId;
        String pairKey = id1 + "-" + id2;

        int hash = 0;
        for (char c : pairKey.toCharArray()) {
            hash += c;
        }
        boolean baseDirection = hash % 2 == 0;

        String linkKey = source.getId() + "-" + target.getId() + "-" +
                (sourceMember != null ? sourceMember : "") + "-" +
                (targetMember != null ? targetMember : "");

        if (!curveDirectionMap.containsKey(linkKey)) {
            long existingLinksCount = curveDirectionMap.keySet().stream()
                    .filter(key -> key.startsWith(pairKey))
                    .count();

            boolean direction = baseDirection == (existingLinksCount % 2 == 0);

            curveDirectionMap.put(linkKey, direction);
        }

        return curveDirectionMap.get(linkKey);
    }

    public boolean shouldCurveToRight(double sourceY, double targetY,
                                      ClassLayout.Size sourceSize, ClassLayout.Size targetSize,
                                      boolean flipCurve) {
        double sourceCenterY = sourceY + sourceSize.height / 2;
        double targetCenterY = targetY + targetSize.height / 2;

        double perpX = -(targetCenterY - sourceCenterY);

        if (flipCurve) {
            perpX = -perpX;
        }

        return perpX > 0;
    }

    public CurveData calculateCurve(GeometryUtils.Point start, GeometryUtils.Point end, boolean flipDirection) {
        GeometryUtils.Vector dir = new GeometryUtils.Vector(start, end);
        double distance = start.distanceTo(end);

        GeometryUtils.Vector perp = dir.perpendicular();

        if (flipDirection) {
            perp = perp.negate();
        }

        GeometryUtils.Vector perpNorm = perp.normalize();

        double arcHeightRatio = 0.3;
        double arcHeight = distance * arcHeightRatio;

        GeometryUtils.Point cp1 = new GeometryUtils.Point(
                start.x() + dir.dx() * 0.25 + perpNorm.dx() * arcHeight,
                start.y() + dir.dy() * 0.25 + perpNorm.dy() * arcHeight
        );
        GeometryUtils.Point cp2 = new GeometryUtils.Point(
                start.x() + dir.dx() * 0.75 + perpNorm.dx() * arcHeight,
                start.y() + dir.dy() * 0.75 + perpNorm.dy() * arcHeight
        );

        double t = 0.5;
        double u = 1 - t;

        GeometryUtils.Point midPoint = new GeometryUtils.Point(
                u*u*u * start.x() + 3*u*u*t * cp1.x() + 3*u*t*t * cp2.x() + t*t*t * end.x(),
                u*u*u * start.y() + 3*u*u*t * cp1.y() + 3*u*t*t * cp2.y() + t*t*t * end.y()
        );

        double midTangentX = 3 * (u*u * (cp1.x() - start.x()) + 2*u*t * (cp2.x() - cp1.x()) + t*t * (end.x() - cp2.x()));
        double midTangentY = 3 * (u*u * (cp1.y() - start.y()) + 2*u*t * (cp2.y() - cp1.y()) + t*t * (end.y() - cp2.y()));
        double midTangentLength = Math.hypot(midTangentX, midTangentY);

        return new CurveData(
                midPoint,
                midTangentX / midTangentLength,
                midTangentY / midTangentLength
        );
    }

    public GeometryUtils.Point calculateLabelPosition(CurveData curveData, double perpOffset) {
        GeometryUtils.Vector perp = curveData.tangent().perpendicular();

        return curveData.midPoint.offset(perp, perpOffset);
    }

    public GeometryUtils.Point calculateQuantifierPosition(GeometryUtils.Point anchor, GeometryUtils.Point start, GeometryUtils.Point end,
                                                           double headSize, boolean isStart) {
        GeometryUtils.Vector dir = new GeometryUtils.Vector(start, end).normalize();
        GeometryUtils.Vector perp = dir.perpendicular();

        double direction = isStart ? 1 : -1;
        double lineOffset = 15;
        double perpOffset = 15;

        return anchor
                .offset(dir, direction * (headSize + lineOffset))
                .offset(perp, perpOffset);
    }

    private String cleanMemberName(String methodName) {
        return methodName.replaceFirst("^[+\\-#~]\\s*", "").trim();
    }

    private boolean matchesMember(String clean, String memberName) {
        return clean.startsWith(memberName + " ")
                || clean.startsWith(memberName + ":")
                || clean.equals(memberName);
    }

    private boolean matchesMethod(String clean, String memberName) {
        return clean.startsWith(memberName + "(")
                || clean.startsWith(memberName + " ")
                || clean.equals(memberName);
    }
}