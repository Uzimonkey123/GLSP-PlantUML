import {injectable} from "inversify";
import {GNode, IViewArgs, RenderingContext, ShapeView, svg} from "@eclipse-glsp/client";
import {VNode} from "snabbdom";

/** @jsx svg */

@injectable()
export class EntityView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = "#5d4949";

        const nameLabel = node.children.find(child => child.type === 'label:entityName');
        const fieldLabels = node.children.filter(child => child.type === 'label:field');
        const methodLabels = node.children.filter(child => child.type === 'label:method');
        const bodyLabels = node.children.filter(child => child.type === 'label:body');

        const lineHeight = 14;
        const padding = 5;
        const minSectionHeight = 10;
        const headerH = 30;

        if (this.hasSeparator(bodyLabels)) {
            return this.renderAdvanced(context, w, h, background, nameLabel, bodyLabels, headerH, lineHeight);
        }

        const fieldH = fieldLabels.length > 0
            ? fieldLabels.length * lineHeight + padding * 2
            : minSectionHeight;

        return <g>
            <rect class-sprotty-node={true} x={0} y={0} width={w} height={h} fill={background} stroke="white" stroke-width="2"/>

            <g>
                {nameLabel && (
                    <g transform={`translate(${w/2}, ${headerH/2})`}>
                        {context.renderElement(nameLabel)}
                    </g>
                )}
                <line x1={0} y1={headerH} x2={w} y2={headerH} class-simple-line={true}/>
            </g>

            <g>
                {fieldLabels.map((field, index) => (
                    <g transform={`translate(${w/2}, ${headerH + padding + (index * lineHeight) + 5})`}>
                        {context.renderElement(field)}
                    </g>
                ))}
                <line x1={0} y1={headerH + fieldH} x2={w} y2={headerH + fieldH} class-simple-line={true}/>
            </g>

            <g>
                {methodLabels.map((method, index) => (
                    <g transform={`translate(${w/2}, ${headerH + fieldH + padding + (index * lineHeight) + 5})`}>
                        {context.renderElement(method)}
                    </g>
                ))}
            </g>
        </g>;
    }

    private hasSeparator(bodyLabels: any[]): boolean {
        for (const label of bodyLabels) {
            const text = (label as any).text || '';

            if (/^-{2,}|^[.]{2,}|^={2,}|^_{2,}/.test(text.trim())) {
                return true;
            }
        }

        return false;
    }

    private renderAdvanced(context: RenderingContext, w: number, h: number,
                               background: string, nameLabel: any, bodyLabels: any[],
                               headerH: number, lineHeight: number): VNode {
        let currentY = headerH + 10;
        const elements: VNode[] = [];

        for (const label of bodyLabels) {
            const text = (label as any).text || '';
            const separator = this.renderSeparator(text, currentY, w);

            if (separator) {
                elements.push(separator);
                currentY += lineHeight;

            } else {
                elements.push(
                    <g transform={`translate(${w/2}, ${currentY})`}>
                        {context.renderElement(label)}
                    </g>
                );
                currentY += lineHeight;
            }
        }

        return <g>
            <rect class-sprotty-node={true} x={0} y={0} width={w} height={h} fill={background} stroke="black" stroke-width="2"/>

            <g>
                {nameLabel && (
                    <g transform={`translate(${w/2}, ${headerH/2})`}>
                        {context.renderElement(nameLabel)}
                    </g>
                )}
                <line x1={0} y1={headerH} x2={w} y2={headerH} class-simple-line={true}/>
            </g>

            <g>{elements}</g>
        </g>;
    }

    private renderSeparator(text: string, y: number, w: number): VNode | null {
        const trimmed = text.trim();

        let separator: string | null = null;
        let regex: RegExp | null = null;

        if (/^-{2,}/.test(trimmed)) {
            separator = '-';
            regex = /^-+\s*|\s*-+$/g;

        } else if (/^[.]{2,}/.test(trimmed)) {
            separator = '.';
            regex = /^[.]+\s*|\s*[.]+$/g;

        } else if (/^={2,}/.test(trimmed)) {
            separator = '=';
            regex = /^=+\s*|\s*=+$/g;

        } else if (/^_{2,}/.test(trimmed)) {
            separator = '_';
            regex = /^_+\s*|\s*_+$/g;

        }

        if (!separator || !regex) return null;

        const title = trimmed.replace(regex, "").trim();
        const lineProps = this.getSeparatorLineProps(separator, y);

        if (title) {
            return <g>
                <line x1={0} y1={lineProps.y1} x2={w/2 - 40} y2={lineProps.y1} {...lineProps.attrs}/>

                {/* y2 just in case of "=" separator for double line */}
                {lineProps.y2 !== null &&
                    <line x1={0} y1={lineProps.y2} x2={w/2 - 40} y2={lineProps.y2} {...lineProps.attrs}/>}

                <text x={w/2} y={y + 3} text-anchor="middle" fill="white" font-size="10">{title}</text>
                <line x1={w/2 + 40} y1={lineProps.y1} x2={w} y2={lineProps.y1} {...lineProps.attrs}/>

                {lineProps.y2 !== null &&
                    <line x1={w/2 + 40} y1={lineProps.y2} x2={w} y2={lineProps.y2} {...lineProps.attrs}/>}
            </g>;
        }

        return <g>
            <line x1={0} y1={lineProps.y1} x2={w} y2={lineProps.y1} {...lineProps.attrs}/>
            {lineProps.y2 !== null &&
                <line x1={0} y1={lineProps.y2} x2={w} y2={lineProps.y2} {...lineProps.attrs}/>}
        </g>;
    }

    private getSeparatorLineProps(type: string, y: number) {
        switch (type) {
            case '-':
                return { y1: y, y2: null, attrs: { stroke: "black", "stroke-width": "1" }};
            case '.':
                return { y1: y, y2: null, attrs: { stroke: "black", "stroke-width": "1", "stroke-dasharray": "2,2" }};
            case '=':
                return { y1: y - 2, y2: y + 2, attrs: { stroke: "black", "stroke-width": "1" }};
            case '_':
                return { y1: y, y2: null, attrs: { stroke: "black", "stroke-width": "2" }};
            default:
                return { y1: y, y2: null, attrs: {}};
        }
    }
}

@injectable()
export class DiamondEntityView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = "#FFFFFF";

        const cx = w/2;
        const cy = h/2;

        return <g>
            <polygon
                points={`${cx},0 ${w},${cy} ${cx},${h} 0,${cy}`}
                fill={background}
                stroke="black"
                stroke-width="2"
            />
        </g>
    }
}

@injectable()
export class CircleEntityView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = "#FFFFFF";

        const radius = 15;
        const cx = w / 2;
        const cy = radius + 5;

        return <g>
            <circle
                cx={cx}
                cy={cy}
                r={radius}
                fill={background}
                stroke="black"
                stroke-width="2"
            />

            <g transform={`translate(${cx}, ${cy + radius + 15})`}>
                {context.renderChildren(node)}
            </g>
        </g>;
    }
}

@injectable()
export class AssociationPointView extends ShapeView {
    override render(node: GNode, context: RenderingContext): VNode | undefined {
        if (!this.isVisible(node, context)) {
            return undefined;
        }

        return <g class-sprotty-node={true} class-association-point={true}>
            <circle
                cx={4}
                cy={4}
                r={4}
                fill="#000"
                stroke="none"
            />
        </g>;
    }
}