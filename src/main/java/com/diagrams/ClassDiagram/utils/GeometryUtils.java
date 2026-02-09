package com.diagrams.ClassDiagram.utils;

public class GeometryUtils {
    public record Point(double x, double y) {

        public Point offset(Vector v, double distance) {
                return new Point(x + v.dx * distance, y + v.dy * distance);
            }

            public Point offset(double dx, double dy) {
                return new Point(x + dx, y + dy);
            }
        }

    public static class Vector {
        public final double dx;
        public final double dy;

        public Vector(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Vector(Point from, Point to) {
            this.dx = to.x - from.x;
            this.dy = to.y - from.y;
        }

        public Vector normalize() {
            double len = Math.hypot(dx, dy);
            return len < 0.01 ? this : new Vector(dx / len, dy / len);
        }

        public Vector perpendicular() {
            return new Vector(-dy, dx);
        }

        public Vector negate() {
            return new Vector(-dx, -dy);
        }

        public double length() {
            return Math.hypot(dx, dy);
        }
    }

    public record Dimensions(double width, double height) {
    }

    private GeometryUtils() {}
}