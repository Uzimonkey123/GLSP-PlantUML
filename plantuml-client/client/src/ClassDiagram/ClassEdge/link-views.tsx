import {injectable} from 'inversify';
import {
    GEdge, GNode, IViewArgs,
    Point, PolylineEdgeView, RenderingContext,
    svg
} from '@eclipse-glsp/client';
import {VNode} from 'snabbdom';
import {
    ClassEdgeArgs, EDGE_CONFIG, getEdgeArgs,
    isDiamondEntity, isMemberLink, isParallelLink,
    isSelfLoop, LinkRenderContext
} from './types';
import {CurvedEdgeRenderer} from "./curved-edge-view";
import {
    AnchorCalculator, ArrowHeadRenderer, getStrokeDashArray
} from '../utils';

/** @jsx svg */

const edgeControlPointOverrides = new Map<string, Point>();

function drawLine(start: Point, end: Point, args: ClassEdgeArgs): {path: VNode; result: {startPos: Point; endPos: Point; angle: number}} {
    const dx = end.x - start.x;
    const dy = end.y - start.y;
    const length = Math.sqrt(dx * dx + dy * dy) || 1;

    const unitX = dx / length;
    const unitY = dy / length;

    const startHeadSize = ArrowHeadRenderer.getSize(args.headStart) * args.thickness;
    const endHeadSize = ArrowHeadRenderer.getSize(args.headEnd) * args.thickness;

    const lineStart = {x: start.x + unitX * startHeadSize, y: start.y + unitY * startHeadSize};
    const lineEnd = {x: end.x - unitX * endHeadSize, y: end.y - unitY * endHeadSize};

    const path = (
        <path
            d={`M ${lineStart.x} ${lineStart.y} L ${lineEnd.x} ${lineEnd.y}`}
            stroke={args.color}
            stroke-width={args.thickness}
            stroke-dasharray={getStrokeDashArray(args.style)}
            fill="none"
        />
    );

    return {
        path,
        result: {
            startPos: lineStart,
            endPos: lineEnd,
            angle: ArrowHeadRenderer.getAngle(start, end)
        }
    };
}

function drawSimpleArrow(start: Point, end: Point, args: ClassEdgeArgs): VNode[] {
    const {path, result} = drawLine(start, end, args);
    const vnodes: VNode[] = [path];

    const startHead = ArrowHeadRenderer.render(args.headStart, result.startPos, result.angle + 180, args.color, args.thickness);
    const endHead = ArrowHeadRenderer.render(args.headEnd, result.endPos, result.angle, args.color, args.thickness);

    if (startHead) vnodes.push(startHead);
    if (endHead) vnodes.push(endHead);

    return vnodes;
}

function wrapInEdgeGroup(children: VNode[]): VNode {
    return <g class-sprotty-edge={true}>
        {children}
    </g>;
}

