import {injectable} from "inversify";
import {GNode, IViewArgs, RenderingContext, ShapeView, svg} from "@eclipse-glsp/client";
import {VNode} from "snabbdom";

/** @jsx svg */

@injectable()
export class RectangularNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext,
        args?: IViewArgs
    ): VNode {
        const w = node.size.width;
        const totalH = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 30;
        const footerH = 30;

        // Lifeline between header and footer
        const lifeLineStart = headerH;
        const lifeLineEnd = totalH - footerH;

        return <g>
            {/* Top rectangle */}
            <g>
                <rect class-sprotty-node={true} x={0} y={0} width={w} height={headerH} fill={background}/>
                <g transform={`translate(${w/2},${headerH/2})`}>
                    {context.renderChildren(node)}
                </g>
            </g>

            {/* Dashed lifeline, auto‐size */}
            <line
                x1={w/2}
                y1={lifeLineStart}
                x2={w/2}
                y2={lifeLineEnd}
                stroke="black"
                stroke-dasharray="4 2"
            />

            {/* Bottom rectangle */}
            {showFoot && (
                <g transform={`translate(0, ${lifeLineEnd})`}>
                    <rect class-sprotty-node={true} x={0} y={0} width={w} height={footerH} fill={background}/>
                    <g transform={`translate(${w/2},${footerH/2})`}>
                        {context.renderChildren(node)}
                    </g>
                </g>
            )}
        </g>;
    }
}

@injectable()
export class ActorNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 15;
        const footerH = 15;

        const cx = w / 2; // Center of circle
        const headRadius = 6;

        const bodyStartY = headRadius * 2;
        const bodyEndY = bodyStartY + 20;

        const drawStickman = (offsetY: number) => (
            <g transform={`translate(0, ${offsetY})`}>
                {/* Head */}
                <circle cx={cx} cy={headRadius} r={headRadius} stroke="black" fill={background} />
                {/* Body */}
                <line x1={cx} y1={bodyStartY} x2={cx} y2={bodyEndY} stroke="black" />
                {/* Arms */}
                <line x1={cx - 15} y1={bodyStartY + 5} x2={cx + 15} y2={bodyStartY + 5} stroke="black" />
                {/* Legs */}
                <line x1={cx} y1={bodyEndY} x2={cx - 15} y2={bodyEndY + 15} stroke="black" />
                <line x1={cx} y1={bodyEndY} x2={cx + 15} y2={bodyEndY + 15} stroke="black" />
            </g>
        );

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH + 5;
        const lifeLineEnd = h - footerH - 5;

        // Position for the label
        const labelY = headerH;

        return (
            <g>
                {/* Top stickman */}
                {drawStickman(labelY - 65)}

                {/* Top label */}
                <g transform={`translate(${cx}, ${labelY})`}>
                    {context.renderChildren(node)}
                </g>

                {/* Lifeline */}
                <line
                    x1={cx}
                    y1={lifeLineStart}
                    x2={cx}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/* Bottom label */}
                        <g transform={`translate(${cx}, ${lifeLineEnd + 5})`}>
                            {context.renderChildren(node)}
                        </g>

                        {/* Bottom stickman */}
                        {drawStickman(lifeLineEnd + 10)}
                    </g>
                )}
            </g>
        );
    }
}

