import {injectable} from "inversify";
import {GNode, IViewArgs, RenderingContext, ShapeView, svg} from "@eclipse-glsp/client";
import {VNode} from "snabbdom";
import {getTypeConfig} from "./class-views";
import {TspanConverter} from "../utils";

/** @jsx svg */

@injectable()
export class EntityView extends ShapeView {
    override render(
        node: Readonly<GNode>,
        context: RenderingContext
    ): VNode {
        const w = node.size.width;
        const h = node.size.height;
        const background = "#C0C0C0";

        const nameLabel = node.children.find(child => child.type === 'label:entityName');
        const genericNameLabel = node.children.find(child => child.type === 'label:generic');
        const fieldLabels = node.children.filter(child => child.type === 'label:field');
        const methodLabels = node.children.filter(child => child.type === 'label:method');
        const bodyLabels = node.children.filter(child => child.type === 'label:body');

        const lineHeight = 14;
        const padding = 5;
        const minSectionHeight = 10;
        const hasStereotype = nameLabel && (nameLabel as any).args?.stereotypeName &&
            (nameLabel as any).args.stereotypeName.length > 0;
        const headerH = hasStereotype ? 44 : 30;

        if (this.hasSeparator(bodyLabels)) {
            return this.renderAdvanced(context, w, h, background, nameLabel, bodyLabels, headerH, lineHeight);
        }

        const fieldH = fieldLabels.length > 0
            ? fieldLabels.length * lineHeight + padding * 2
            : minSectionHeight;

        const genericText = genericNameLabel ? ((genericNameLabel as any).text || '') : '';
        const charWidth = 7;
        const boxPadding = 10;
        const genericBoxW = genericNameLabel ? (genericText.length * charWidth + boxPadding) : 50;
        const genericBoxH = 20;
        const genericBoxY = 0;
        const genericBoxX = w - genericBoxW;
        const nameLabelX = genericNameLabel ? genericBoxX / 2 : w / 2;

        let tipOffsetY = 0;
        const tipGap = 15;

        return <g>
            <rect class-sprotty-node={true} x={0} y={0} width={w} height={h} fill={background} stroke="white" stroke-width="2"/>

            <g>
                {nameLabel && (
                    <g transform={`translate(${nameLabelX}, ${headerH/2})`}>
                        {context.renderElement(nameLabel)}
                    </g>
                )}
                <line x1={0} y1={headerH} x2={w} y2={headerH} class-simple-line={true}/>
            </g>

            {genericNameLabel && nameLabel && (
                <g transform={`translate(${padding + 10}, ${headerH/2})`}>
                    {this.renderIcon(nameLabel)}
                </g>
            )}

            {genericNameLabel && (
                <g>
                    <rect
                        x={genericBoxX}
                        y={genericBoxY - 1}
                        width={genericBoxW + 1}
                        height={genericBoxH}
                        fill="white"
                        stroke="black"
                        stroke-width="1"
                        stroke-dasharray="3,3"
                    />
                    <g transform={`translate(${genericBoxX + genericBoxW/2}, ${genericBoxY + genericBoxH/2})`}>
                        {context.renderElement(genericNameLabel)}
                    </g>
                </g>
            )}

            <g>
                {fieldLabels.map((field, index) => {
                    const fieldTip = node.children.find(child =>
                        child.type === 'label:tip' &&
                        child.id === field.id + '-tip'
                    );

                    const fieldY = headerH + padding + (index * lineHeight) + 5;
                    const tipY = tipOffsetY;

                    if (fieldTip) {
                        const tipHeight = this.calculateTipHeight((fieldTip as any).text || '');
                        tipOffsetY += tipHeight + tipGap;
                    }

                    return <g>
                        <g transform={`translate(${w / 2}, ${fieldY})`}>
                            {context.renderElement(field)}
                        </g>

                        {fieldTip && (
                            <g>
                                <g transform={`translate(${w + 10}, ${tipY})`}>
                                    {this.renderTipAsNote(fieldTip)}
                                </g>
                                {this.renderTipLine(w, fieldY, w + 10, tipY + this.calculateTipHeight((fieldTip as any).text || '') / 2, (field as any).text)}
                            </g>
                        )}
                    </g>;
                })}
                <line x1={0} y1={headerH + fieldH} x2={w} y2={headerH + fieldH} class-simple-line={true}/>
            </g>

            <g>
                {methodLabels.map((method, index) => {
                    const methodTip = node.children.find(child =>
                        child.type === 'label:tip' &&
                        child.id === method.id + '-tip'
                    );

                    const methodY = headerH + fieldH + padding + (index * lineHeight) + 5;
                    const tipY = tipOffsetY;

                    if (methodTip) {
                        const tipHeight = this.calculateTipHeight((methodTip as any).text || '');
                        tipOffsetY += tipHeight + tipGap;
                    }

                    return <g>
                        <g transform={`translate(${w / 2}, ${methodY})`}>
                            {context.renderElement(method)}
                        </g>

                        {methodTip && (
                            <g>
                                <g transform={`translate(${w + 10}, ${tipY})`}>
                                    {this.renderTipAsNote(methodTip)}
                                </g>
                                {this.renderTipLine(w, methodY, w + 10, tipY + this.calculateTipHeight((methodTip as any).text || '') / 2, (method as any).text)}                            </g>
                        )}
                    </g>;
                })}
            </g>
        </g>;
    }

