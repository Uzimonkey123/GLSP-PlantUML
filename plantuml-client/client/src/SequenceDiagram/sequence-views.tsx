import {injectable} from 'inversify';
import {
	GEdge,
	GEdgeView,
	GLabel,
	GLabelView,
	IViewArgs,
	Point,
	PolylineEdgeViewWithGapsOnIntersections,
	RenderingContext,
	SEdgeImpl,
	svg
} from '@eclipse-glsp/client';
import {VNode} from "snabbdom";
import '../../css/diagram.css';
import {createIcon, TspanConverter} from "../utils";

/** @jsx svg */

@injectable()
export class ParticipantLabelView extends GLabelView {
	override render(label: Readonly<GLabel>, context: RenderingContext, args?: IViewArgs): VNode {
		// TODO: Formating for creol and html (monospace, italic etc..)
		const text = label.text ?? '';
		const lines = text.split("<br>");
		const lineHeight = 14;

		const width = (label as any).args?.width;
		const background = (label as any).args?.stereotypeCharColor;
		const stereotypeChar = (label as any).args?.stereotypeChar ?? '-';
		const hasIcon = stereotypeChar.length > 0 && stereotypeChar !== '-';

		const elements: VNode[] = [];
		let currentY = -lineHeight * 0.75;

		if (hasIcon) {
			// Render the icon circle and letter on top, aligned vertically with first line
			const iconGroup = createIcon(width, background, stereotypeChar);
			elements.push(iconGroup);
		}

		lines.forEach((line) => {
			// ---- is horizontal line
			if (/^[-]{4,}$/.test(line)) {
				const y = currentY + lineHeight / 2;
				elements.push(
					<line
						x1={-width / 2}
						y1={y}
						x2={width / 2}
						y2={y}
						class-simple-line={true}
					/>
				);

				currentY += lineHeight;
				return;
			}

			const isBold = line.startsWith('=');
			const content = isBold ? line.slice(1).trim() : line;

			// Normal text push
			elements.push(
				<text
					x={hasIcon ? 10 : 0}
					y={currentY + lineHeight * 0.75}
					style={{ fontWeight: isBold ? 'bold' : 'normal' }}
					class-sprotty-label={true}
					text-anchor="start"
				>
					{content}
				</text>
			);

			// Go to the next line
			currentY += lineHeight;
		});

		return <g>{elements}</g>;
	}
}

@injectable()
export class SequenceMessageDivider extends PolylineEdgeViewWithGapsOnIntersections {
	protected override renderAdditionals(
		edge: GEdge,
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

		const labelPadding = 4;
		const labelLength = (edge.args?.labelWidth as number);
		const label = (edge.args?.label as string);
		const labelWidth = labelLength * 7
		const labelHeight = label.split("<br>").length * 14;

		additionals.push(
			<g>
				{/* Bottom line */}
				<line
					x1={start.x - 10}
					y1={centerY - 6}
					x2={end.x + 10}
					y2={centerY - 6}
					stroke="black"
				/>

				{/* Top line*/}
				<line
					x1={start.x - 10}
					y1={centerY - 9}
					x2={end.x + 10}
					y2={centerY - 9}
					stroke="black"
				/>

				{/* Label */}
				<rect
					x={centerX - labelWidth / 2 - labelPadding}
					y={centerY - labelHeight}
					width={labelWidth + 2 * labelPadding}
					height={labelHeight + labelPadding}
					fill="#5d4949"
					stroke="black"
					stroke-width={2}
				/>
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
		context: RenderingContext
	): VNode[]
	{
		return super.renderAdditionals(edge, segments, context);
	}
}

export interface renderLine {
	startArrowPos: {x: number, y: number};
	endArrowPos: {x: number, y: number};
	angle: number;
	strokeWidth: number;
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
		let circleOffset = circle ? 6 : 8;

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
		
		return additionals;
	}


