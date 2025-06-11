import {VNode} from "snabbdom";
import {svg} from "@eclipse-glsp/client";

export function TspanConverter(html: string): VNode[] {
    type Style = {
        bold?: boolean;
        italic?: boolean;
        underline?: boolean;
        color?: string };
    const stack: Style[] = [{}];
    const out: VNode[] = [];

    const push = (t: string) => {
        if (!t) return;
        const s = stack[stack.length - 1];
        out.push(
            <tspan
                font-weight={s.bold ? 'bold' : undefined}
        font-style={s.italic ? 'italic' : undefined}
        text-decoration={s.underline ? 'underline' : undefined}
        fill={s.color}>
            {t}
            </tspan>);
    };

    html.replace(/<\/?[^>]+>|[^<]+/g, token => {
        if (token.startsWith('</')) {
            stack.pop();
        } else if (token.startsWith('<')) {
            const top = { ...stack[stack.length - 1] };
            if (token.startsWith('<b')) top.bold = true;
            if (token.startsWith('<i')) top.italic = true;
            if (token.startsWith('<u')) top.underline = true;
            if (token.startsWith('<font')) {
                const matcher = token.match(/color\s*=\s*['"]?([^'">]+)/i);
                if (matcher) top.color = matcher[1];
            }
            stack.push(top);

        } else {
            push(token);
        }

        return '';
    });

    return out;
}