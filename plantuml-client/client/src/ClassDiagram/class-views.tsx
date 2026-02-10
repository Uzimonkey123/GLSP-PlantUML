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
    PolylineEdgeView,
    RenderingContext,
    svg
} from '@eclipse-glsp/client';
import {VNode} from "snabbdom";
import '../../css/diagram.css';
import {createIcon, renderVisibilityShape} from "../utils";
import {renderLine} from "../SequenceDiagram/sequence-views";
import {CurvedEdgeRenderer} from "./curved-edge-view";

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

            {hasStereotypeName && (
                <text
                    x={visibilityShape ? 15 : 5}
                    y={-12}
                    class-sprotty-label={true}
                    text-anchor="start"
                    style={{
                        fontSize: '11px',
                        fill: '#666'
                    }}
                >
                    {stereotypeName}
                </text>
            )}

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
export class ClassLinkView extends PolylineEdgeView {
    private start = {x: 0, y: 0};
    private end = {x: 0, y: 0};

    private style! : string;
    private headStart! : string;
    private headEnd! : string;
    private arrColor! : string;
    private thickness! : number;

    private context!: RenderingContext;

    public override render(edge: GEdge, context: RenderingContext, args?: IViewArgs): VNode | undefined {
        const sourceMember = (edge.args as any)?.sourceMember;
        const targetMember = (edge.args as any)?.targetMember;

        this.style = (edge.args?.style as string) ?? 'normal';
        this.headStart = (edge.args?.headStart as string) ?? 'none';
        this.headEnd = (edge.args?.headEnd as string) ?? 'none';
        this.thickness = (edge.args?.thickness as number) ?? 1.0;
        this.arrColor = (edge.args?.color as string) ?? '#000000';

        this.context = context;

        // If members specified, use curved rendering
        if (sourceMember || targetMember) {
            return this.renderMemberLink(edge, context, args);
        }

        // Use default rendering (notes are handled in renderMessageLabel)
        return super.render(edge, context, args);
    }

    private renderMemberLink(edge: GEdge, context: RenderingContext, args?: IViewArgs): VNode | undefined {
        const source = edge.source as GNode;
        const target = edge.target as GNode;

        if (!source || !target) {
            return super.render(edge, context, args);
        }

        const sourceMember = (edge.args as any)?.sourceMember;
        const targetMember = (edge.args as any)?.targetMember;

        const additionals: VNode[] = [];

        // Determine curve direction using CurvedEdgeRenderer
        const { flipCurve, curveToRight } = CurvedEdgeRenderer.determineCurveDirection(
            source,
            target,
            sourceMember,
            targetMember
        );

        // Calculate anchor points at member positions with curve direction
        const sourceAnchor = CurvedEdgeRenderer.calculateMemberAnchorPoint(
            source,
            sourceMember,
            curveToRight
        );
        const targetAnchor = CurvedEdgeRenderer.calculateMemberAnchorPoint(
            target,
            targetMember,
            curveToRight
        );

        this.start = sourceAnchor;
        this.end = targetAnchor;

        // Calculate control points and curve data using CurvedEdgeRenderer
        const curveData = CurvedEdgeRenderer.calculateCurveWithTangents(
            sourceAnchor,
            targetAnchor,
            flipCurve
        );

        // Draw curved path
        additionals.push(
            CurvedEdgeRenderer.renderCurvedPath(curveData, {
                style: this.style,
                headStart: this.headStart,
                headEnd: this.headEnd,
                thickness: this.thickness,
                color: this.arrColor
            })
        );

        // Calculate arrow head positions with proper offsets
        const startHeadSize = this.getHeadSize(this.headStart) * this.thickness;
        const endHeadSize = this.getHeadSize(this.headEnd) * this.thickness;

        const arrowTransforms = CurvedEdgeRenderer.calculateArrowTransforms(
            sourceAnchor,
            targetAnchor,
            curveData,
            startHeadSize,
            endHeadSize
        );

        // Draw arrow heads
        this.drawHead(
            this.headStart,
            arrowTransforms.startArrowPos,
            arrowTransforms.startAngle + 180,
            false,
            additionals,
            this.thickness
        );
        this.drawHead(
            this.headEnd,
            arrowTransforms.endArrowPos,
            arrowTransforms.endAngle,
            false,
            additionals,
            this.thickness
        );

        return <g class-sprotty-edge={true}>{additionals}</g>;
    }

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

        let start = segments[0];
        let end = segments[segments.length - 1];

        const sourceQualifier = (edge.args as any)?.sourceQualifier;
        const targetQualifier = (edge.args as any)?.targetQualifier;

        // Get entity bounds to determine edge direction
        const source = edge.source as GNode;
        const target = edge.target as GNode;

