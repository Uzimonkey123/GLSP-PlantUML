import {VNode} from "snabbdom";
import {EditLabelUI, GLabel, GLabelView, GNode, IViewArgs, RenderingContext, svg} from "@eclipse-glsp/client";
import {injectable} from "inversify";

@injectable()
export class BrEditLabelUI extends EditLabelUI {
    private isMultilineLabel(): boolean {
        const id = this.label?.id ?? '';
        return !id.startsWith('group-');  // Groups do not have multiline label/comment/separator
    }

    public override get editControl(): HTMLInputElement | HTMLTextAreaElement {
        // Check if input or text area is needed for label/multiline
        return this.isMultilineLabel() ? this.textAreaElement : this.inputElement;
    }

    protected override configureAndAdd(
        element: HTMLInputElement | HTMLTextAreaElement,
        container: HTMLElement
    ): void {
        // Make the previous handlers as they should be
        super.configureAndAdd(element, container);

        element.style.overflow = 'hidden'; // no scrollbar
        element.style.resize = 'none';

        element.addEventListener('keydown', (ev: Event) => {
            const e = ev as KeyboardEvent;
            if (e.key === 'Enter' && e.shiftKey) { // Shift enter for next line
                e.preventDefault();
                e.stopPropagation(); // Do not apply at shift + enter
                this.insertAtCursor(element, '\n');
                this.autoSizeEditor();
            }
        });

        // To change size automatically
        element.addEventListener('input', () => this.autoSizeEditor());

        // Create the first sized elements with DOM
        requestAnimationFrame(() => this.autoSizeEditor());
    }

    protected override applyTextContents(): void {
        if (!this.label) return;
        // To display multilines instead of simple <br>
        const display = (this.label.text ?? '').replace(/<br>/g, '\n');
        this.editControl.value = display;

        if (this.editControl instanceof HTMLTextAreaElement) {
            // To see the beginning of the message, not scrolled etc...
            this.editControl.selectionStart = this.editControl.selectionEnd = display.length;
            this.editControl.scrollTop = 0;
            this.editControl.scrollLeft = 0;

        } else {
            // For input text
            this.editControl.setSelectionRange(0, display.length);
        }
    }

    protected override applyFontStyling(): void {
        super.applyFontStyling();
        requestAnimationFrame(() => this.autoSizeEditor());
    }

    protected override async applyLabelEdit(): Promise<void> {
        this.editControl.value = this.editControl.value
            .replace(/\r?\n/g, '<br>');
        await super.applyLabelEdit();
    }

    private insertAtCursor(el: HTMLInputElement | HTMLTextAreaElement, text: string): void {
        const start = el.selectionStart ?? 0;
        const end   = el.selectionEnd ?? 0;
        el.value = el.value.substring(0, start) + text + el.value.substring(end);
        const pos = start + text.length;
        el.setSelectionRange(pos, pos);
    }

    private autoSizeEditor(): void {
        if (!(this.editControl instanceof HTMLTextAreaElement)) return;
        const ta = this.editControl;

        ta.style.height = 'auto';
        ta.style.height = ta.scrollHeight + 'px';

        ta.style.width = 'auto';
        ta.style.width = ta.scrollWidth + 4 + 'px';
    }
}

@injectable()
export class HtmlLabelView extends GLabelView {
    override render(label: Readonly<GLabel>, context: RenderingContext, args?: IViewArgs): VNode {
        const num = (label as any).args?.numbering as string | undefined;
        const visibility = (label as any).args?.visibility as string | undefined;
        const boxWidth = (label as any).args?.boxWidth as number | undefined;
        const text = label.text ?? '';

        const numLines = num ? TspanConverter(num) : [];
        const textLines = TspanConverter(text);

        const lines: VNode[] = [];
        const max = Math.max(numLines.length, textLines.length);
        const initialY = max > 1 ? "10" : "0";

        for (let i = 0; i < max; i++) {
            const numSpans = numLines[i] ?? [];
            const textSpans = textLines[i] ?? [];
            const dy = i === 0 ? initialY : "1.2em";

            lines.push(
                <tspan x="0" {...(i === 0 ? { y: dy } : { dy })}>
                    {numSpans}
                    {numSpans.length > 0 && textSpans.length > 0 ? <tspan></tspan> : null}
                    {textSpans}
                </tspan>
            );
        }

        const visibilityShape = this.renderVisibilityShape(visibility);

        const shapeOffset = boxWidth ? -boxWidth/2 + 3 : 0;

        return (
            <g>
                {visibilityShape && <g transform={`translate(${shapeOffset}, 0)`}>{visibilityShape}</g>}
                <text
                    class-sprotty-label={true}
                    text-anchor="start"
                    x="0"
                    y="0"
                >
                    {lines}
                </text>
            </g>
        );
    }

    private renderVisibilityShape(visibility: string | undefined): VNode | null {
        if (!visibility) return null;

        const size = 8;
        const cy = 0;

        switch (visibility) {
            case 'public':
                return <circle cx={size/2} cy={cy} r={size/2} fill="green" stroke="green" stroke-width="1"/>;

            case 'protected':
                return <polygon
                    points={`${size/2},${cy-size/2} ${size},${cy} ${size/2},${cy+size/2} 0,${cy}`}
                    fill="yellow"
                    stroke="yellow"
                    stroke-width="1"
                />;

            case 'private':
                return <rect
                    x={0}
                    y={cy-size/2}
                    width={size}
                    height={size}
                    fill="red"
                    stroke="red"
                    stroke-width="1"
                />;

            case 'package_private':
                return <polygon
                    points={`${size/2},${cy-size/2} ${size},${cy+size/2} 0,${cy+size/2}`}
                    fill="blue"
                    stroke="blue"
                    stroke-width="1"
                />;

            default:
                return null;
        }
    }
}