function renderSelfLoop(ctx: LinkRenderContext): VNode {
    const {edge, source, args} = ctx;
    const additionals: VNode[] = [];

    const x = source.position.x;
    const y = source.position.y;
    const width = source.size.width;
    const height = source.size.height;

    const index = args.selfLoopIndex ?? 0;
    const total = args.selfLoopTotal ?? 1;

    const messageLabel = edge.children?.find(child => child.type === 'label:link') as any;
    const messageText = messageLabel?.text ?? '';
    const charWidth = 7;
    const labelPadding = 12;
    const labelWidth = messageText.length > 0
        ? messageText.length * charWidth + labelPadding
        : 40;

    const baseBulge = 25;
    const bulgeSpacing = labelWidth + 8;
    const bulge = baseBulge + (total - 1 - index) * bulgeSpacing;

    const baseSpread = Math.min(height * 0.15, 12);
    const spreadStep = 8;
    const spread = baseSpread + (total - 1 - index) * spreadStep;

    const centerY = y + height / 2;
    const anchorY1 = args.sourceMember
        ? y + CurvedEdgeRenderer.getMemberYOffset(source, args.sourceMember)
        : centerY - spread;
    const anchorY2 = args.targetMember
        ? y + CurvedEdgeRenderer.getMemberYOffset(source, args.targetMember)
        : centerY + spread;
    const anchorX = x + width;

    const start: Point = {x: anchorX, y: anchorY1};
    const end: Point = {x: anchorX, y: anchorY2};

    const controlPoint1: Point = {x: anchorX + bulge, y: anchorY1};
    const controlPoint2: Point = {x: anchorX + bulge, y: anchorY2};

    const startTangentX = controlPoint1.x - start.x;
    const startTangentY = controlPoint1.y - start.y;
    const startTangentLen = Math.hypot(startTangentX, startTangentY) || 1;
    const startAngle = Math.atan2(startTangentY, startTangentX) * 180 / Math.PI;

    const endTangentX = end.x - controlPoint2.x;
    const endTangentY = end.y - controlPoint2.y;
    const endTangentLen = Math.hypot(endTangentX, endTangentY) || 1;
    const endAngle = Math.atan2(endTangentY, endTangentX) * 180 / Math.PI;

    const startHeadSize = ArrowHeadRenderer.getSize(args.headStart) * args.thickness;
    const endHeadSize = ArrowHeadRenderer.getSize(args.headEnd) * args.thickness;

    const pathStart = {
        x: start.x + (startTangentX / startTangentLen) * startHeadSize,
        y: start.y + (startTangentY / startTangentLen) * startHeadSize
    };

    const pathEnd = {
        x: end.x - (endTangentX / endTangentLen) * endHeadSize,
        y: end.y - (endTangentY / endTangentLen) * endHeadSize
    };

    additionals.push(
        <path
            d={`M ${pathStart.x},${pathStart.y} C ${controlPoint1.x},${controlPoint1.y} ${controlPoint2.x},${controlPoint2.y} ${pathEnd.x},${pathEnd.y}`}
            stroke={args.color}
            stroke-width={args.thickness}
            stroke-dasharray={getStrokeDashArray(args.style)}
            fill="none"
            class-sprotty-edge={true}
        />
    );

    const startHead = ArrowHeadRenderer.render(args.headStart, pathStart, startAngle + 180, args.color, args.thickness);
    const endHead = ArrowHeadRenderer.render(args.headEnd, pathEnd, endAngle, args.color, args.thickness);

    if (startHead) additionals.push(startHead);
    if (endHead) additionals.push(endHead);

    return wrapInEdgeGroup(additionals);
}

function renderMemberLink(ctx: LinkRenderContext): VNode {
    const {source, target, args} = ctx;
    const additionals: VNode[] = [];
    const {sourceAnchor, targetAnchor, needsCurve, flipCurve} =
        CurvedEdgeRenderer.determineMemberLinkStyle(source, target, args.sourceMember, args.targetMember);

    if (needsCurve) {
        const curveData = CurvedEdgeRenderer.calculateCurveWithTangents(sourceAnchor, targetAnchor, flipCurve);
        additionals.push(CurvedEdgeRenderer.renderCurvedPath(curveData, {
            style: args.style,
            headStart: args.headStart,
            headEnd: args.headEnd,
            thickness: args.thickness,
            color: args.color
        }))

        const startHeadSize = ArrowHeadRenderer.getSize(args.headStart) * args.thickness;
        const endHeadSize = ArrowHeadRenderer.getSize(args.headEnd) * args.thickness;

        const startPos = {
            x: sourceAnchor.x + curveData.startTangent.x * startHeadSize,
            y: sourceAnchor.y + curveData.startTangent.y * startHeadSize
        };

        const endPos = {
            x: targetAnchor.x - curveData.endTangent.x * endHeadSize,
            y: targetAnchor.y - curveData.endTangent.y * endHeadSize
        };

        const startAngle = Math.atan2(curveData.startTangent.y, curveData.startTangent.x) * 180 / Math.PI;
        const endAngle = Math.atan2(curveData.endTangent.y, curveData.endTangent.x) * 180 / Math.PI;

        const startHead = ArrowHeadRenderer.render(args.headStart, startPos, startAngle + 180, args.color, args.thickness);
        const endHead = ArrowHeadRenderer.render(args.headEnd, endPos, endAngle, args.color, args.thickness);

        if (startHead) additionals.push(startHead);
        if (endHead) additionals.push(endHead);

    } else {
        additionals.push(...drawSimpleArrow(sourceAnchor, targetAnchor, args));
    }

    return wrapInEdgeGroup(additionals);
}

