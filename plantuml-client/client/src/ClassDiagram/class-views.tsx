import {injectable} from 'inversify';
import {
    GLabel,
    GLabelView,
    IViewArgs,
    RenderingContext,
    svg
} from '@eclipse-glsp/client';
import {VNode} from "snabbdom";
import '../../css/diagram.css';
import {createIcon, renderVisibilityShape, TspanConverter} from "../utils";

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
