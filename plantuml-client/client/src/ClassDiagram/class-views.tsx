/*
 * File: class-views.tsx
 * Author: Norman Babiak
 * Description: Label, note and other edge views for class diagram
 * Date: 29.4.2026
 */

import {injectable} from 'inversify';
import {
    GEdge,
    GEdgeView,
    GLabel,
    GLabelView,
    GNode,
    IView,
    IViewArgs,
    Point,
    RenderingContext,
    svg
} from '@eclipse-glsp/client';
import {VNode} from "snabbdom";
import '../../css/diagram.css';
import {createIcon, renderVisibilityShape} from "../utils-common";
import {CurvedEdgeRenderer} from "./ClassEdge/curved-edge-view";

/** @jsx svg */

/**
 * Maps a PlantUML entity type to its icon color and single-character stereotype. Falls back to class
 */
export function getTypeConfig(type: string): { color: string; char: string } {
    const configs: Record<string, { color: string; char: string }> = {
        'abstract': { color: '#6DBABA', char: 'A' },
        'abstract class': { color: '#6DBABA', char: 'A' },
        'annotation': { color: '#FF6B35', char: '@' },
        'class': { color: '#ADD1B2', char: 'C' },
        'dataclass': { color: '#9B59B6', char: 'D' },
        'entity': { color: '#50C878', char: 'E' },
        'enum': { color: '#B85450', char: 'E' },
        'exception': { color: '#E74C3C', char: 'X' },
        'interface': { color: '#6C7AC4', char: 'I' },
        'metaclass': { color: '#7F8C8D', char: 'M' },
        'protocol': { color: '#95A5A6', char: 'P' },
        'record': { color: '#F39C12', char: 'R' },
        'stereotype': { color: '#E91E63', char: 'S' },
        'struct': { color: '#BDC3C7', char: 'S' },
    };

    return configs[type] || configs['class'];
}

/**
 * Renders the entity name label with a colored type icon, optional visibility shape, and bold/italic styling based on
 * the entity type
 */
@injectable()
export class EntityLabelView extends GLabelView {
    override render(label: Readonly<GLabel>, context: RenderingContext, args?: IViewArgs): VNode {
        const text = label.text ?? '';

        const type = (label as any).args?.type
        const typeConfig = getTypeConfig(type);

        const width = (label as any).args?.width;
        const background = typeConfig.color;

        const hasGeneric = (label as any).args?.hasGeneric as boolean | undefined;

        // Check for stereotype
        const stereotypeChar = (label as any).args?.stereotypeChar as string | undefined;
        const stereotypeColor = (label as any).args?.stereotypeColor as string | undefined;
        const hasStereotypeChar = stereotypeChar && stereotypeChar.trim().length > 0 && stereotypeChar !== ' ';
        const hasStereotypeColor = stereotypeColor && stereotypeColor.length > 0;

        const displayChar = hasStereotypeChar ? stereotypeChar : typeConfig.char;
        const iconColor = hasStereotypeColor ? stereotypeColor : background;

        const isBold = text.startsWith('=');
        const content = isBold ? text.slice(1).trim() : text;

        const isItalic = ['abstract_class', 'interface'].includes(type);
        const visibility = (label as any).args?.visibility as string | undefined;

        const visibilityShape = renderVisibilityShape(visibility, false);
        const iconRadius = 8;
        const iconRightEdge = -width/2 + iconRadius + 2 + iconRadius;
        const shapeOffset = width ? iconRightEdge + 3 : 0;

        return <g>
            {!hasGeneric && createIcon(width, iconColor, displayChar)}
            {visibilityShape && <g transform={`translate(${shapeOffset}, 0)`}>{visibilityShape}</g>}

            <text
                x={visibilityShape ? 15 : 5}
                y={0}
                style={{
                    fontWeight: isBold ? 'bold' : 'normal',
                    fontStyle: isItalic ? 'italic' : 'normal'
                }}
                class-sprotty-label={true}
                text-anchor="start"
            >
                {content}
            </text>
        </g>;
    }
}

/**
 * Invisible label view
 */
@injectable()
export class HiddenLabelView implements IView {
    render(label: Readonly<GLabel>, context: RenderingContext): VNode {
        return <g></g>;
    }
}

/**
 * Renders a note edge to a specific entity, or member of an entity
 */
@injectable()
export class SimpleNoteEdgeView extends GEdgeView {
    protected override renderLine(edge: GEdge, segments: Point[], context: RenderingContext): VNode {
        if (segments.length >= 2) {
            const source = edge.source as GNode;
            const target = edge.target as GNode;
            const memberName = (edge.args as any)?.memberName;
            const isMemberTip = memberName && typeof memberName === 'string' && memberName.trim().length > 0 && source;

            if (isMemberTip) {
                return this.renderMemberTipLink(source, target, segments, memberName);

            } else {
                const isSourceNote = source.type === "entity:note";

                if (isSourceNote) {
                    return this.renderRegularNoteLink(source, segments, true);

                } else {
                    return this.renderRegularNoteLink(target, segments, false);
                }
            }
        }

        return <g />;
    }

    /**
     * Renders a note edge that points to a specific member
     */
    private renderMemberTipLink(source: GNode, target: GNode, segments: Point[], memberName: string): VNode {
        const memberYOffset = CurvedEdgeRenderer.getMemberYOffset(source, memberName);
        const first = {
            x: source.position.x + source.size.width,
            y: source.position.y + memberYOffset
        };

        const last = segments[segments.length - 1];
        const noteColor = (target as any).args.background;

        const baseWidth = 12;

        // Estimate the text extent to place the tip near the member name's end
        const charWidth = 6.5;
        const textWidth = memberName.length * charWidth;
        const entityCenterX = source.position.x + source.size.width / 2;
        const textEndX = entityCenterX + textWidth / 2;

        const tipAnchorX = Math.min(textEndX, source.position.x + source.size.width - 10);
        const tipAnchorY = first.y;

        // Perpendicular offset to give the base its width
        const dx = last.x - tipAnchorX;
        const dy = last.y - tipAnchorY;
        const length = Math.sqrt(dx * dx + dy * dy);
        const perpX = -dy / length * baseWidth / 2;
        const perpY = dx / length * baseWidth / 2;

        return <polygon
            points={`${last.x - perpX},${last.y - perpY} 
                     ${last.x + perpX},${last.y + perpY} 
                     ${tipAnchorX},${tipAnchorY}`}
            fill={noteColor}
            stroke={noteColor}
            stroke-width="1"
            class-note-link={true}
        />;
    }

    /**
     * Renders a standard note edge as a filled triangle
     */
    private renderRegularNoteLink(source: GNode, segments: Point[], noteAtStart: boolean): VNode {
        const first = segments[0];
        const last = segments[segments.length - 1];

        const base = noteAtStart ? first : last;
        const tip  = noteAtStart ? last : first;

        const noteColor = (source as any).args.background;
        const baseWidth = 12;

        const dx = last.x - first.x;
        const dy = last.y - first.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const perpX = -dy / length * baseWidth / 2;
        const perpY = dx / length * baseWidth / 2;

        return <polygon
            points={`${base.x - perpX},${base.y - perpY}
                    ${base.x + perpX},${base.y + perpY}
                    ${tip.x},${tip.y}`}
            fill={noteColor}
            stroke={noteColor}
            stroke-width="1"
            class-note-link={true}
        />;
    }
}