function renderParallelLink(ctx: LinkRenderContext): VNode {
    const {edge, source, target, args} = ctx;
    const additionals: VNode[] = [];

    const index = args.parallelIndex!;
    const totalCount = args.parallelTotal!;
    const {sourceAnchor, targetAnchor, offset} = AnchorCalculator.getParallelAnchors(source, target, index, totalCount);
    const flipCurve = offset < 0;
    const isOuter = totalCount > 2 && (index === 0 || index === totalCount - 1);

    if (edgeControlPointOverrides.has(edge.id)) {
        const controlPoint = edgeControlPointOverrides.get(edge.id)!;
        renderBezier(sourceAnchor, targetAnchor, controlPoint, args, additionals);

        if (edge.selected) additionals.push(renderHandle(edge.id, controlPoint));

        return wrapInEdgeGroup(additionals);
    }

    if (isOuter) {
        renderCurvedParallel(sourceAnchor, targetAnchor, flipCurve, args, additionals);

        if (edge.selected) {
            const peak = getAutoCurvePeak(sourceAnchor, targetAnchor, flipCurve);
            additionals.push(renderHandle(edge.id, peak));
        }

    } else {
        additionals.push(...drawSimpleArrow(sourceAnchor, targetAnchor, args));

        if (edge.selected) {
            const midpoint = {x: (sourceAnchor.x + targetAnchor.x) / 2, y: (sourceAnchor.y + targetAnchor.y) / 2};
            additionals.push(renderHandle(edge.id, midpoint));
        }
    }

    return wrapInEdgeGroup(additionals);
}

function renderCurvedParallel(sourceAnchor: Point, targetAnchor: Point, flipCurve: boolean, args: ClassEdgeArgs, additionals: VNode[]) {
    const curveData = CurvedEdgeRenderer.calculateCurveWithTangents(sourceAnchor, targetAnchor, flipCurve);
    const startHeadSize = ArrowHeadRenderer.getSize(args.headStart) * args.thickness;
    const endHeadSize = ArrowHeadRenderer.getSize(args.headEnd) * args.thickness;

    const pathStart = {
        x: sourceAnchor.x + curveData.startTangent.x * startHeadSize,
        y: sourceAnchor.y + curveData.startTangent.y * startHeadSize
    };

    const pathEnd = {
        x: targetAnchor.x - curveData.endTangent.x * endHeadSize,
        y: targetAnchor.y - curveData.endTangent.y * endHeadSize
    };

    additionals.push(CurvedEdgeRenderer.renderCurvedPath(curveData, {
        style: args.style,
        headStart: args.headStart,
        headEnd: args.headEnd,
        thickness: args.thickness,
        color: args.color
    }))

    const startAngle = Math.atan2(curveData.startTangent.y, curveData.startTangent.x) * 180 / Math.PI;
    const endAngle = Math.atan2(curveData.endTangent.y, curveData.endTangent.x) * 180 / Math.PI;

    const startHead = ArrowHeadRenderer.render(args.headStart, pathStart, startAngle + 180, args.color, args.thickness);
    const endHead = ArrowHeadRenderer.render(args.headEnd, pathEnd, endAngle, args.color, args.thickness);

    if (startHead) additionals.push(startHead);
    if (endHead) additionals.push(endHead);
}