    private renderTipAsNote(tipLabel: any): VNode {
        const text = tipLabel.text || '';
        const boxWidth = tipLabel.args?.boxWidth || 200;

        const textLines = TspanConverter(text);
        const lineHeight = 14;
        const padding = 10;
        const foldSize = 10;

        const maxLineLength = Math.max(...textLines.map((line: VNode[]) =>
            line.reduce((sum, span) => sum + ((span as any).text?.length || 0), 0)
        ));
        const charWidth = 7;
        const noteWidth = Math.min(Math.max(maxLineLength * charWidth + padding * 2, 100), boxWidth * 0.9);
        const noteHeight = textLines.length * lineHeight + padding * 2;

        const color = '#FFFFCC';

        const lines: VNode[] = [];
        const initialY = textLines.length > 1 ? "10" : "0";

        for (let i = 0; i < textLines.length; i++) {
            const textSpans = textLines[i] ?? [];
            const dy = i === 0 ? initialY : "1.2em";

            lines.push(
                <tspan x="0" {...(i === 0 ? { y: dy } : { dy })}>
                    {textSpans}
                </tspan>
            );
        }

        return <g class-tip-note={true}>
            <polygon
                points={`0,0 ${noteWidth - foldSize},0 
                 ${noteWidth},${foldSize} ${noteWidth},${noteHeight} 
                 0,${noteHeight}`}
                fill={color}
                stroke="black"
                stroke-width="1"
            />

            <line
                x1={noteWidth - foldSize}
                y1={0}
                x2={noteWidth - foldSize}
                y2={foldSize}
                stroke="black"
                stroke-width="1"
            />

            <line
                x1={noteWidth - foldSize}
                y1={foldSize}
                x2={noteWidth}
                y2={foldSize}
                stroke="black"
                stroke-width="1"
            />

            <g transform={`translate(${noteWidth / 2 - 5}, ${padding})`}>
                <text
                    class-sprotty-label={true}
                    text-anchor="start"
                    x="0"
                    y="0"
                    font-size="11"
                    fill="black"
                >
                    {lines}
                </text>
            </g>
        </g>;
    }

    private renderTipLine(memberX: number, memberY: number, tipX: number, tipY: number, memberText?: string): VNode {
        const noteColor = '#FFFFCC';
        const baseWidth = 12;

        const charWidth = 6.5;
        const textWidth = memberText ? memberText.length * charWidth : 100;
        const entityCenterX = memberX / 2;
        const textEndX = entityCenterX + textWidth / 2;

        const memberAnchorX = Math.min(textEndX, memberX - 10);

        const dx = memberAnchorX - tipX;
        const dy = memberY - tipY;
        const length = Math.sqrt(dx * dx + dy * dy);
        const perpX = -dy / length * baseWidth / 2;
        const perpY = dx / length * baseWidth / 2;

        return <polygon
            points={`${tipX - perpX},${tipY - perpY} 
                     ${tipX + perpX},${tipY + perpY} 
                     ${memberAnchorX},${memberY}`}
            fill={noteColor}
            stroke="#FFFFCC"
            stroke-width="1"
        />;
    }

    private calculateTipHeight(text: string): number {
        const lines = text.split('<br>').length;
        const lineHeight = 14;
        const padding = 10;
        return lines * lineHeight + padding * 2;
    }

    private renderIcon(nameLabel: any): VNode {
        const type = (nameLabel as any).args?.type;
        const stereotypeChar = (nameLabel as any).args?.stereotypeChar;
        const stereotypeColor = (nameLabel as any).args?.stereotypeColor;

        const typeConfig = getTypeConfig(type);
        const displayChar = (stereotypeChar && stereotypeChar.trim().length > 0 && stereotypeChar !== ' ')
            ? stereotypeChar
            : typeConfig.char;
        const iconColor = (stereotypeColor && stereotypeColor.length > 0)
            ? stereotypeColor
            : typeConfig.color;

        const iconRadius = 8;

        return <g>
            <circle
                cx={0}
                cy={0}
                r={iconRadius}
                fill={iconColor}
                stroke="black"
                stroke-width={1}
            />
            <text
                x={0}
                y={0}
                fill="white"
                font-weight="bold"
                font-size="12"
                text-anchor="middle"
                dominant-baseline="middle"
            >
                {displayChar}
            </text>
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

export class NoteEntityView extends ShapeView {
    override render(node: GNode, context: RenderingContext): VNode {
        const width = node.size.width;
        const height = node.size.height;
        const foldSize = 12;
        const padding = 10;

        const color = '#FFFFCC';

        return <g class-note-entity={true}>
            <polygon
                points={`0,0 ${width - foldSize},0 
                         ${width},${foldSize} ${width},${height} 
                         0,${height}`}
                fill={color}
                stroke="black"
                stroke-width="1"
                class-note-body={true}
            />

            <line
                x1={width - foldSize}
                y1={0}
                x2={width - foldSize}
                y2={foldSize}
                stroke="black"
                stroke-width="1"
                class-note-fold={true}
            />

            <line
                x1={width - foldSize}
                y1={foldSize}
                x2={width}
                y2={foldSize}
                stroke="black"
                stroke-width="1"
                class-note-fold={true}
            />

            <g transform={`translate(${width / 2 - 5}, ${padding})`}>
                {context.renderChildren(node)}
            </g>
        </g>;
    }
}