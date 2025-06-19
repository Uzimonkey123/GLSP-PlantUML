import { injectable } from 'inversify';
import {
	GEdge,
	Point,
	RenderingContext,
	IViewArgs,
	PolylineEdgeViewWithGapsOnIntersections,
	svg,
	SEdgeImpl,
	GLabelView,
	GLabel
} from '@eclipse-glsp/client';
import { VNode } from "snabbdom";
import '../css/diagram.css';
import { TspanConverter } from "./utils";

/** @jsx svg */

@injectable()
export class HtmlLabelView extends GLabelView {
	override render(label: Readonly<GLabel>,
					context: RenderingContext,
					args?: IViewArgs): VNode {
		const num = (label as any).args?.numbering as string | undefined;
		const text = label.text ?? '';

		const numSpans = num ? TspanConverter(num) : [];

		return (
			<text class-sprotty-label={true} text-anchor="middle">
				{numSpans}
				{num ? <tspan> </tspan> : undefined }
				<tspan>{text}</tspan>
			</text>
		);
	}
}

@injectable()
export class SequenceMessageDivider extends PolylineEdgeViewWithGapsOnIntersections {
	protected override renderAdditionals(
		edge: SEdgeImpl,
		segments: Point[],
		context: RenderingContext,
		args? : IViewArgs
	): VNode[] {

		const additionals = super.renderAdditionals(edge, segments, context);

		if (segments.length < 3) return additionals;

		const interior = segments.slice(1, segments.length - 1);
		if (interior.length < 2) return additionals;

		const start = interior[0];
		const end = interior[interior.length - 1];
		// Center of the divider
		const centerX = (start.x + end.x) / 2;
		const centerY = (start.y + end.y) / 2;

		// Label text
		const label = context.renderChildren(edge, args);

		const labelPadding = 4;
		const fontSize = 11;
		const textVNode = label[0]; // Get the first and only label
		const textContent = String(textVNode.children ?? "");
		const labelWidth = textContent.length * 4.5; // Calculate an approx. width of letters

		additionals.push(
			<g>
				{/* Bottom line */}
				<line
					x1={start.x}
					y1={centerY - 6}
					x2={end.x}
					y2={centerY - 6}
					stroke="black"
				/>

				{/* Top line*/}
				<line
					x1={start.x}
					y1={centerY - 9}
					x2={end.x}
					y2={centerY - 9}
					stroke="black"
				/>

				{/* Label */}
				<rect
					x={centerX - labelWidth / 2 - labelPadding}
					y={centerY - 15}
					width={labelWidth + 2 * labelPadding}
					height={fontSize + 4}
					fill="#5d4949"
					stroke="black"
					stroke-width={2}
				/>

				<text
					x={centerX}
					y={centerY - 2}
					text-anchor="middle"
					alignment-baseline="middle"
					font-size={fontSize}
				>
					{label}
				</text>
			</g>
		);

		return additionals;
	}
}

@injectable()
export class SequenceMessageDelay extends PolylineEdgeViewWithGapsOnIntersections {
	protected override renderAdditionals(
		edge: SEdgeImpl,
		segments: Point[],
		context: RenderingContext,
		args?: IViewArgs
		): VNode[]
	{

		const additionals = super.renderAdditionals(edge, segments, context);

		if (segments.length < 3) return additionals;

		const interior = segments.slice(1, segments.length - 1);
		if (interior.length < 2) return additionals;

		const start = interior[0];
		const end = interior[interior.length - 1];

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
export class SequenceMessageEdgeView extends PolylineEdgeViewWithGapsOnIntersections {

	private start = {x: 0, y: 0};
	private end = {x: 0, y: 0};
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

		const halfWidth = (edge.args?.toWidth as number) ?? 0;
		const creating = (edge.args?.creating as boolean) ?? false;


		// Check if the node is created. If yes, adjust the ending x coord
		if(creating) {
			if (this.end.x > this.start.x) {
				this.end.x -= halfWidth;
			} else {
				this.end.x += halfWidth;
			}
		}

		this.self = (edge.args?.self as boolean) ?? false;
		const incoming = (edge.args?.incoming as boolean) ?? false;
		const outgoing = (edge.args?.outgoing as boolean) ?? false;

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
			incoming || outgoing
				? this.drawOutInArrow(additionals, edge, incoming)
				: this.drawSimpleArrow(additionals);
		} else {
			this.drawSelfArrow(additionals);
		}

		this.placeLabel(context, additionals, edge, args);
		return additionals;
	 }

