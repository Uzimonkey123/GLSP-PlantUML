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
        const stereotypeName = (label as any).args?.stereotypeName as string | undefined;
        const stereotypeChar = (label as any).args?.stereotypeChar as string | undefined;
        const stereotypeColor = (label as any).args?.stereotypeColor as string | undefined;
        const hasStereotypeName = stereotypeName && stereotypeName.length > 0;
        const hasStereotypeChar = stereotypeChar && stereotypeChar.trim().length > 0 && stereotypeChar !== ' ';
        const hasStereotypeColor = stereotypeColor && stereotypeColor.length > 0;

        const displayChar = hasStereotypeChar ? stereotypeChar : typeConfig.char;
        const iconColor = hasStereotypeColor ? stereotypeColor : background;

        const isBold = text.startsWith('=');
        const content = isBold ? text.slice(1).trim() : text;

        const isItalic = ['abstract_class', 'interface'].includes(type);
        const visibility = (label as any).args?.visibility as string | undefined;

        const visibilityShape = renderVisibilityShape(visibility);
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

@injectable()
export class HiddenLabelView implements IView {
    render(label: Readonly<GLabel>, context: RenderingContext): VNode {
        return <g></g>;
    }
}

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
                return this.renderRegularNoteLink(source, segments);
            }
        }

        return <g />;
    }

    private renderMemberTipLink(source: GNode, target: GNode, segments: Point[], memberName: string): VNode {
        const memberYOffset = CurvedEdgeRenderer.getMemberYOffset(source, memberName);
        const first = {
            x: source.position.x + source.size.width,
            y: source.position.y + memberYOffset
        };

        const last = segments[segments.length - 1];
        const noteColor = (target as any).args.background;

        const baseWidth = 12;

        const charWidth = 6.5;
        const textWidth = memberName.length * charWidth;
        const entityCenterX = source.position.x + source.size.width / 2;
        const textEndX = entityCenterX + textWidth / 2;

        const tipAnchorX = Math.min(textEndX, source.position.x + source.size.width - 10);
        const tipAnchorY = first.y;

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

    private renderRegularNoteLink(source: GNode, segments: Point[]): VNode {
        const first = segments[0];
        const last = segments[segments.length - 1];

        const noteColor = (source as any).args.background;
        const baseWidth = 12;

        const dx = last.x - first.x;
        const dy = last.y - first.y;
        const length = Math.sqrt(dx * dx + dy * dy);
        const perpX = -dy / length * baseWidth / 2;
        const perpY = dx / length * baseWidth / 2;

        return <polygon
            points={`${first.x - perpX},${first.y - perpY} 
                     ${first.x + perpX},${first.y + perpY} 
                     ${last.x},${last.y}`}
            fill={noteColor}
            stroke={noteColor}
            stroke-width="1"
            class-note-link={true}
        />;
    }
}