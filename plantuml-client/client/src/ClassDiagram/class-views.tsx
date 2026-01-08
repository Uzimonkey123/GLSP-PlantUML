import {injectable} from 'inversify';
import {
    GEdge,
    GLabel,
    GLabelView,
    IViewArgs, Point, PolylineEdgeView,
    RenderingContext,
    svg
} from '@eclipse-glsp/client';
import {VNode} from "snabbdom";
import '../../css/diagram.css';
import {createIcon, renderVisibilityShape} from "../utils";
import {renderLine} from "../SequenceDiagram/sequence-views";

/** @jsx svg */

@injectable()
export class EntityLabelView extends GLabelView {
    override render(label: Readonly<GLabel>, context: RenderingContext, args?: IViewArgs): VNode {
        const text = label.text ?? '';

        const type = (label as any).args?.type
        const typeConfig = this.getTypeConfig(type);

        const width = (label as any).args?.width;
        const background = typeConfig.color;
        const stereotypeChar = typeConfig.char;

        const isBold = text.startsWith('=');
        const content = isBold ? text.slice(1).trim() : text;

        const isItalic = ['abstract_class', 'interface'].includes(type);
        const visibility = (label as any).args?.visibility as string | undefined;

        const visibilityShape = renderVisibilityShape(visibility);
        const iconRadius = 8;
        const iconRightEdge = -width/2 + iconRadius + 2 + iconRadius;
        const shapeOffset = width ? iconRightEdge + 3 : 0;

        return <g>
            {createIcon(width, background, stereotypeChar)}
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

    private getTypeConfig(type: string): { color: string; char: string } {
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
}

@injectable()
export class ClassLinkView extends PolylineEdgeView {
    private start = {x: 0, y: 0};
    private end = {x: 0, y: 0};

    private style! : string;
    private headStart! : string;
    private headEnd! : string;
    private arrColor! : string;

    private headPath(kind: string): string | undefined {
        let offset = 8;

        switch (`${kind}`) {
            case 'EXTENDS':
                return `M0,-4 L${offset},0 L0,4 Z`;
            case 'COMPOSITION':
            case 'AGREGATION':
                return `M0,0 L${offset / 2},-4 L${offset},0 L${offset / 2},4 Z`;
            case 'ARROW':
                return `M0,-4 L${offset},0 M0,4 L${offset},0, M0,0 L${offset},0`;
            case 'SQUARE':
                return `M0,-4 L${offset},-4 L${offset},4 L0,4 Z`;
            case 'NOT_NAVIGABLE':
                return `M-4,-4 L4,4 M4,-4 L-4,4`;
            case 'CROWFOOT':
                return `M0,0 L${offset},-4 M0,0 L${offset},0 M0,0 L${offset},4`;
            case 'PLUS':
                return `M0,-4 A 4,4 0 1,1 0,4 A 4,4 0 1,1 0,-4 Z M0,-4 L0,4 M-4,0 L4,0`;
            default:
                return undefined;
        }
    }

    private getHeadSize(kind: string): number {
        const sizes: Record<string, number> = {
            'EXTENDS': 8,
            'COMPOSITION': 8,
            'AGREGATION': 8,
            'ARROW': 8,
            'SQUARE': 4,
            'NOT_NAVIGABLE': 10,
            'CROWFOOT': 8,
            'PLUS': 4,
            'none': 0
        };

        return sizes[kind] || 0;
    }

    protected override renderAdditionals(
        edge: GEdge,
        segments: Point[],
        context: RenderingContext,
        args?: IViewArgs
    ): VNode[] {

        const additionals = super.renderAdditionals(edge, segments, context);
        if (segments.length < 2) return additionals;

        this.start = segments[0];
        this.end = segments[segments.length - 1];

        this.style = (edge.args?.style as string) ?? 'normal'
        this.headStart = (edge.args?.headStart as string) ?? 'none';
        this.headEnd = (edge.args?.headEnd as string) ?? 'none';

        this.drawSimpleArrow(additionals);

        return additionals;
    }

    private drawMessageLine(start: {x: number, y:number}, end: {x: number, y: number}, additionals: VNode[]): renderLine {
        const dx = end.x - start.x;
        const dy = end.y - start.y;
        const length = Math.sqrt(dx * dx + dy * dy);

        const strokeWidth = 1.5;

        const startHeadSize = this.getHeadSize(this.headStart);
        const endHeadSize = this.getHeadSize(this.headEnd);

        const ux = dx / length;
        const uy = dy / length;

        const lineStart = {
            x: start.x + ux * startHeadSize,
            y: start.y + uy * startHeadSize
        };

        const lineEnd = {
            x: end.x - ux * endHeadSize,
            y: end.y - uy * endHeadSize
        };

        additionals.unshift(
            <path
                d={`M ${lineStart.x} ${lineStart.y} L ${lineEnd.x} ${lineEnd.y}`}
                stroke="black"
                strokeWidth={strokeWidth}
                marker-end="none"
                fill="none"
                class-sprotty-edge={true}
            />
        );

        const angle = (Math.atan2(dy, dx) * 180) / Math.PI;

        const endArrowPos = { x: lineEnd.x, y: lineEnd.y };
        const startArrowPos = { x: lineStart.x, y: lineStart.y };

        return {
            startArrowPos,
            endArrowPos,
            angle,
            strokeWidth
        };
    }

    private drawSimpleArrow(additionals: VNode[]) {
        const {
            startArrowPos, endArrowPos, angle, strokeWidth
        } = this.drawMessageLine(this.start, this.end, additionals);

        this.drawHead(this.headStart, startArrowPos, angle + 180, "start", false, additionals, strokeWidth);
        this.drawHead(this.headEnd, endArrowPos, angle, "end", false, additionals, strokeWidth);
    }

    private drawHead(kind: string, at: Point, ang: number, pos: string, circle: boolean, additionals: VNode[], strokeWidth: number) {
        if (kind == 'none') return;

        const d = this.headPath(kind);
        if (d) {
            additionals.push(
                <path d={d}
                      transform={`translate(${at.x} ${at.y}) rotate(${ang})`}
                      style={{ fill: kind === 'COMPOSITION' ? "black" : 'none' }}
                      stroke="black"
                      strokeWidth={strokeWidth}
                />
            );
        }
    }
}
