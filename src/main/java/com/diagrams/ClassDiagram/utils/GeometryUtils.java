/*
 * File: GeometryUtils.java
 * Author: Norman Babiak
 * Description: Geometry primitives for edge calculations.
 * Date: 6.5.2026
 */

package com.diagrams.ClassDiagram.utils;

public class GeometryUtils {

    public record Point(double x, double y) {

        /** Returns a new point shifted along a vector by the given distance */
        public Point offset(Vector v, double distance) {
            return new Point(x + v.dx() * distance, y + v.dy() * distance);
        }

        /** Returns a new point shifted by raw dx/dy values */
        public Point offset(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }

        /** Returns the midpoint between this point and another */
        public Point midpoint(Point other) {
            return new Point((x + other.x) / 2, (y + other.y) / 2);
        }

        /** Returns the distance to another point */
        public double distanceTo(Point other) {
            return Math.hypot(other.x - x, other.y - y);
        }
    }

    public record Vector(double dx, double dy) {
        /** Creates a vector from one point to another */
        public Vector(Point from, Point to) {
            this(to.x() - from.x(), to.y() - from.y());
        }

        /** Returns the unit vector in the same direction */
        public Vector normalize() {
            double len = length();
            return len < 0.01 ? this : new Vector(dx / len, dy / len);
        }

        /** Returns the 90° perpendicular vector */
        public Vector perpendicular() {
            return new Vector(-dy, dx);
        }

        /** Returns the vector pointing in the opposite direction */
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

        /**
         * Finds the point where a ray from the rectangle's center toward the target crosses the rectangle boundary.
         */
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