@injectable()
export class BoundaryNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 15;
        const footerH = 15;

        const cx = w / 2; // Center of circle

        const drawBoundary = (offsetY: number) => {
            const verticalLine = 12;
            const horizontalLine = 20;
            const circleRadius = 6;

            return (
                <g>
                    {/* Vertical line */}
                    <line
                        x1={cx - horizontalLine}
                        y1={offsetY - verticalLine / 2}
                        x2={cx - horizontalLine}
                        y2={offsetY + verticalLine / 2}
                        stroke="black"
                    />

                    {/* Horizontal line  */}
                    <line
                        x1={cx - horizontalLine}
                        y1={offsetY}
                        x2={cx}
                        y2={offsetY}
                        stroke="black"
                    />

                    {/* Circle */}
                    <circle
                        cx={cx}
                        cy={offsetY}
                        r={circleRadius}
                        stroke="black"
                        fill={background}
                    />
                </g>
            );
        };

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH + 5;
        const lifeLineEnd = h - footerH - 5;

        // Position for the label
        const labelY = headerH;

        return (
            <g>
                {/* Top boundary */}
                {drawBoundary(labelY - 20)}

                {/* Top label */}
                <g transform={`translate(${cx}, ${labelY})`}>
                    {context.renderChildren(node)}
                </g>

                {/* Lifeline */}
                <line
                    x1={cx}
                    y1={lifeLineStart}
                    x2={cx}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/* Bottom label */}
                        <g transform={`translate(${cx}, ${lifeLineEnd + 5})`}>
                            {context.renderChildren(node)}
                        </g>

                        {/* Bottom boundary */}
                        {drawBoundary(lifeLineEnd + 20)}
                    </g>
                )}
            </g>
        );
    }
}

@injectable()
export class ControlNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 15;
        const footerH = 15;

        const cx = w / 2; // Center of circle

        const drawControl = (offsetY: number) => {
            const circleRadius = 6;

            // Arrow dimensions
            const arrowLength = 8;
            const arrowWidth = 6;

            // Arrow tip position towards left
            const arrowTipX = cx- 6;
            const arrowTipY = offsetY - circleRadius;

            return (
                <g>
                    {/* Circle */}
                    <circle
                        cx={cx}
                        cy={offsetY}
                        r={circleRadius}
                        stroke="black"
                        fill={background}
                    />

                    {/* Arrowhead */}
                    <polygon
                        points={`
                            ${arrowTipX},${arrowTipY}
                            ${arrowTipX + arrowLength},${arrowTipY - arrowWidth / 2}
                            ${arrowTipX + arrowLength},${arrowTipY + arrowWidth / 2}
                        `}
                        fill="black"
                    />
                </g>
            );
        };

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH + 5;
        const lifeLineEnd = h - footerH - 5;

        // Position for the label
        const labelY = headerH;

        return (
            <g>
                {/* Top control */}
                {drawControl(labelY - 20)}

                {/* Top label */}
                <g transform={`translate(${cx}, ${labelY})`}>
                    {context.renderChildren(node)}
                </g>

                {/* Lifeline */}
                <line
                    x1={cx}
                    y1={lifeLineStart}
                    x2={cx}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/* Bottom label */}
                        <g transform={`translate(${cx}, ${lifeLineEnd + 5})`}>
                            {context.renderChildren(node)}
                        </g>

                        {/* Bottom control */}
                        {drawControl(lifeLineEnd + 20)}
                    </g>
                )}
            </g>
        );
    }
}

@injectable()
export class EntityNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 15;
        const footerH = 15;

        const cx = w / 2;

        const drawEntitySymbol = (offsetY: number) => {
            const circleRadius = 6;
            const lineLength = 16;

            return (
                <g>
                    {/* Circle */}
                    <circle
                        cx={cx}
                        cy={offsetY}
                        r={circleRadius}
                        stroke="black"
                        fill={background}
                    />

                    {/* Line under the circle */}
                    <line
                        x1={cx - lineLength / 2}
                        y1={offsetY + 7}
                        x2={cx + lineLength / 2}
                        y2={offsetY + 7}
                        stroke="black"
                    />
                </g>
            );
        };

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH + 5;
        const lifeLineEnd = h - footerH - 5;

        // Position for the label
        const labelY = headerH;

        return (
            <g>
                {/* Top entity */}
                {drawEntitySymbol(labelY - 20)}

                {/* Top label */}
                <g transform={`translate(${cx}, ${labelY})`}>
                    {context.renderChildren(node)}
                </g>

                {/* Lifeline */}
                <line
                    x1={cx}
                    y1={lifeLineStart}
                    x2={cx}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/* Bottom label */}
                        <g transform={`translate(${cx}, ${lifeLineEnd + 5})`}>
                            {context.renderChildren(node)}
                        </g>

                        {/* Bottom entity */}
                        {drawEntitySymbol(lifeLineEnd + 20)}
                    </g>
                )}
            </g>
        );
    }
}

