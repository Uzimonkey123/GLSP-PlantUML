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

        const x = node.position.x;
        const y = node.position.y;

        return <g>
            <rect class-sprotty-node={true} x={x} y={y} width={w} height={h}/>
        </g>
    }
}