	private drawMessageLine(start: {x: number, y:number}, end: {x: number, y: number}, additionals: VNode[]): renderLine {
		const dx = end.x - start.x;
		const dy = end.y - start.y;
		const direction = Math.sign(dx) || 1;

		const strokeWidth = this.style === 'bold'   ? 2.5 : 1.5;
		const dashed = this.style === 'dotted' ? '2 2' : undefined;

		// Setting offset according to if needed to start or end earlier
		let lineStartOffset = this.headStart == 'cross' ? -10 : 0;
		let lineEndOffset = this.headEnd == 'cross' ? 10 : 2;

		const lineStartX = start.x - direction * lineStartOffset;
		const lineStartY = start.y;

		const lineEndX = end.x - direction * lineEndOffset;
		const lineEndY = end.y;

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
			x: end.x + (dx === 0 ? 0 : (dx > 0 ? -shift : shift)),
			y: end.y
		}
		const startArrowPos = {
			x: start.x + (dx === 0 ? 0 : (dx > 0 ? shift : -shift)),
			y: start.y
		}

		return {
			startArrowPos,
			endArrowPos,
			angle,
			strokeWidth
		};
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

		const drawStart = {
			x: drawStartX,
			y: drawStartY
		}

		const drawEnd = {
			x: drawEndX,
			y: drawEndY
		}

		const {
			startArrowPos, endArrowPos, angle, strokeWidth
		} = this.drawMessageLine(drawStart, drawEnd, additionals);

		this.drawHead(this.headStart, startArrowPos, angle + 180, "start", this.circleStart, additionals, strokeWidth);
		this.drawHead(this.headEnd, endArrowPos, angle, "end", this.circleEnd, additionals, strokeWidth);

		if (this.circleStart) this.drawCircle({ x: drawStartX, y: drawStartY }, additionals);
		if (this.circleEnd) this.drawCircle({ x: drawEndX, y: drawEndY }, additionals);
	}

	private drawSelfArrow(additionals: VNode[]) {
		const strokeWidth = this.style === 'bold'   ? 2.5 : 1.5;
		const dashed = this.style === 'dotted' ? '2 2' : undefined;

		let crossOffsetStart = this.headStart == 'cross' ? -10 : 0;
		let crossOffsetEnd = this.headEnd == 'cross' ? -10 : -4;
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
		const {
			startArrowPos, endArrowPos, angle, strokeWidth
		} = this.drawMessageLine(this.start, this.end, additionals);

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
					  stroke={this.arrColor}
					  strokeWidth={strokeWidth}
				/>
			);
		}
	}

	private drawCircle(point: Point, additionals: VNode[]) {
		additionals.push(
			<circle
				cx={point.x}
				cy={point.y}
				r={3}
				fill={this.arrColor}
				stroke={this.arrColor}
				strokeWidth={1}
			/>
		)
	}
}

@injectable()
export class AnchorEdgeView extends PolylineEdgeViewWithGapsOnIntersections {
	protected override renderAdditionals(
		edge: GEdge,
		segments: Point[],
		context: RenderingContext,
	): VNode[] {

		const additionals = super.renderAdditionals(edge, segments, context);

		const start = segments[0];
		const end = segments[segments.length - 1];

		const lineStartX = start.x;
		const lineStartY = start.y;
		const lineEndX = start.x;
		const lineEndY = end.y;

		const arrowShape = `M 0 -4 L 10 0 L 0 4 Z`;

		const dx = lineEndX - lineStartX;
		const dy = lineEndY - lineStartY;
		const angle = Math.atan2(dy, dx) * (180 / Math.PI);

		// Draw line
		additionals.unshift(
			<path
				d={`M ${lineStartX} ${lineStartY} L ${lineEndX} ${lineEndY}`}
				stroke="black"
				strokeWidth={1.5}
				markerEnd="none"
				fill="none"
			/>
		);

		// Draw bottom arrow
		additionals.push(
			<path
				d={arrowShape}
				transform={`translate(${lineStartX} ${lineStartY + 10}) rotate(${angle + 180})`}
				fill="black"
				stroke="black"
				strokeWidth={1.5}
			/>
		);

		// Draw top arrow
		additionals.push(
			<path
				d={arrowShape}
				transform={`translate(${lineEndX} ${lineEndY - 10}) rotate(${angle})`}
				fill="black"
				stroke="black"
				strokeWidth={1.5}
			/>
		);

		return additionals;
	}
}

