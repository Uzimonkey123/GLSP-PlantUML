import {injectable} from "inversify";
import {GNode, RenderingContext, ShapeView, svg} from "@eclipse-glsp/client";
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

        const lineHeight = 14;
        const padding = 5;
        const minSectionHeight = 10;
        const headerH = 30;
        const fieldH = fieldLabels.length > 0
            ? fieldLabels.length * lineHeight + padding * 2
            : minSectionHeight;

        return <g>
            <rect class-sprotty-node={true} x={0} y={0} width={w} height={h} fill={background} stroke="white" stroke-width="2"/>

            <g>
                {nameLabel && (
                    <g transform={`translate(${w/2}, ${headerH/2 + 5})`}>
                        {context.renderElement(nameLabel)}
                    </g>
                )}
                <line x1={0} y1={headerH} x2={w} y2={headerH} class-simple-line={true}/>
            </g>

            <g>
                {fieldLabels.map((field, index) => (
                    <g transform={`translate(${w/2}, ${headerH + padding + (index * lineHeight) + 10})`}>
                        {context.renderElement(field)}
                    </g>
                ))}
                <line x1={0} y1={headerH + fieldH} x2={w} y2={headerH + fieldH} class-simple-line={true}/>
            </g>

            <g>
                {methodLabels.map((method, index) => (
                    <g transform={`translate(${w/2}, ${headerH + fieldH + padding + (index * lineHeight) + 10})`}>
                        {context.renderElement(method)}
                    </g>
                ))}
            </g>
        </g>;
    }
}