import { injectable } from 'inversify';
import { 
    GNode, 
    GEdge, 
    Point, 
    RenderingContext, 
    IViewArgs, 
    PolylineEdgeViewWithGapsOnIntersections,
    ShapeView,
    svg
 } from '@eclipse-glsp/client';
import { VNode } from "snabbdom";
import '../css/diagram.css';

 /** @jsx svg */

@injectable()
export class SequenceMessageEdgeView extends PolylineEdgeViewWithGapsOnIntersections {
	protected override renderAdditionals(
		edge: GEdge,
		segments: Point[],
		context: RenderingContext,
		args?: IViewArgs
	): VNode[] {
		const additionals = super.renderAdditionals(edge, segments, context);

		if (segments.length < 3) { return additionals; }

		const interior = segments.slice(1, segments.length - 1);
		if (interior.length < 2) { return additionals; }

		const start = interior[0];
		const end = interior[interior.length - 1];

		// Get the vectors
		const dx = end.x - start.x;
		const dy = end.y - start.y;
		
		// Calculate the end of the line
		const lineEndX = end.x - (dx / (Math.hypot(dx, dy) || 1)) * 10;
		const lineEndY = end.y - (dy / (Math.hypot(dx, dy) || 1)) * 10;

		// Angle of the arrow head
		const angle = (Math.atan2(dy, dx) * 180) / Math.PI; // Convert to degrees
		const shift = 10; 
		// Shift the arrow head a bit to the left or right depending on the angle
		let arrowX = end.x;
		let arrowY = end.y;

		// Depending the angle, calculate where to move the arrow head on the line
		angle == 0 ? (arrowX -= shift) : (arrowX += shift);

		additionals.unshift(
		<path
			d={`M ${start.x} ${start.y} L ${lineEndX} ${lineEndY}`}
			stroke="black"
			strokeWidth={2}
			fill="none"
			class-sprotty-edge={true}
		/>
		);

		additionals.push(
		<path
			class-sprotty-edge={true}
			class-arrow={true}
			d="M0,-4 L10,0 L0,4 Z"
			transform={`translate(${arrowX} ${arrowY}) rotate(${angle})`}
			fill="white"
		/>
		);

		// Center label
		const midX = (start.x + end.x) / 2;
		const midY = (start.y + end.y) / 2;
		const labels = context.renderChildren(edge, args);
		if (labels.length) {
			additionals.push(
			<g transform={`translate(${midX},${midY})`}>
				{labels.map((l, i) =>
					<text key={i}
						{...(l.data?.props as any)}
						text-anchor="middle"
						fill="black">
					{l.children}
					</text>
				)}
			</g>
			);
		}
		return additionals;
	}
}

@injectable()
export class RectangularNodeView extends ShapeView {
  	override render(
		node: Readonly<GNode>, 
		context: RenderingContext
	): VNode {
		const w = node.size.width;
		const totalH = node.size.height;

		const headerH = 30;
		const footerH = 30;

		// Lifeline between header and footer
		const lifeLineStart = headerH;
		const lifeLineEnd = totalH - footerH;

		return <g>
		{/* Top rectangle */}
		<g>
			<rect class-sprotty-node={true} x={0} y={0} width={w} height={headerH}/>
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
		<g transform={`translate(0, ${lifeLineEnd})`}>
			<rect class-sprotty-node={true} x={0} y={0} width={w} height={footerH}/>
			<g transform={`translate(${w/2},${footerH/2})`}>
				{context.renderChildren(node)}
			</g>
		</g>
		</g>;
	}
}