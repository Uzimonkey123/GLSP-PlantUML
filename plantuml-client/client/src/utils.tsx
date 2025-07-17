import {VNode} from "snabbdom";
import {GNode, svg} from "@eclipse-glsp/client";

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

    // Replace <br> with \n
    const normalized = html.replace(/<br\s*\/?>/gi, '\n');

    // Tokenize the input so it takes the \n, <.., />
    const tokens = normalized.match(/<<[^<>]+>>|<\/?[^>]+>|[^<\n]+|\n/g) || [];

    for (const token of tokens) {
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
    const lineHeight = 14; // Base height across client and server
    const iconRadius = 8;
    const iconCenterX = -width / 2 + iconRadius + 5; // 5px padding from left edge
    const iconCenterY = lineHeight / 2;

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
