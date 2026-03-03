import { GEdge, GNode, IViewArgs, Point, RenderingContext } from '@eclipse-glsp/client';

export const EDGE_CONFIG = {
    handle: {radius: 5, hitRadius: 10},
    parallel: {spacing: 15},
    selfLoop: {maxSpread: 25, maxBulge: 40, minBulge: 20},
    curve: {offsetRatio: 0.3},
    qualifier: {padding: 4, lineHeight: 14, charWidth: 6.5, gap: 0},
    noteLink: {baseWidth: 12},
} as const;

export const ARROW_PATHS: Record<string, string> = {
    EXTENDS: 'M0,-4 L8,0 L0,4 Z',
    COMPOSITION: 'M0,0 L4,-4 L8,0 L4,4 Z',
    AGREGATION: 'M0,0 L4,-4 L8,0 L4,4 Z',
    ARROW: 'M0,-4 L8,0 M0,4 L8,0, M0,0 L8,0',
    SQUARE: 'M0,-4 L8,-4 L8,4 L0,4 Z',
    NOT_NAVIGABLE: 'M-4,-4 L4,4 M4,-4 L-4,4',
    CROWFOOT: 'M0,0 L8,-4 M0,0 L8,0 M0,0 L8,4',
    PLUS: 'M0,-4 A 4,4 0 1,1 0,4 A 4,4 0 1,1 0,-4 Z M0,-4 L0,4 M-4,0 L4,0',
};

export const ARROW_SIZES: Record<string, number> = {
    EXTENDS: 8,
    COMPOSITION: 8,
    AGREGATION: 8,
    ARROW: 8,
    SQUARE: 4,
    NOT_NAVIGABLE: 10,
    CROWFOOT: 8,
    PLUS: 4,
    none: 0,
};

export const FILLED_ARROWS = new Set(['COMPOSITION']);
export type LineStyle = 'normal' | 'DASHED' | 'DOTTED';
export type ArrowHead = keyof typeof ARROW_SIZES | 'none';

export interface ClassEdgeArgs {
    style: LineStyle;
    headStart: ArrowHead;
    headEnd: ArrowHead;
    thickness: number;
    color: string;
    sourceMember?: string;
    targetMember?: string;
    sourceQualifier?: string;
    targetQualifier?: string;
    parallelIndex?: number;
    parallelTotal?: number;
    selfLoopIndex?: number;
    selfLoopTotal?: number;
}

export interface LinkRenderContext {
    edge: GEdge;
    source: GNode;
    target: GNode;
    args: ClassEdgeArgs;
    renderingContext: RenderingContext;
    viewArgs?: IViewArgs;
}

export function getEdgeArgs(edge: GEdge): ClassEdgeArgs {
    const args = (edge.args ?? {}) as Partial<ClassEdgeArgs>;
    return {
        style: args.style ?? 'normal',
        headStart: args.headStart ?? 'none',
        headEnd: args.headEnd ?? 'none',
        thickness: args.thickness ?? 1.0,
        color: args.color ?? '#000000',
        sourceMember: args.sourceMember,
        targetMember: args.targetMember,
        sourceQualifier: args.sourceQualifier,
        targetQualifier: args.targetQualifier,
        parallelIndex: args.parallelIndex,
        parallelTotal: args.parallelTotal,
        selfLoopIndex: args.selfLoopIndex,
        selfLoopTotal: args.selfLoopTotal,
    };
}

export function isSelfLoop(edge: GEdge): boolean {
    const source = edge.source as GNode | undefined;
    const target = edge.target as GNode | undefined;

    return !!(source && target && source.id === target.id);
}

export function isMemberLink(args: ClassEdgeArgs): boolean {
    return !!(args.sourceMember || args.targetMember);
}

export function isParallelLink(args: ClassEdgeArgs): boolean {
    return args.parallelTotal !== undefined && args.parallelTotal > 1;
}

export function isDiamondEntity(node: GNode): boolean {
    return node.type === 'entity:diamond';
}