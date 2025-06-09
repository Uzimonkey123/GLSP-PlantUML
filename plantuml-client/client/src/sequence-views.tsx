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

	private start! : Point;
	private end! : Point;
	private circleStart = false;
	private circleEnd = false;

	private self! : boolean;
	private style! : string;
	private headStart! : string;
	private headEnd! : string;
	private partStart! : string;
	private partEnd! : string;
	private arrColor! : string;
	private circleStartPart! : string;
	private circleEndPart! : string;

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

		this.circleStart = false;
		this.circleEnd = false;

		if (segments.length < 3) return additionals;

		const interior = segments.slice(1, segments.length - 1);
		if (interior.length < 2) return additionals;

		this.start = interior[0];
		this.end = interior[interior.length - 1];

		this.self = (edge.args?.self as boolean) ?? false;

		// Getting heads, parts and style for a single arrow
		this.style = (edge.args?.style as string) ?? 'solid';
		this.headStart = (edge.args?.headStart  as string) ?? 'none';
		this.headEnd = (edge.args?.headEnd  as string) ?? 'none';
		this.partStart = (edge.args?.partStart as string) ?? 'full';
		this.partEnd = (edge.args?.partEnd as string) ?? 'full';
		this.arrColor = (edge.args?.arrColor as string) ?? "black";

		// Getting args for circle decoration at the start/end of arrows
		this.circleStartPart = (edge.args?.circleStart as string) ?? 'none';
		this.circleEndPart = (edge.args?.circleEnd as string) ?? 'none';
		if (this.circleStartPart !== "none") this.circleStart = true;
		if (this.circleEndPart !== "none") this.circleEnd = true;

		if(!this.self) {
			this.drawSimpleArrow(additionals);
		} else {
			this.drawSelfArrow(additionals);
		}

		this.placeLabel(context, additionals, edge, args);
		return additionals;
	 }

	 private drawSelfArrow(additionals: VNode[]) {
		 const strokeWidth = this.style === 'bold'   ? 2.5 : 1.5;
		 const dashed = this.style === 'dotted' ? '2 2' : undefined;

		 let crossOffsetStart = this.headStart == 'cross' ? -10 : 0;
		 let crossOffsetEnd = this.headEnd == 'cross' ? -10 : 0;
		 const lineStart = this.start.x - crossOffsetStart;
		 const lineEnd = this.start.x - crossOffsetEnd;

		 additionals.unshift(
			 <path
				 d={`M ${lineStart} ${this.start.y}
				 	L ${lineStart + 50 + crossOffsetStart} ${this.start.y}
				 	M ${lineEnd} ${this.start.y + 15}
				 	L ${lineEnd + 50 + crossOffsetEnd} ${this.start.y + 15}
				 	M ${lineStart + 50 + crossOffsetStart} ${this.start.y}
				 	L ${lineStart + 50 + crossOffsetStart} ${this.start.y + 15}`}
				 stroke={this.arrColor}
				 strokeWidth={strokeWidth}
				 {...(dashed ? {'stroke-dasharray': dashed} : {})}
				 marker-end="none"
				 fill="none"
				 class-sprotty-edge={true}
			 />
		 );

		 const endArrowPos = {
			 x: this.end.x + 10,
			 y: this.end.y + 15
		 }
		 const startArrowPos = {
			 x: this.start.x + 10,
			 y: this.start.y
		 }

		 this.drawHead(this.headStart, startArrowPos, 180, "start", this.circleStart, additionals, strokeWidth);
		 this.drawHead(this.headEnd, endArrowPos, 180, "end", this.circleEnd, additionals, strokeWidth);

		 if (this.circleStart) this.drawCircle(this.start, additionals);
		 if (this.circleEnd) {
			 const endCircle = {
				 x: this.end.x,
				 y: this.end.y + 15
			 }
			 this.drawCircle(endCircle, additionals);
		 }
	 }

	 private drawSimpleArrow(additionals: VNode[]) {

		 const dx = this.end.x - this.start.x;
		 const dy = this.end.y - this.start.y;
		 const norm = Math.hypot(dx, dy) || 1;

		 const strokeWidth = this.style === 'bold'   ? 2.5 : 1.5;
		 const dashed = this.style === 'dotted' ? '2 2' : undefined;

		 // Setting offset according to if needed to start or end earlier
		 let lineStartOffset = this.headStart == 'cross' ? -10 : 0;
		 let lineEndOffset = this.headEnd == 'cross' ? 10 : 0;

		 const lineStartX = this.start.x - (dx / norm) * lineStartOffset;
		 const lineStartY = this.start.y - (dy / norm) * lineStartOffset;

		 const lineEndX = this.end.x - (dx / norm) * lineEndOffset;
		 const lineEndY = this.end.y - (dy / norm) * lineEndOffset;

		 additionals.unshift(
			 <path
				 d={`M ${lineStartX} ${lineStartY} L ${lineEndX} ${lineEndY}`}
				 stroke={this.arrColor}
				 strokeWidth={strokeWidth}
				 {...(dashed ? {'stroke-dasharray': dashed} : {})}
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
			 x: this.end.x + (dx === 0 ? 0 : (dx > 0 ? -shift : shift)),
			 y: this.end.y
		 }
		 const startArrowPos = {
			 x: this.start.x + (dx === 0 ? 0 : (dx > 0 ? shift : -shift)),
			 y: this.start.y
		 }

		 this.drawHead(this.headStart, startArrowPos, angle + 180, "start", this.circleStart, additionals, strokeWidth);
		 this.drawHead(this.headEnd, endArrowPos, angle, "end", this.circleEnd, additionals, strokeWidth);

		 if (this.circleStart) this.drawCircle(this.start, additionals);
		 if (this.circleEnd) this.drawCircle(this.end, additionals);
	 }

	 private drawHead(kind: string, at: Point, ang: number, pos: string, circle: boolean, additionals: VNode[], strokeWidth: number) {
		 if (kind == 'none') return;

		 const part = pos == "end" ? this.partEnd : this.partStart
		 const d = this.headPath(kind, part as 'top' | 'bottom' | 'full', circle);
		 if (d) {
			 additionals.push(
				 <path d={d}
					   transform={`translate(${at.x} ${at.y}) rotate(${ang})`}
					   style={{ fill: kind === 'block' ? this.arrColor : 'none' }}
					   stroke={this.arrColor} strokeWidth={strokeWidth}
				 />
			 );
		 }
	 }

	 private drawCircle(at: Point, additionals: VNode[]) {
		 additionals.push(
			 <circle
				 cx={at.x}
				 cy={at.y}
				 r={3}
				 fill={this.arrColor}
				 stroke={this.arrColor}
				 strokeWidth={1}
			 />
		 )
	 }

	 private placeLabel(context: RenderingContext, additionals: VNode[], edge: GEdge, args?: IViewArgs) {
		 const midX = (this.start.x + this.end.x) / 2;
		 const midY = (this.start.y + this.end.y) / 2;
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