@injectable()
export class DatabaseNodeView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 15;
        const footerH = 15;

        const cx = w / 2;

        const drawDatabase = (offsetY: number) => {
            const width = 30;
            const height = 40;
            const rx = width / 2;
            const ry = 6;

            const x = cx - rx;

            return (
                <g>
                    {/* Rectangle without borders */}
                    <rect
                        x={x}
                        y={offsetY}
                        width={width}
                        height={height - 6}
                        fill={background}
                        stroke="none"
                        strokeWidth="1"
                        rx={rx}
                        ry="0"
                    />

                    {/* Left border of the rectangle */}
                    <line
                        x1={x}
                        y1={offsetY}
                        x2={x}
                        y2={offsetY + height - 6}
                        stroke="black"
                        strokeWidth="1"
                    />

                    {/* Right border of the rectangle */}
                    <line
                        x1={x + width}
                        y1={offsetY}
                        x2={x + width}
                        y2={offsetY + height - 6}
                        stroke="black"
                        strokeWidth="1"
                    />

                    {/* Top ellipse */}
                    <ellipse
                        cx={cx}
                        cy={offsetY}
                        rx={rx}
                        ry={ry}
                        stroke="black"
                        fill={background}
                    />

                    {/* Half bottom ellipse */}
                    <path
                        d={`
                            M ${cx - rx} ${offsetY + height - ry}
                            A ${rx} ${ry} 0 0 0 ${cx + rx} ${offsetY + height - ry}
                        `}
                        stroke="black"
                        fill={background}
                    />
                </g>
            );
        };

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH + 5;
        const lifeLineEnd = h - footerH - 5;

        // Label position
        const labelY = headerH;

        return (
            <g>
                {/* Top database */}
                {drawDatabase(labelY - 50)}

                {/* Top label */}
                <g transform={`translate(${cx}, ${labelY})`}>
                    {context.renderChildren(node)}
                </g>

                {/* Lifeline */}
                <line
                    x1={cx}
                    y1={lifeLineStart}
                    x2={cx}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/* Bottom label */}
                        <g transform={`translate(${cx}, ${lifeLineEnd + 5})`}>
                            {context.renderChildren(node)}
                        </g>

                        {/* Bottom database */}
                        {drawDatabase(lifeLineEnd + 25)}
                    </g>
                )}
            </g>
        );
    }
}

@injectable()
export class CollectionNodeView extends ShapeView {
    override render(node: Readonly<GNode>, context: RenderingContext): VNode {
        const w = node.size.width;
        const totalH = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 30;
        const footerH = 30;

        const lifeLineStart = headerH;
        const lifeLineEnd = totalH - footerH;

        return <g>
            {/* Back rectangle */}
            <rect
                x={6}
                y={-6}
                width={w}
                height={headerH}
                fill={background}
                class-sprotty-node={true}
            />

            {/* Front rectangle */}
            <g>
                <rect class-sprotty-node={true} x={0} y={0} width={w} height={headerH} fill={background}/>
                <g transform={`translate(${w / 2}, ${headerH / 2})`}>
                    {context.renderChildren(node)}
                </g>
            </g>

            {/* Lifeline */}
            <line
                x1={w / 2}
                y1={lifeLineStart}
                x2={w / 2}
                y2={lifeLineEnd}
                stroke="black"
                stroke-dasharray="4 2"
            />

            {showFoot && (
                <g>
                    {/* Back of bottom rectangle */}
                    <rect
                        x={6}
                        y={lifeLineEnd}
                        width={w}
                        height={headerH}
                        fill={background}
                        class-sprotty-node={true}
                    />

                    {/* Bottom rectangle */}
                    <g transform={`translate(0, ${lifeLineEnd + 6})`}>
                        <rect class-sprotty-node={true} x={0} y={0} width={w} height={footerH} fill={background}/>
                        <g transform={`translate(${w / 2}, ${footerH / 2})`}>
                            {context.renderChildren(node)}
                        </g>
                    </g>
                </g>
            )}
        </g>;
    }
}

