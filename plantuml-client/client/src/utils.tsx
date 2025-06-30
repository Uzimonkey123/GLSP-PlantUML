import {VNode} from "snabbdom";
import {svg} from "@eclipse-glsp/client";

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