export class ReferenceEdgeView extends GEdgeView {
	protected override renderAdditionals(edge: GEdge, segments: Point[], _context: RenderingContext): VNode[] {
		const additionals = super.renderAdditionals(edge, segments, _context);

		const x1 = edge.args?.x1 as number;
		const x2 = edge.args?.x2 as number;
		const y1 = edge.args?.y1 as number;
		const y2 = edge.args?.y2 as number;

		additionals.unshift(
			<polygon
				points={`${x1},${y1} ${x1},${y1 + 10}
				 		${x1 + 25},${y1 + 10} 
				 		${x1 + 30},${y1 + 5} 
				 		${x1 + 30},${y1}`}
				fill="grey"
				class-simple-line={true}
			/>,

			<text
				x={x1 + 12}
				y={y1 + 9}
				fontSize="10"
				fontWeight="bold"
				fill="white"
			>
				ref
			</text>,

			<rect
				x={x1}
				y={y1}
				width={x2-x1}
				height={y2-y1}
				fill="none"
				class-simple-line={true}
			/>
		);

		return additionals;
	}
}

export class GroupsView extends GEdgeView {
	protected override renderAdditionals(edge: GEdge, segments: Point[], _context: RenderingContext): VNode[] {
		const additionals = super.renderAdditionals(edge, segments, _context);

		const x1 = edge.args?.x1 as number;
		const x2 = edge.args?.x2 as number;
		const y1 = edge.args?.y1 as number;
		const y2 = edge.args?.y2 as number;
		const width = edge.args?.labelWidth as number;
		const backColor = edge.args?.backColor as string;
		const elementColor = edge.args?.elementColor as string;

		const separatorsRaw = edge.args?.separators;
		const separators: number[] = Array.isArray(separatorsRaw)
			? separatorsRaw.map(Number).filter((n): n is number => !isNaN(n))
			: [];

		additionals.unshift(
			<rect
				x={x1}
				y={y1}
				width={x2 - x1}
				height={y2 - y1}
				fill={backColor}
				opacity={0.5}
			/>,

			<rect
				x={x1}
				y={y1}
				width={x2 - x1}
				height={y2 - y1}
				fill="none"
				class-simple-line={true}
			/>,

			<polygon
				points={`${x1},${y1} ${x1},${y1 + 11}
							${x1 + width},${y1 + 11} 
							${x1 + width + 5},${y1 + 5} 
							${x1 + width + 5},${y1}`}
				fill={elementColor}
				class-simple-line={true}
			/>
		);

		// Render the separator lines ELSE/ALSO as dashed
		for (const y of separators) {
			additionals.push(
				<line
					x1={x1}
					x2={x2}
					y1={y - 3}
					y2={y - 3}
					class-simple-line={true}
					stroke-dasharray="4 2"
				/>
			);
		}
		return additionals;
	}
}

export class NoteEdgeView extends GEdgeView {
	protected override renderAdditionals(edge: GEdge, segments: Point[], _context: RenderingContext): VNode[] {
		const additionals = super.renderAdditionals(edge, segments, _context);

		const x1 = edge.args?.x1 as number;
		const x2 = edge.args?.x2 as number;
		const y1 = edge.args?.y1 as number;
		const y2 = edge.args?.y2 as number;

		const color = edge.args?.color as string;
		const shape = edge.args?.shape as string;

		if (shape == 'NORMAL') {
			additionals.unshift(
				<polygon
					points={`${x1},${y1} ${x2 - 7},${y1}
							${x2},${y1 + 7} ${x2},${y2}
							${x1},${y2}`}
					fill={color}
					class-simple-line={true}
				/>,

				<line
					x1={x2 - 7}
					y1={y1}
					x2={x2 - 7}
					y2={y1 + 7}
					class-simple-line={true}
				/>,

				<line
					x1={x2 - 7}
					y1={y1 + 7}
					x2={x2}
					y2={y1 + 7}
					class-simple-line={true}
				/>
			);
		}

		if (shape == 'BOX') {
			additionals.unshift(
				<rect
					x={x1}
					y={y1}
					width={x2 - x1}
					height={y2 - y1}
					fill={color}
					class-simple-line={true}
				/>
			)
		}

		if (shape == 'HEXAGONAL') {
			additionals.unshift(
				<polygon
					points={`${x1},${(y1 + y2) / 2} ${x1 + 7},${y1}
							${x2 - 7},${y1} ${x2},${(y1 + y2) / 2}
							${x2 - 7},${y2} ${x1 + 7},${y2}`}
					fill={color}
					class-simple-line={true}
				/>
			)
		}

		return additionals;
	}
}