        // Render qualifier boxes if present
        if (sourceQualifier && source) {
            const { box, newAnchorPoint } = this.renderQualifierBoxSmart(
                sourceQualifier,
                start,
                source
            );

            additionals.push(box);
            start = newAnchorPoint;
        }

        if (targetQualifier && target) {
            const { box, newAnchorPoint } = this.renderQualifierBoxSmart(
                targetQualifier,
                end,
                target
            );

            additionals.push(box);
            end = newAnchorPoint;
        }

        this.start = start;
        this.end = end;

        this.drawSimpleArrow(additionals);

        return additionals;
    }

    private renderQualifierBoxSmart(
        text: string,
        anchorPoint: Point,
        entity: GNode
    ): { box: VNode; newAnchorPoint: Point } {

        const padding = 4;
        const lineHeight = 14;
        const charWidth = 6.5;
        const gap = 0;

        const boxWidth = text.length * charWidth + padding * 2;
        const boxHeight = lineHeight + padding * 2;

        const entityBounds = {
            left: entity.position.x,
            right: entity.position.x + entity.size.width,
            top: entity.position.y,
            bottom: entity.position.y + entity.size.height
        };

        const distToLeft = Math.abs(anchorPoint.x - entityBounds.left);
        const distToRight = Math.abs(anchorPoint.x - entityBounds.right);
        const distToTop = Math.abs(anchorPoint.y - entityBounds.top);
        const distToBottom = Math.abs(anchorPoint.y - entityBounds.bottom);

        const minDist = Math.min(distToLeft, distToRight, distToTop, distToBottom);

        let boxX: number;
        let boxY: number;
        let newAnchorX: number;
        let newAnchorY: number;

        // TODO: REFACTOR
        if (minDist === distToRight) {
            boxX = anchorPoint.x + gap;
            boxY = anchorPoint.y - boxHeight / 2;
            newAnchorX = boxX + boxWidth;
            newAnchorY = anchorPoint.y;
        } else if (minDist === distToLeft) {
            boxX = anchorPoint.x - boxWidth - gap;
            boxY = anchorPoint.y - boxHeight / 2;
            newAnchorX = boxX;
            newAnchorY = anchorPoint.y;
        } else if (minDist === distToBottom) {
            boxX = anchorPoint.x - boxWidth / 2;
            boxY = anchorPoint.y + gap;
            newAnchorX = anchorPoint.x;
            newAnchorY = boxY + boxHeight;
        } else {
            boxX = anchorPoint.x - boxWidth / 2;
            boxY = anchorPoint.y - boxHeight - gap;
            newAnchorX = anchorPoint.x;
            newAnchorY = boxY;
        }

        const box = (
            <g class-qualifier-box={true}>
                <rect
                    x={boxX}
                    y={boxY}
                    width={boxWidth}
                    height={boxHeight}
                    fill="white"
                    stroke="black"
                    stroke-width="1"
                />
                <text
                    x={boxX + boxWidth / 2}
                    y={boxY + boxHeight / 2}
                    text-anchor="middle"
                    dominant-baseline="middle"
                    font-size="10"
                >
                    {text}
                </text>
            </g>
        );

        return {
            box,
            newAnchorPoint: { x: newAnchorX, y: newAnchorY }
        };
    }

    private drawMessageLine(start: {x: number, y:number}, end: {x: number, y: number}, additionals: VNode[]): renderLine {
        const dx = end.x - start.x;
        const dy = end.y - start.y;
        const length = Math.sqrt(dx * dx + dy * dy);

        const startHeadSize = this.getHeadSize(this.headStart) * this.thickness;
        const endHeadSize = this.getHeadSize(this.headEnd) * this.thickness;

        const strokeWidth = this.thickness;

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
                stroke={this.arrColor}
                stroke-width={this.thickness}
                stroke-dasharray={
                    this.style === "DASHED"
                        ? "5,5"
                        : this.style === "DOTTED"
                            ? "1,5"
                            : "none"
                }
                marker-end="none"
                fill="none"
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

        this.drawHead(this.headStart, startArrowPos, angle + 180, false, additionals, strokeWidth);
        this.drawHead(this.headEnd, endArrowPos, angle, false, additionals, strokeWidth);
    }

    private drawHead(kind: string, at: Point, ang: number, circle: boolean, additionals: VNode[], strokeWidth: number) {
        if (kind == 'none') return;

        const d = this.headPath(kind);
        if (d) {
            const isFilled = kind === 'COMPOSITION';
            const scale = strokeWidth;
            const normalizedStroke = isFilled ? 1 : 1;

            additionals.push(
                <path d={d}
                      transform={`translate(${at.x} ${at.y}) rotate(${ang}) scale(${scale})`}
                      style={{ fill: isFilled ? this.arrColor : 'none' }}
                      stroke={this.arrColor}
                      stroke-width={normalizedStroke}
                />
            );
        }
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