function renderBezier(sourceAnchor: Point, targetAnchor: Point, controlPoint: Point, args: ClassEdgeArgs, additionals: VNode[]) {
    const startToControlX = controlPoint.x - sourceAnchor.x;
    const startToControlY = controlPoint.y - sourceAnchor.y;
    const startToControlLen = Math.sqrt(startToControlX * startToControlX + startToControlY * startToControlY) || 1;

    const controlToEndX = targetAnchor.x - controlPoint.x;
    const controlToEndY = targetAnchor.y - controlPoint.y;
    const controlToEndLen = Math.sqrt(controlToEndX * controlToEndX + controlToEndY * controlToEndY) || 1;

    const startHeadSize = ArrowHeadRenderer.getSize(args.headStart) * args.thickness;
    const endHeadSize = ArrowHeadRenderer.getSize(args.headEnd) * args.thickness;

    const pathStart = {
        x: sourceAnchor.x + (startToControlX / startToControlLen) * startHeadSize,
        y: sourceAnchor.y + (startToControlY / startToControlLen) * startHeadSize
    };

    const pathEnd = {
        x: targetAnchor.x - (controlToEndX / controlToEndLen) * endHeadSize,
        y: targetAnchor.y - (controlToEndY / controlToEndLen) * endHeadSize
    };

    additionals.push(
        <path
            d={`M ${pathStart.x},${pathStart.y} Q ${controlPoint.x},${controlPoint.y} ${pathEnd.x},${pathEnd.y}`}
            stroke={args.color}
            stroke-width={args.thickness}
            stroke-dasharray={getStrokeDashArray(args.style)}
            fill="none"
            class-sprotty-edge={true}
        />
    );

    const startAngle = Math.atan2(startToControlY, startToControlX) * 180 / Math.PI;
    const endAngle = Math.atan2(controlToEndY, controlToEndX) * 180 / Math.PI;

    const startHead = ArrowHeadRenderer.render(args.headStart, pathStart, startAngle + 180, args.color, args.thickness);
    const endHead = ArrowHeadRenderer.render(args.headEnd, pathEnd, endAngle, args.color, args.thickness);

    if (startHead) additionals.push(startHead);
    if (endHead) additionals.push(endHead);
}

function getAutoCurvePeak(source: Point, target: Point, flipCurve: boolean): Point {
    const midX = (source.x + target.x) / 2;
    const midY = (source.y + target.y) / 2;

    const dx = target.x - source.x;
    const dy = target.y - source.y;

    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
    const sign = flipCurve ? -1 : 1;

    return {
        x: midX + sign * (-dy / dist) * dist * EDGE_CONFIG.curve.offsetRatio,
        y: midY + sign * (dx / dist) * dist * EDGE_CONFIG.curve.offsetRatio
    };
}

function renderHandle(edgeId: string, position: Point): VNode {
    const {radius, hitRadius} = EDGE_CONFIG.handle;

    return (
        <g class-edge-drag-handle={true} data-edge-id={edgeId}>
            <circle cx={position.x} cy={position.y} r={hitRadius} fill="transparent" stroke="none" />
            <circle cx={position.x} cy={position.y} r={radius} fill="#ffffff" stroke="#4A90D9" stroke-width="2" />
        </g>
    );
}

function renderDiamondLink(ctx: LinkRenderContext): VNode {
    const {source, target, args} = ctx;

    const sourceCenterX = source.position.x + source.size.width / 2;
    const sourceCenterY = source.position.y + source.size.height / 2;

    const targetCenterX = target.position.x + target.size.width / 2;
    const targetCenterY = target.position.y + target.size.height / 2;

    const sourceAnchor = isDiamondEntity(source)
        ? AnchorCalculator.getClosestDiamondTip(source, targetCenterX, targetCenterY)
        : AnchorCalculator.getBoundaryPoint(source, targetCenterX, targetCenterY);

    const targetAnchor = isDiamondEntity(target)
        ? AnchorCalculator.getClosestDiamondTip(target, sourceCenterX, sourceCenterY)
        : AnchorCalculator.getBoundaryPoint(target, sourceCenterX, sourceCenterY);

    return wrapInEdgeGroup(drawSimpleArrow(sourceAnchor, targetAnchor, args));
}

