/*
 * File: utils.tsx
 * Author: Norman Babiak
 * Description: Utility functions for arrow rendering and edge creation, calculation
 * Date: 29.4.2026
 */

import { GNode, Point, svg } from '@eclipse-glsp/client';
import { VNode } from 'snabbdom';
import {
    ARROW_PATHS, ARROW_SIZES, EDGE_CONFIG, FILLED_ARROWS,
    ArrowHead, isDiamondEntity, LineStyle
} from './ClassEdge/types';

/** @jsx svg */

/**
 * Returns the SVG stroke-dasharray value corresponding to a PlantUML line style
 */
export function getStrokeDashArray(style: LineStyle): string {
    switch (style) {
        case 'DASHED': return '5,5';
        case 'DOTTED': return '1,5';
        default: return 'none';
    }
}

/**
 * Utility object for rendering arrow heads at edge endpoints, looking up head sizes, and computing angles between two points.
 */
export const ArrowHeadRenderer = {
    /**
     * Returns the reserved space that a given arrow head type has along the edge.
     */
    getSize(kind: ArrowHead): number {
        return ARROW_SIZES[kind] ?? 0;
    },

    /**
     * Renders an SVG path for the given arrow head type
     */
    render(kind: ArrowHead, position: Point, angle: number, color: string, thickness: number): VNode | null {
        if (kind === 'none') return null;
        const path = ARROW_PATHS[kind];

        const isFilled = FILLED_ARROWS.has(kind);
        return (
            <path
                d={path}
                transform={`translate(${position.x} ${position.y}) rotate(${angle}) scale(${thickness})`}
                style={{ fill: isFilled ? color : 'none' }}
                stroke={color}
                stroke-width={1}
            />
        );
    },

    /**
     * Computes the angle in degrees from start to end
     */
    getAngle(start: Point, end: Point): number {
        return Math.atan2(end.y - start.y, end.x - start.x) * 180 / Math.PI;
    }
};

/** ň
 * Utility object for computing anchor points on entity boundaries
 */
