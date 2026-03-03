package com.diagrams.ClassDiagram.utils;

import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Geometry Calculation Tests")
class GeometryCalculationTest {

    @Nested
    @DisplayName("GeometryUtils.Point")
    class PointTests {

        @Test
        @DisplayName("midpoint calculates center")
        void midpoint() {
            var a = new GeometryUtils.Point(0, 0);
            var b = new GeometryUtils.Point(100, 200);

            var mid = a.midpoint(b);

            assertEquals(50, mid.x());
            assertEquals(100, mid.y());
        }

        @Test
        @DisplayName("distanceTo uses Pythagorean theorem")
        void distanceTo() {
            var a = new GeometryUtils.Point(0, 0);
            var b = new GeometryUtils.Point(3, 4);

            assertEquals(5.0, a.distanceTo(b), 0.001);
        }
    }

    @Nested
    @DisplayName("GeometryUtils.Vector")
    class VectorTests {

        @Test
        @DisplayName("normalize creates unit vector")
        void normalize() {
            var v = new GeometryUtils.Vector(3, 4);
            var norm = v.normalize();

            assertEquals(1.0, norm.length(), 0.001);
        }

        @Test
        @DisplayName("perpendicular rotates 90 degrees")
        void perpendicular() {
            var v = new GeometryUtils.Vector(1, 0);
            var perp = v.perpendicular();

            assertEquals(0, perp.dx(), 0.001);
            assertEquals(1, perp.dy(), 0.001);
        }

        @Test
        @DisplayName("negate reverses direction")
        void negate() {
            var v = new GeometryUtils.Vector(5, -3);
            var neg = v.negate();

            assertEquals(-5, neg.dx());
            assertEquals(3, neg.dy());
        }
    }

    @Nested
    @DisplayName("GeometryUtils.Rectangle")
    class RectangleTests {

        @Test
        @DisplayName("boundaryIntersection finds right edge")
        void boundaryIntersectionRight() {
            var rect = new GeometryUtils.Rectangle(0, 0, 100, 100);
            var target = new GeometryUtils.Point(200, 50);
            var boundary = rect.boundaryIntersection(target);

            assertEquals(100, boundary.x(), 0.001);
        }
    }

    @Nested
    @DisplayName("CurvedEdgeCalculator")
    class CurvedEdgeCalculatorTests {
        private CurvedEdgeCalculator calculator;

        @BeforeEach
        void setup() {
            calculator = new CurvedEdgeCalculator();
        }

        @Test
        @DisplayName("getMemberYOffset returns center for null member")
        void offsetForNullMember() {
            var size = new ClassLayout.Size(100, 80);
            double offset = calculator.getMemberYOffset(size, false, List.of(), List.of(), null);

            assertEquals(40, offset);
        }

        @Test
        @DisplayName("getMemberYOffset finds field by name")
        void offsetFindsField() {
            var size = new ClassLayout.Size(100, 100);
            var fields = List.of(new EntityMethod("+name"), new EntityMethod("+age"));

            double offset = calculator.getMemberYOffset(size, false, fields, List.of(), "age");

            assertTrue(offset > 30 && offset < 80);
        }

        @Test
        @DisplayName("shouldCurveToRight flip reverses direction")
        void flipReversesDirection() {
            var size = new ClassLayout.Size(100, 80);

            boolean normal = calculator.shouldCurveToRight(0, 100, size, size, false);
            boolean flipped = calculator.shouldCurveToRight(0, 100, size, size, true);

            assertNotEquals(normal, flipped);
        }

        @Test
        @DisplayName("calculateCurve returns normalized tangent")
        void curveHasNormalizedTangent() {
            var start = new GeometryUtils.Point(0, 0);
            var end = new GeometryUtils.Point(100, 100);

            var curve = calculator.calculateCurve(start, end, false);

            double tangentLength = Math.hypot(curve.midTangentX(), curve.midTangentY());
            assertEquals(1.0, tangentLength, 0.001);
        }
    }

    @Nested
    @DisplayName("LinkGeometry")
    class LinkGeometryTests {
        private LinkGeometry linkGeometry;
        private ClassLayout.Size defaultSize;

        @BeforeEach
        void setup() {
            linkGeometry = new LinkGeometry();
            defaultSize = new ClassLayout.Size(100, 80);
        }

        @Test
        @DisplayName("create returns StraightEdge without members")
        void straightEdgeWithoutMembers() {
            ClassLink link = createLink(null, null);
            var edge = linkGeometry.create(link, defaultSize, defaultSize);

            assertInstanceOf(LinkGeometry.StraightEdge.class, edge);
        }

        @Test
        @DisplayName("create returns CurvedEdge with source member")
        void curvedEdgeWithSourceMember() {
            ClassLink link = createLink("field1", null);
            var edge = linkGeometry.create(link, defaultSize, defaultSize);

            assertInstanceOf(LinkGeometry.CurvedEdge.class, edge);
        }

        @Test
        @DisplayName("create returns CurvedEdge with target member")
        void curvedEdgeWithTargetMember() {
            ClassLink link = createLink(null, "method1");
            var edge = linkGeometry.create(link, defaultSize, defaultSize);

            assertInstanceOf(LinkGeometry.CurvedEdge.class, edge);
        }

        @Test
        @DisplayName("createParallel returns ParallelEdge")
        void parallelEdge() {
            ClassLink link = createLink(null, null);
            var edge = linkGeometry.createParallel(link, defaultSize, defaultSize, false, 0, 3);

            assertInstanceOf(LinkGeometry.ParallelEdge.class, edge);
        }

        private ClassLink createLink(String sourceMember, String targetMember) {
            ClassEntity e1 = new ClassEntity(0, 0, "ent-0", "A", "CLASS",
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            ClassEntity e2 = new ClassEntity(200, 0, "ent-1", "B", "CLASS",
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

            e1.setStereotypeName("");
            e2.setStereotypeName("");

            ClassLink link = new ClassLink("link-0", e1, e2, "ARROW", "", 1,
                    "NONE", "ARROW", "", "");

            if (sourceMember != null) link.setSourceMember(sourceMember);
            if (targetMember != null) link.setTargetMember(targetMember);

            return link;
        }
    }
}