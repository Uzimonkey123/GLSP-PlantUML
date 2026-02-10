import { injectable } from 'inversify';
import { VNode } from 'snabbdom';
import { RenderingContext, ShapeView, IView } from '@eclipse-glsp/client';
import { GCompartment } from '@eclipse-glsp/client';

/** @jsx svg */
import { svg } from '@eclipse-glsp/client';

@injectable()
export class PackageFolderView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width = model.size?.width  ?? 200;
        const height = model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel = (model as any).args?.isTopLevel === 'true';
        const tabH = parseInt((model as any).args?.headerHeight ?? '30');
        const tabW= parseInt((model as any).args?.labelWidth   ?? '100');

        const stroke= "black";
        const strokeWidth = isTopLevel ? '2' : '1.5';
        const r= 5;
        const tab= `M 0,${tabH} L 0,${r} Q 0,0 ${r},0 L ${tabW - r},0 Q ${tabW},0 ${tabW},${r} L ${tabW},${tabH}`;

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <rect
                    x={0} y={tabH} width={width} height={height - tabH}
                    rx={r} ry={r}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />
                <path
                    d={tab}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />
                <line
                    x1={0} y1={tabH} x2={width} y2={tabH}
                    style={{ stroke: stroke, strokeWidth: strokeWidth }}
                />
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageRectangleView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width= model.size?.width  ?? 200;
        const height= model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel= (model as any).args?.isTopLevel === 'true';

        const stroke= "black";
        const strokeWidth= isTopLevel ? '2' : '1.5';

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <rect
                    x={0} y={0} width={width} height={height}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />

                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageFrameView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width= model.size?.width  ?? 200;
        const height= model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel= (model as any).args?.isTopLevel === 'true';
        const badgeW= parseInt((model as any).args?.labelWidth ?? '80');
        const badgeH= parseInt((model as any).args?.headerHeight ?? '22');

        const stroke = "black";
        const strokeWidth= isTopLevel ? '2' : '1.5';
        const notch= 8;
        const badge= `M 0,0 L ${badgeW},0 L ${badgeW},${badgeH - notch} L ${badgeW - notch},${badgeH} L 0,${badgeH} Z`;

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <rect
                    x={0} y={0} width={width} height={height}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />
                <path
                    d={badge}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageNodeView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width= model.size?.width  ?? 200;
        const height= model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel= (model as any).args?.isTopLevel === 'true';
        const headerH= parseInt((model as any).args?.headerHeight ?? '30');

        const stroke= "black";
        const strokeWidth= isTopLevel ? '2' : '1.5';

        const depth= 14;
        const frontW= width - depth;
        const frontH= height - depth;
        const topFace= `M 0,${depth} L ${depth},0 L ${width},0 L ${frontW},${depth} Z`;
        const rightFace= `M ${frontW},${depth} L ${width},0 L ${width},${frontH} L ${frontW},${frontH + depth} Z`;

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <path d={topFace}   style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }} />
                <path d={rightFace} style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }} />
                <rect
                    x={0} y={depth} width={frontW} height={frontH}
                    style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }}
                />
                <line
                    x1={0} y1={depth + headerH} x2={frontW} y2={depth + headerH}
                    style={{ stroke: stroke, strokeWidth: '1' }}
                />
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageDatabaseView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width= model.size?.width  ?? 200;
        const height= model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel= (model as any).args?.isTopLevel === 'true';

        const stroke = "black";
        const strokeWidth = isTopLevel ? '2' : '1.5';

        const rx= width / 2;
        const ry= 14;
        const cx= width / 2;
        const bodyY = ry;
        const bodyH= height - ry * 2;

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <rect
                    x={0} y={bodyY} width={width} height={bodyH}
                    style={{ fill: background, stroke: 'none' }}
                />
                <line x1={0}     y1={bodyY} x2={0}     y2={bodyY + bodyH} style={{ stroke: stroke, strokeWidth: strokeWidth }} />
                <line x1={width} y1={bodyY} x2={width} y2={bodyY + bodyH} style={{ stroke: stroke, strokeWidth: strokeWidth }} />
                <ellipse cx={cx} cy={bodyY + bodyH} rx={rx} ry={ry}
                         style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }} />
                <ellipse cx={cx} cy={bodyY} rx={rx} ry={ry}
                         style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }} />
                <ellipse cx={cx} cy={bodyY + ry + 8} rx={rx * 0.92} ry={ry * 0.45}
                         style={{ fill: 'none', stroke: stroke, strokeWidth: '1', strokeDasharray: '4,3' }} />
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageCloudView extends ShapeView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        const width= model.size?.width  ?? 200;
        const height= model.size?.height ?? 150;
        const background = (model as any).args?.background ?? 'none';
        const isTopLevel= (model as any).args?.isTopLevel === 'true';

        const stroke= "black";
        const strokeWidth= isTopLevel ? '2' : '1.5';

        const p = (x: number, y: number) => `${(x * width).toFixed(1)},${(y * height).toFixed(1)}`;
        const cloudPath = [
            `M ${p(0.20, 1.00)}`,
            `C ${p(0.00, 1.00)} ${p(0.00, 0.72)} ${p(0.08, 0.62)}`,
            `C ${p(0.02, 0.54)} ${p(0.02, 0.38)} ${p(0.15, 0.35)}`,
            `C ${p(0.14, 0.14)} ${p(0.38, 0.00)} ${p(0.50, 0.05)}`,
            `C ${p(0.56, 0.00)} ${p(0.70, 0.00)} ${p(0.73, 0.10)}`,
            `C ${p(0.82, 0.02)} ${p(1.00, 0.14)} ${p(0.97, 0.30)}`,
            `C ${p(1.02, 0.36)} ${p(1.02, 0.54)} ${p(0.93, 0.60)}`,
            `C ${p(1.02, 0.72)} ${p(1.00, 1.00)} ${p(0.80, 1.00)}`,
            `Z`,
        ].join(' ');

        return (
            <g class-sprotty-node={true} class-top-level={isTopLevel}>
                <path d={cloudPath} style={{ fill: background, stroke: stroke, strokeWidth: strokeWidth }} />
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}

@injectable()
export class PackageHeaderView implements IView {
    render(model: GCompartment, context: RenderingContext): VNode | undefined {
        return (
            <g>
                {context.renderChildren(model)}
            </g>
        ) as any;
    }
}
