/*
 * File: GeometryCalculationTest.java
 * Author: Norman Babiak
 * Description: Tests for geometry calculations of class diagram, point, vector, rectangle...
 * Date: 28.4.2026
 */

package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Geometry Tests")
class GeometryCalculationTest {

    @Test
    @DisplayName("Point: midpoint and distance")
    void pointOps() {
        GeometryUtils.Point pointA = new GeometryUtils.Point(0, 0);
        GeometryUtils.Point pointB = new GeometryUtils.Point(100, 200);
        GeometryUtils.Point midpoint = pointA.midpoint(pointB);

        assertEquals(50, midpoint.x());
        assertEquals(100, midpoint.y());
        assertEquals(5.0, new GeometryUtils.Point(0, 0).distanceTo(new GeometryUtils.Point(3, 4)), 0.001);
    }

    @Test
    @DisplayName("Vector: normalize, perpendicular, negate")
    void vectorOps() {
        GeometryUtils.Vector vector = new GeometryUtils.Vector(3, 4);
        GeometryUtils.Vector unit = new GeometryUtils.Vector(1, 0);
        GeometryUtils.Vector perp = unit.perpendicular();

        assertEquals(1.0, vector.normalize().length(), 0.001);
        assertEquals(0, perp.dx(), 0.001);
        assertEquals(1, perp.dy(), 0.001);
        assertEquals(-5, new GeometryUtils.Vector(5, -3).negate().dx());
    }

    @Test
    @DisplayName("Rectangle: boundary intersection hits edge")
    void rectangleBoundary() {
        GeometryUtils.Rectangle rect = new GeometryUtils.Rectangle(0, 0, 100, 100);
        GeometryUtils.Point intersection = rect.boundaryIntersection(new GeometryUtils.Point(200, 50));

        assertEquals(100, intersection.x(), 0.001);
    }

    @Test
    @DisplayName("CurvedEdgeCalculator: null member returns center offset")
    void curveNullMember() {
        CurvedEdgeCalculator calculator = new CurvedEdgeCalculator();
        ClassLayout.Size size = new ClassLayout.Size(100, 80);

        assertEquals(40, calculator.getMemberYOffset(size, false, List.of(), List.of(), null));
    }

    @Test
    @DisplayName("CurvedEdgeCalculator: finds field by name")
    void curveFieldOffset() {
        List<EntityMethod> fields = List.of(new EntityMethod("+name"), new EntityMethod("+age"));
        CurvedEdgeCalculator calculator = new CurvedEdgeCalculator();
        ClassLayout.Size size = new ClassLayout.Size(100, 100);

        double offset = calculator.getMemberYOffset(size, false, fields, List.of(), "age");

        assertTrue(offset > 30 && offset < 80);
    }

    @Test
    @DisplayName("CurvedEdgeCalculator: flip reverses curve direction, tangent normalized")
    void curveDirectionAndTangent() {
        ClassLayout.Size size = new ClassLayout.Size(100, 80);
        CurvedEdgeCalculator calculator = new CurvedEdgeCalculator();

        boolean normal = calculator.shouldCurveToRight(0, 100, size, size, false);
        boolean flipped = calculator.shouldCurveToRight(0, 100, size, size, true);
        assertNotEquals(normal, flipped);

        var curve = calculator.calculateCurve(new GeometryUtils.Point(0, 0), new GeometryUtils.Point(100, 100), false);
        assertEquals(1.0, Math.hypot(curve.midTangentX(), curve.midTangentY()), 0.001);
    }

    @Test
    @DisplayName("LinkGeometry: straight without members, curved with members")
    void linkGeometryTypes() {
        LinkGeometry linkGeometry = new LinkGeometry();
        ClassLayout.Size size = new ClassLayout.Size(100, 80);

        assertInstanceOf(LinkGeometry.StraightEdge.class, linkGeometry.create(makeLink(null, null), size, size));
        assertInstanceOf(LinkGeometry.CurvedEdge.class, linkGeometry.create(makeLink("f1", null), size, size));
        assertInstanceOf(LinkGeometry.CurvedEdge.class, linkGeometry.create(makeLink(null, "m1"), size, size));
    }

    @Test
    @DisplayName("LinkGeometry: createParallel returns ParallelEdge")
    void parallelEdge() {
        LinkGeometry linkGeometry = new LinkGeometry();
        ClassLayout.Size size = new ClassLayout.Size(100, 80);

        assertInstanceOf(LinkGeometry.ParallelEdge.class,
                linkGeometry.createParallel(makeLink(null, null), size, size, false, 0, 3));
    }

    private ClassLink makeLink(String sourceMember, String targetMember) {
        ClassEntity entity1 = new ClassEntity(0, 0, "e0", "A", "CLASS", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        ClassEntity entity2 = new ClassEntity(200, 0, "e1", "B", "CLASS", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity1.setStereotypeName("");
        entity2.setStereotypeName("");

        ClassLink link = new ClassLink("l", entity1, entity2, "ARROW", "", 1, "NONE", "ARROW", "", "");
        if (sourceMember != null) link.setSourceMember(sourceMember);
        if (targetMember != null) link.setTargetMember(targetMember);

        return link;
    }
}