function renderSimpleLink(ctx: LinkRenderContext): VNode {
    const {source, target, args} = ctx;
    const additionals: VNode[] = [];
    const sourceCenterX = source.position.x + source.size.width / 2;
    const sourceCenterY = source.position.y + source.size.height / 2;
    const targetCenterX = target.position.x + target.size.width / 2;
    const targetCenterY = target.position.y + target.size.height / 2;

    let start = AnchorCalculator.getBoundaryPoint(source, targetCenterX, targetCenterY);
    let end = AnchorCalculator.getBoundaryPoint(target, sourceCenterX, sourceCenterY);

    if (args.sourceQualifier) {
        const {box, anchor} = renderQualifierBox(args.sourceQualifier, start, source);
        additionals.push(box);
        start = anchor;
    }

    if (args.targetQualifier) {
        const {box, anchor} = renderQualifierBox(args.targetQualifier, end, target);
        additionals.push(box);
        end = anchor;
    }

    additionals.push(...drawSimpleArrow(start, end, args));
    return wrapInEdgeGroup(additionals);
}

function renderQualifierBox(text: string, anchorPoint: Point, entity: GNode): {box: VNode; anchor: Point} {
    const {padding, lineHeight, charWidth, gap} = EDGE_CONFIG.qualifier;
    const boxWidth = text.length * charWidth + padding * 2;
    const boxHeight = lineHeight + padding * 2;

    const bounds = {
        left: entity.position.x,
        right: entity.position.x + entity.size.width,
        top: entity.position.y,
        bottom: entity.position.y + entity.size.height
    };

    const distLeft = Math.abs(anchorPoint.x - bounds.left);
    const distRight = Math.abs(anchorPoint.x - bounds.right);
    const distTop = Math.abs(anchorPoint.y - bounds.top);
    const distBottom = Math.abs(anchorPoint.y - bounds.bottom);
    const minDist = Math.min(distLeft, distRight, distTop, distBottom);

    let boxX: number;
    let boxY: number;
    let newAnchorX: number;
    let newAnchorY: number;

    if (minDist === distRight) {
        boxX = anchorPoint.x + gap;
        boxY = anchorPoint.y - boxHeight / 2;
        newAnchorX = boxX + boxWidth;
        newAnchorY = anchorPoint.y;

    } else if (minDist === distLeft) {
        boxX = anchorPoint.x - boxWidth - gap;
        boxY = anchorPoint.y - boxHeight / 2;
        newAnchorX = boxX;
        newAnchorY = anchorPoint.y;

    } else if (minDist === distBottom) {
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
                stroke-width="1" />
        </g>
    );

    return {box, anchor: {x: newAnchorX, y: newAnchorY}};
}

@injectable()
export class ClassLinkView extends PolylineEdgeView {
    override render(edge: GEdge, context: RenderingContext, args?: IViewArgs): VNode | undefined {
        const source = edge.source as GNode;
        const target = edge.target as GNode;

        if (!source || !target) {
            return super.render(edge, context, args);
        }

        const edgeArgs = getEdgeArgs(edge);
        const ctx: LinkRenderContext = {edge, source, target, args: edgeArgs, renderingContext: context, viewArgs: args};

        if (isSelfLoop(edge)) return renderSelfLoop(ctx);
        if (isMemberLink(edgeArgs)) return renderMemberLink(ctx);
        if (isParallelLink(edgeArgs)) return renderParallelLink(ctx);
        if (isDiamondEntity(source) || isDiamondEntity(target)) return renderDiamondLink(ctx);

        return renderSimpleLink(ctx);
    }

    protected override renderAdditionals(): VNode[] {
        return [];
    }
}
