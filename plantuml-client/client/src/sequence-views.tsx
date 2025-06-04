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

	 private headPath(kind: string, part: 'top' | 'bottom' | 'full', circle: boolean): string | undefined {
		 let circleOffset = circle ? 5 : 10;

		 switch (`${kind}:${part}`) {
			 case 'block:full':
				 return `M0,-4 L${circleOffset},0 L0,4 Z`;
			 case 'block:top':
				 return `M0,-4 L${circleOffset},0 L0,0 Z`;
			 case 'block:bottom':
				 return `M0,4 L${circleOffset},0 L0,0 Z`;

			 case 'open:full':
				 return `M0,-4 L${circleOffset + 1},0 M0,4 L${circleOffset + 1},0, M0,0 L${circleOffset + 1},0`;
			 case 'open:top':
				 return `M0,-4 L${circleOffset + 1},0 M0,0 L${circleOffset + 1},0`;
			 case 'open:bottom':
				 return `M0,4 L${circleOffset + 1},0 M0,0 L${circleOffset + 1},0`;

			 case 'cross:full':
				 return 'M-4,-4 L4,4 M4,-4 L-4,4'

			 default:
				 return undefined;
		 }
	 }

	 protected override renderAdditionals(
		 edge: GEdge,
		 segments: Point[],
		 context: RenderingContext,
		 args?: IViewArgs
	 ): VNode[] {

	 	const additionals = super.renderAdditionals(edge, segments, context);

		let circleStart = false;
		let circleEnd = false;

		if (segments.length < 3) return additionals;

		const interior = segments.slice(1, segments.length - 1);
		if (interior.length < 2) return additionals;

		const start = interior[0];
		const end = interior[interior.length - 1];
		const dx = end.x - start.x;
		const dy = end.y - start.y;
		const norm = Math.hypot(dx, dy) || 1;


		// Getting heads, parts and style for a single arrow
		const style = (edge.args?.style as string) ?? 'solid';
		const headStart = (edge.args?.headStart  as string) ?? 'none';
		const headEnd = (edge.args?.headEnd  as string) ?? 'none';
		const partStart = (edge.args?.partStart as string) ?? 'full';
		const partEnd = (edge.args?.partEnd as string) ?? 'full';

		// Getting args for circle decoration at the start/end of arrows
		const circleStartPart = (edge.args?.circleStart as string) ?? 'none';
		const circleEndPart = (edge.args?.circleEnd as string) ?? 'none';
		if (circleStartPart !== "none") circleStart = true;
		if (circleEndPart !== "none") circleEnd = true;


		const strokeWidth = style === 'bold'   ? 2.5 : 1.5;
		const dashed = style === 'dotted' ? '2 2' : undefined;

	 	// Setting offset according to if needed to start or end earlier
		let lineStartOffset = headStart == 'cross' ? -10 : 0;
		let lineEndOffset = headEnd == 'cross' ? 10 : 0;

		const lineStartX = start.x - (dx / norm) * lineStartOffset;
		const lineStartY = start.y - (dy / norm) * lineStartOffset;

		const lineEndX = end.x - (dx / norm) * lineEndOffset;
		const lineEndY = end.y - (dy / norm) * lineEndOffset;

		additionals.unshift(
			<path
				d={`M ${lineStartX} ${lineStartY} L ${lineEndX} ${lineEndY}`}
				stroke="black"
				strokeWidth={strokeWidth}
				{...(dashed ? { 'stroke-dasharray': dashed } : {})}
				marker-end="none"
				fill="none"
				class-sprotty-edge={true}
			/>
		);

		// Calculation of angle of the arrow + setting the shift according to it
		const angle = (Math.atan2(dy, dx) * 180) / Math.PI;
		const shift = 10;

		// Saving new end and start arrow positions according to side they are facing
		const endArrowPos = {
			x: end.x + (dx === 0 ? 0 : (dx > 0 ? -shift : shift)),
			y: end.y
		}
		const startArrowPos = {
			x: start.x + (dx === 0 ? 0 : (dx > 0 ? shift : -shift)),
			y: start.y
		}

		const drawHead = (kind: string, at: Point, ang: number, pos: string, circle: boolean) => {
			if (kind == 'none') return;

			const part = pos == "end" ? partEnd : partStart
			const d = this.headPath(kind, part as 'top' | 'bottom' | 'full', circle);
			if (d) {
				additionals.push(
					<path d={d}
						transform={`translate(${at.x} ${at.y}) rotate(${ang})`}
						fill={kind === 'block' ? 'black' : 'none'}
						stroke="black" strokeWidth={strokeWidth}
						class-sprotty-edge={true}
					/>
				);
			}
		};

		const drawCircle = (at: Point) : void => {
			additionals.push(
				<circle
					cx={at.x}
					cy={at.y}
					r={3}
					fill="black"
					stroke="black"
					strokeWidth={1}
				/>
			)
		}

		drawHead(headStart, startArrowPos, angle + 180, "start", circleStart);
		drawHead(headEnd, endArrowPos, angle, "end", circleEnd);

		if (circleStart) drawCircle(start);
		if (circleEnd) drawCircle(end);


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