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

        double dy = targetCenterY - sourceCenterY;
        double perpX = -dy;

        if (flipCurve) {
            perpX = -perpX;
        }

        return perpX > 0;
    }

    public CurveData calculateCurve(GeometryUtils.Point start, GeometryUtils.Point end, boolean flipDirection) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double distance = Math.sqrt(dx * dx + dy * dy);

        double perpX = -dy;
        double perpY = dx;

        if (flipDirection) {
            perpX = -perpX;
            perpY = -perpY;
        }

        double perpLength = Math.sqrt(perpX * perpX + perpY * perpY);
        double perpNormX = perpX / perpLength;
        double perpNormY = perpY / perpLength;

        double arcHeightRatio = 0.3;
        double arcHeight = distance * arcHeightRatio;

        GeometryUtils.Point cp1 = new GeometryUtils.Point(
                start.x() + dx * 0.25 + perpNormX * arcHeight,
                start.y() + dy * 0.25 + perpNormY * arcHeight
        );

        GeometryUtils.Point cp2 = new GeometryUtils.Point(
                start.x() + dx * 0.75 + perpNormX * arcHeight,
                start.y() + dy * 0.75 + perpNormY * arcHeight
        );

        double t = 0.5;
        double u = 1 - t;

        GeometryUtils.Point midPoint = new GeometryUtils.Point(
                u*u*u * start.x() + 3*u*u*t * cp1.x() + 3*u*t*t * cp2.x() + t*t*t * end.x(),
                u*u*u * start.y() + 3*u*u*t * cp1.y() + 3*u*t*t * cp2.y() + t*t*t * end.y()
        );

        double midTangentX = 3 * (u*u * (cp1.x() - start.x()) + 2*u*t * (cp2.x() - cp1.x()) + t*t * (end.x() - cp2.x()));
        double midTangentY = 3 * (u*u * (cp1.y() - start.y()) + 2*u*t * (cp2.y() - cp1.y()) + t*t * (end.y() - cp2.y()));
        double midTangentLength = Math.sqrt(midTangentX * midTangentX + midTangentY * midTangentY);

        return new CurveData(
                midPoint,
                midTangentX / midTangentLength,
                midTangentY / midTangentLength
        );
    }

    public GeometryUtils.Point calculateLabelPosition(CurveData curveData, double perpOffset) {
        double perpX = -curveData.midTangentY;
        double perpY = curveData.midTangentX;

        return new GeometryUtils.Point(
                curveData.midPoint.x() + perpX * perpOffset,
                curveData.midPoint.y() + perpY * perpOffset
        );
    }

    public GeometryUtils.Point calculateQuantifierPosition(GeometryUtils.Point anchorPoint, GeometryUtils.Point startPoint, GeometryUtils.Point endPoint,
                                                           double headSize, boolean isStart) {
        double dx = endPoint.x() - startPoint.x();
        double dy = endPoint.y() - startPoint.y();
        double length = Math.sqrt(dx * dx + dy * dy);

        double unitX = dx / length;
        double unitY = dy / length;

        double perpX = -unitY;
        double perpY = unitX;

        double direction = isStart ? 1 : -1;

        double lineOffset = 15;
        double perpOffset = 15;
        return new GeometryUtils.Point(
                anchorPoint.x() + unitX * direction * (headSize + lineOffset) + perpX * perpOffset,
                anchorPoint.y() + unitY * direction * (headSize + lineOffset) + perpY * perpOffset
        );
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