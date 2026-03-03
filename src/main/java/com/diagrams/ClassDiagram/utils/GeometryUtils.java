package com.diagrams.ClassDiagram.utils;

public class GeometryUtils {

    public record Point(double x, double y) {

        public Point offset(Vector v, double distance) {
            return new Point(x + v.dx() * distance, y + v.dy() * distance);
        }

        public Point offset(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }

        public Point midpoint(Point other) {
            return new Point((x + other.x) / 2, (y + other.y) / 2);
        }

        public double distanceTo(Point other) {
            return Math.hypot(other.x - x, other.y - y);
        }
    }

    public record Vector(double dx, double dy) {
        public Vector(Point from, Point to) {
            this(to.x() - from.x(), to.y() - from.y());
        }

        public Vector normalize() {
            double len = length();
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

    public record Dimensions(double width, double height) {}

    public record Rectangle(double x, double y, double width, double height) {
        public Point center() {
            return new Point(x + width / 2, y + height / 2);
        }

        public Point boundaryIntersection(Point target) {
            Point c = center();
            double dx = target.x() - c.x();
            double dy = target.y() - c.y();

            if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
                return new Point(x + width, c.y());
            }

            double hw = width / 2;
            double hh = height / 2;
            double t = (Math.abs(dx) / hw > Math.abs(dy) / hh)
                    ? (dx > 0 ? hw : -hw) / dx
                    : (dy > 0 ? hh : -hh) / dy;

            return new Point(c.x() + t * dx, c.y() + t * dy);
        }
    }

    private GeometryUtils() {}
}