export function TspanConverter(html: string): VNode[][] {
    type Style = { //TODO: Add more styles according to plantuml
        bold?: boolean;
        italic?: boolean;
        underline?: boolean;
        color?: string };
    const stack: Style[] = [{}]; // Stack for styles
    const result: VNode[][] = [];
    let currentLine: VNode[] = [];

    const applyStyle = (text: string) => {
        if (!text) return;
        const style = stack[stack.length - 1];

        currentLine.push(
            <tspan
                font-weight={style.bold ? 'bold' : undefined}
                font-style={style.italic ? 'italic' : undefined}
                text-decoration={style.underline ? 'underline' : undefined}
                fill={style.color}
            >
                {text}
            </tspan>
        );
    };

    const applyLineModifier = (styleKey: 'italic' | 'underline') => {
        // Update base style
        stack[0] = { ...stack[0], [styleKey]: true };

        // Update all styles in the stack
        for (let i = 1; i < stack.length; i++) {
            stack[i] = { ...stack[i], [styleKey]: true };
        }

        currentLine = currentLine.map(vnode => {
            const attrs = vnode.data?.attrs || {};
            const newAttrs: any = { ...attrs };

            if (styleKey === 'italic') {
                newAttrs['font-style'] = 'italic';
            } else if (styleKey === 'underline') {
                newAttrs['text-decoration'] = 'underline';
            }

            return {
                ...vnode,
                data: {...vnode.data, attrs: newAttrs}
            };
        });
    };

    // Replace <br> with \n
    const normalized = html.replace(/<br\s*\/?>/gi, '\n');

    // Tokenize the input so it takes the \n, <.., />
    const tokens = normalized.match(/<<[^<>]+>>|\{abstract\}|\{static\}|\{classifier\}|<\/?[^>]+>|[^<\n{}]+|\n/g) || [];

    for (const token of tokens) {
        if (token === '{abstract}') {
            applyLineModifier('italic');
            continue;
        }

        if (token === '{static}' || token === '{classifier}') {
            applyLineModifier('underline');
            continue;
        }

        if (token.startsWith('<<') && token.endsWith('>>')) {
            applyStyle(token);
            continue;
        }

        if (token === '\n') {
            result.push(currentLine);
            currentLine = [];
            continue;
        }

        if (token.startsWith('</')) {
            stack.pop(); // End style

        } else if (token.startsWith('<')) {
            // Opening tag
            const currentStyle = { ...stack[stack.length - 1] };

            // To match the booleans styles defined before
            if (token.startsWith('<b')) currentStyle.bold = true;
            if (token.startsWith('<i')) currentStyle.italic = true;
            if (token.startsWith('<u')) currentStyle.underline = true;

            // To get the color from <font color=...>
            if (token.startsWith('<font')) {
                const colorMatch = token.match(/color=['"]?([^'">]+)/i);
                if (colorMatch) {
                    currentStyle.color = colorMatch[1];
                } else {
                    throw new Error("Error: Wrong usage of <font. Use: <font color=color");
                }
            }

            stack.push(currentStyle);

        } else {
            applyStyle(token); // Normal text without anything in it
        }
    }

    if (currentLine.length > 0) {
        result.push(currentLine);
    }

    return result;
}

export function createIcon(width: number, background: String, stereotypeChar: String) {
    const iconRadius = 8;
    const iconCenterX = -width / 2 + iconRadius + 2; // 2px padding from left edge
    const iconCenterY = 0;

    return (
        <g>
            <circle
                cx={iconCenterX}
                cy={iconCenterY}
                r={iconRadius}
                fill={background}
                stroke="black"
                stroke-width={1}
            />
            <text
                x={iconCenterX}
                y={iconCenterY}
                fill="white"
                font-weight="bold"
                font-size="12"
                text-anchor="middle"
                alignment-baseline="middle"
            >
                {stereotypeChar}
            </text>
        </g>
    );
}

export interface NodeViewArgs {
    w: number;
    h: number;
    background?: string;
    showFoot: boolean;
    headerH: number;
    footerH: number;
    labelLines: number;
    labelHeight: number;
    lifeLineStart: number;
    lifeLineEnd: number;
    cx: number;
}

export function getNodeArgs(node: Readonly<GNode>): NodeViewArgs {
    const w = node.size.width;
    const h = node.size.height;
    const args = (node as any).args ?? {};
    const background = args.background;
    const showFoot = !!args.showFoot;

    const headerH = args.headerHeight || 0;
    const footerH = args.headerHeight || 0;

    const labelLines = (args.name || "").split("<br>").length;
    const lineHeight = 14;
    const labelHeight = labelLines * lineHeight;

    // Lifeline line coordinates to be between the two labels
    const lifeLineStart = headerH;
    const lifeLineEnd = h - footerH;

    return {
        w,
        h,
        background,
        showFoot,
        headerH,
        footerH,
        labelLines,
        labelHeight,
        lifeLineStart,
        lifeLineEnd,
        cx: w / 2
    };
}