export const AnchorCalculator = {
    /**
     * Dispatches to diamond or rectangle boundary calculation depending on the entity shape.
     */
    getBoundaryPoint(node: GNode, refX: number, refY: number): Point {
        if (isDiamondEntity(node)) {
            return this.getDiamondBoundaryPoint(node, refX, refY);
        }

        return this.getRectangleBoundaryPoint(node, refX, refY);
    },

    /**
     * Finds the point where a ray from the rectangle's center toward intersects the rectangle boundary.
     */
    getRectangleBoundaryPoint(node: GNode, refX: number, refY: number): Point {
        const cx = node.position.x + node.size.width / 2;
        const cy = node.position.y + node.size.height / 2;
        const dx = refX - cx;
        const dy = refY - cy;

        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
            return {x: node.position.x + node.size.width, y: cy};
        }

        const hw = node.size.width / 2;
        const hh = node.size.height / 2;
        const t = Math.abs(dx) / hw > Math.abs(dy) / hh
            ? (dx > 0 ? hw : -hw) / dx
            : (dy > 0 ? hh : -hh) / dy;

        return {x: cx + t * dx, y: cy + t * dy};
    },

    /**
     * Returns the four tip points of a diamond-shaped entity
     */
    getDiamondTips(node: GNode): { top: Point; right: Point; bottom: Point; left: Point } {
        const x = node.position.x, y = node.position.y;
        const w = node.size.width, h = node.size.height;
        const cx = x + w / 2, cy = y + h / 2;

        return {
            top: { x: cx, y }, right: { x: x + w, y: cy },
            bottom: { x: cx, y: y + h }, left: { x, y: cy }
        };
    },

    /**
     * Picks the diamond tip whose quadrant contains the reference point.
     */
    getClosestDiamondTip(node: GNode, refX: number, refY: number): Point {
        const tips = this.getDiamondTips(node);
        const cx = node.position.x + node.size.width / 2;
        const cy = node.position.y + node.size.height / 2;
        const dx = refX - cx, dy = refY - cy;

        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) return tips.right;

        const angle = Math.atan2(dy, dx);
        const PI = Math.PI;
        if (angle >= -PI / 4 && angle < PI / 4) return tips.right;
        if (angle >= PI / 4 && angle < 3 * PI / 4) return tips.bottom;
        if (angle >= -3 * PI / 4 && angle < -PI / 4) return tips.top;

        return tips.left;
    },

    getDiamondBoundaryPoint(node: GNode, refX: number, refY: number): Point {
        const cx = node.position.x + node.size.width / 2;
        const cy = node.position.y + node.size.height / 2;
        const dx = refX - cx, dy = refY - cy;

        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
            return this.getDiamondTips(node).right;
        }

        const tips = this.getDiamondTips(node);
        const edges: [Point, Point][] = [
            [tips.top, tips.right], [tips.right, tips.bottom],
            [tips.bottom, tips.left], [tips.left, tips.top]
        ];

        for (const [p1, p2] of edges) {
            const intersection = this.lineRayIntersection(cx, cy, dx, dy, p1, p2);
            if (intersection) return intersection;
        }

        return this.getClosestDiamondTip(node, refX, refY);
    },

    /**
     * Finds where a ray hits a line segment, or null if it misses. Used to attach edges to diamond walls
     */
    lineRayIntersection(ox: number, oy: number, dx: number, dy: number, p1: Point, p2: Point): Point | null {
        const ex = p2.x - p1.x, ey = p2.y - p1.y;
        const denom = dx * ey - dy * ex;

        if (Math.abs(denom) < 0.0001) return null;

        const t = ((p1.x - ox) * ey - (p1.y - oy) * ex) / denom;
        const s = ((p1.x - ox) * dy - (p1.y - oy) * dx) / denom;

        if (t > 0 && s >= 0 && s <= 1) {
            return {x: ox + t * dx, y: oy + t * dy};
        }

        return null;
    },

    /**
     * Slides a boundary anchor point along the entity edge by a perpendicular offset, clamping to stay within the entity bounds
     * Used to separate parallel edges
     */
    slideBoundaryPoint(node: GNode, base: Point, perpX: number, perpY: number, offset: number): Point {
        const left = node.position.x, top = node.position.y;
        const right = left + node.size.width, bottom = top + node.size.height;

        const dL = Math.abs(base.x - left), dR = Math.abs(base.x - right);
        const dT = Math.abs(base.y - top), dB = Math.abs(base.y - bottom);
        const min = Math.min(dL, dR, dT, dB);

        if (min === dL || min === dR) {
            return { x: base.x, y: Math.max(top, Math.min(bottom, base.y + perpY * offset)) };
        }
        return { x: Math.max(left, Math.min(right, base.x + perpX * offset)), y: base.y };
    },

    /**
     * Computes separated source/target anchor points for one edge in a parallel bundle. Handles all combinations of
     * rectangle and diamond entities
     */
    getParallelAnchors(source: GNode, target: GNode, index: number, totalCount: number) {
        const srcCx = source.position.x + source.size.width / 2;
        const srcCy = source.position.y + source.size.height / 2;
        const tgtCx = target.position.x + target.size.width / 2;
        const tgtCy = target.position.y + target.size.height / 2;

        // Perpendicular to the center-to-center line
        const dx = tgtCx - srcCx, dy = tgtCy - srcCy;
        const len = Math.sqrt(dx * dx + dy * dy) || 1;
        const perpX = -dy / len, perpY = dx / len;

        // Offset from the bundle center
        const middle = (totalCount - 1) / 2;
        const offset = (index - middle) * EDGE_CONFIG.parallel.spacing;

        const sourceIsDiamond = isDiamondEntity(source);
        const targetIsDiamond = isDiamondEntity(target);

        let sourceAnchor: Point, targetAnchor: Point;

        if (sourceIsDiamond && targetIsDiamond) {
            sourceAnchor = this.getClosestDiamondTip(source, tgtCx, tgtCy);
            targetAnchor = this.getClosestDiamondTip(target, srcCx, srcCy);

        } else if (sourceIsDiamond) {
            sourceAnchor = this.getClosestDiamondTip(source, tgtCx, tgtCy);
            const base = this.getBoundaryPoint(target, srcCx, srcCy);
            targetAnchor = this.slideBoundaryPoint(target, base, perpX, perpY, offset);

        } else if (targetIsDiamond) {
            targetAnchor = this.getClosestDiamondTip(target, srcCx, srcCy);
            const base = this.getBoundaryPoint(source, tgtCx, tgtCy);
            sourceAnchor = this.slideBoundaryPoint(source, base, perpX, perpY, offset);

        } else {
            const baseSource = this.getBoundaryPoint(source, tgtCx, tgtCy);
            const baseTarget = this.getBoundaryPoint(target, srcCx, srcCy);
            sourceAnchor = this.slideBoundaryPoint(source, baseSource, perpX, perpY, offset);
            targetAnchor = this.slideBoundaryPoint(target, baseTarget, perpX, perpY, offset);
        }

        return { sourceAnchor, targetAnchor, offset };
    }
};