	private drawOutInArrow(additionals: VNode[], edge: GEdge, incoming: boolean) {
		 // Additional arguments to get distance between two nodes who have short arrows
		const fromX = edge.args?.fromX as number;
		const toX = edge.args?.toX as number;
		const short = (edge.args?.isShort as boolean) ?? false;

		// Default lengths
		let drawStartX = this.start.x;
		let drawEndX = this.end.x;
		let drawStartY = this.start.y;
		let drawEndY = this.end.y;

		// Check for short arrow, since they start between the given nodes
		if (short) {
			if (incoming) {
				drawStartX = (fromX + toX) / 2;
				drawEndX = this.end.x;
			} else {
				drawStartX = this.start.x;
				drawEndX = (fromX + toX) / 2;
			}
		}

		const dx = drawEndX - drawStartX;
		const dy = drawEndY - drawStartY;
		const norm = Math.hypot(dx, dy) || 1;

		const strokeWidth = this.style === 'bold' ? 2.5 : 1.5;
		const dashed = this.style === 'dotted' ? '2 2' : undefined;

		// Setting offset according to if needed to start or end earlier
		const lineStartOffset = this.headStart === 'cross' ? -10 : 0;
		const lineEndOffset = this.headEnd === 'cross' ? 10 : 0;

		const lineStartX = drawStartX - (dx / norm) * lineStartOffset;
		const lineStartY = drawStartY - (dy / norm) * lineStartOffset;

		const lineEndX = drawEndX - (dx / norm) * lineEndOffset;
		const lineEndY = drawEndY - (dy / norm) * lineEndOffset;

		additionals.unshift(
			<path
				d={`M ${lineStartX} ${lineStartY} L ${lineEndX} ${lineEndY}`}
				stroke={this.arrColor}
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
			x: drawEndX + (dx === 0 ? 0 : dx > 0 ? -shift : shift),
			y: drawEndY
		};
		const startArrowPos = {
			x: drawStartX + (dx === 0 ? 0 : dx > 0 ? shift : -shift),
			y: drawStartY
		};

		this.drawHead(this.headStart, startArrowPos, angle + 180, "start", this.circleStart, additionals, strokeWidth);
		this.drawHead(this.headEnd, endArrowPos, angle, "end", this.circleEnd, additionals, strokeWidth);

		if (this.circleStart) this.drawCircle({ x: drawStartX, y: drawStartY }, additionals);
		if (this.circleEnd) this.drawCircle({ x: drawEndX, y: drawEndY }, additionals);
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
					 {labels.map((l, i) => {
						 const raw = (l.text as string | undefined) ?? '';
						 return (
							 <text key={i}
								   {...(l.data?.props as any)}
								   text-anchor="middle"
								   dominant-baseline="central"
								   fill="black">

								 {TspanConverter(raw)}
							 </text>
						 );
					 })}
				 </g>
			 );
		 }
	 }
}

@injectable()
export class SequenceHeaderFooter extends GLabelView {
	override render(label: Readonly<GLabel>,
					context: RenderingContext,
					args?: IViewArgs): VNode {
		const text = label.text ?? '';

		return (
			<text text-anchor="middle"
			      fill="grey"
			      font-size="10">
				<tspan fill="grey" font-size="10">{text}</tspan>
			</text>
		);
	}
}

@injectable()
export class SequenceTitle extends GLabelView {
	override render(label: Readonly<GLabel>,
					context: RenderingContext,
					args?: IViewArgs): VNode {
		const text = label.text ?? '';

		return (
			<text class-sprotty-label={true} text-anchor="middle">
				<tspan>{text}</tspan>
			</text>
		);
	}
}