@injectable()
export class QueueNodeView extends ShapeView {
    override render(node: Readonly<GNode>, context: RenderingContext): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = (node as any).args?.background;
        const showFoot = (node as any).args?.showFoot;

        const headerH = 30;
        const footerH = 30;

        const drawQueue = (offsetY: number) => {
            const cy = offsetY;
            const rx = 6; // Radius for the side ellipses
            const ry = 7;

            const cylinderBodyX = 3;
            const cylinderBodyWidth = w - 2 * rx;

            return (
                <g>
                    {/* Left half ellipse */}
                    <path
                        d={`
                            M ${3} ${cy - ry}
                            A ${rx} ${ry} 0 0 0 ${3} ${cy + ry}
                        `}
                        fill={background}
                        stroke="black"
                    />

                    {/* Rectangle without borders */}
                    <rect
                        x={cylinderBodyX}
                        y={cy - ry}
                        width={cylinderBodyWidth + 5}
                        height={2 * ry}
                        fill={background}
                        stroke="none"
                    />

                    {/* Top line of the rectangle*/}
                    <line
                        x1={cylinderBodyX}
                        y1={cy - ry}
                        x2={cylinderBodyWidth + 5}
                        y2={cy - ry}
                        stroke="black"
                    />

                    {/* Bottom line of the rectangle*/}
                    <line
                        x1={cylinderBodyX}
                        y1={cy + ry}
                        x2={cylinderBodyWidth + 5}
                        y2={cy + ry}
                        stroke="black"
                    />

                    {/* Right ellipse */}
                    <ellipse
                        cx={w - rx}
                        cy={cy}
                        rx={rx}
                        ry={ry}
                        fill={background}
                        stroke="black"
                    />

                    {/* Centered label */}
                    <g transform={`translate(${w / 2}, ${cy})`}>
                        {context.renderChildren(node)}
                    </g>
                </g>
            );
        }

        // Lifeline line coordinates to be between the two labels
        const lifeLineStart = headerH - 8;
        const lifeLineEnd = h - footerH + 15;

        return (
            <g>
                {/* Top queue symbol */}
                {drawQueue(15)}

                {/* Dashed lifeline, auto‐size */}
                <line
                    x1={w/2}
                    y1={lifeLineStart}
                    x2={w/2}
                    y2={lifeLineEnd}
                    stroke="black"
                    stroke-dasharray="4 2"
                />

                {showFoot && (
                    <g>
                        {/*Bottom queue symbol */}
                        {drawQueue(lifeLineEnd + 5)}
                    </g>
                )}
            </g>
        );
    }
}

export class LifeEventBar extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {

        return (
            <g>
                <rect
                    x={0}
                    y={0}
                    width={node.size.width}
                    height={node.size.height}
                    fill={(node as any).args?.background}
                    stroke="black"
                    stroke-width={1}
                />
            </g>
        );
    }
}

export class DestroyCross extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const size = 10;
        const half = size / 2;

        return (
            <g>
                <line x1={-half} y1={-half} x2={half} y2={half}
                      stroke="red" stroke-width="2" />
                <line x1={-half} y1={half} x2={half} y2={-half}
                      stroke="red" stroke-width="2" />
            </g>
        );
    }
}