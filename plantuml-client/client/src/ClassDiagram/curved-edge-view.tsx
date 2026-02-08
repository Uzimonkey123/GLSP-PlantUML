import { GNode, Point } from '@eclipse-glsp/client';
import { VNode } from 'snabbdom';
import { svg } from '@eclipse-glsp/client';

/** @jsx svg */

interface CurveData {
    path: string;
    startTangent: Point;
    endTangent: Point;
    cp1: Point;
    cp2: Point;
    midPoint: Point;
    midTangent: Point;
}

interface EdgeStyle {
    style: string;
    headStart: string;
    headEnd: string;
    thickness: number;
    color: string;
}

export class CurvedEdgeRenderer {
    private static curveDirectionMap = new Map<string, boolean>();

    public static getMemberYOffset(entity: GNode, memberName: string | null | undefined): number {
        if (!memberName || memberName.trim() === '') {
            return entity.size.height / 2;
        }

        const lineHeight = 14;
        const padding = 5;

        const nameLabel = entity.children?.find(child => child.type === 'label:entityName');
        const hasStereotype = nameLabel && (nameLabel as any).args?.stereotypeName &&
            (nameLabel as any).args.stereotypeName.length > 0;
        const headerH = hasStereotype ? 44 : 30;

        const fieldLabels = entity.children?.filter(child => child.type === 'label:field') || [];
        for (let i = 0; i < fieldLabels.length; i++) {
            const fieldText = (fieldLabels[i] as any).text || '';
            const cleanText = fieldText.replace(/^[+\-#~]\s*/, '').trim();

            if (cleanText.startsWith(memberName + ' ') ||
                cleanText.startsWith(memberName + ':') ||
                cleanText === memberName) {
                return headerH + padding + (i * lineHeight) + lineHeight / 2;
            }
        }

        const methodLabels = entity.children?.filter(child => child.type === 'label:method') || [];
        const fieldCount = fieldLabels.length;
        const fieldH = fieldCount > 0 ? fieldCount * lineHeight + padding * 2 : 10;

        for (let i = 0; i < methodLabels.length; i++) {
            const methodText = (methodLabels[i] as any).text || '';
            const cleanText = methodText.replace(/^[+\-#~]\s*/, '').trim();

            if (cleanText.startsWith(memberName + '(') ||
                cleanText.startsWith(memberName + ' ') ||
                cleanText === memberName) {
                return headerH + fieldH + padding + (i * lineHeight) + lineHeight / 2;
            }
        }

        return entity.size.height / 2;
    }

    public static calculateMemberAnchorPoint(
        entity: GNode,
        memberName: string | null | undefined,
        curveToRight: boolean
    ): Point {
        const memberYOffset = this.getMemberYOffset(entity, memberName);

        const anchorX = curveToRight
            ? entity.position.x + entity.size.width  // Right edge
            : entity.position.x;

        return {
            x: anchorX,
            y: entity.position.y + memberYOffset
        };
    }

    public static determineCurveDirection(
        source: GNode,
        target: GNode,
        sourceMember: string | null | undefined,
        targetMember: string | null | undefined
    ): { flipCurve: boolean; curveToRight: boolean } {
        const sourceId = source.id;
        const targetId = target.id;

        // Create consistent ordering
        const [id1, id2] = sourceId < targetId ? [sourceId, targetId] : [targetId, sourceId];
        const pairKey = `${id1}-${id2}`;

        // Hash the pair key to get a deterministic direction
        const hash = pairKey.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
        const baseDirection = hash % 2 === 0;

        // For individual links between same pair, check if we already assigned a direction
        const linkKey = `${source.id}-${target.id}-${sourceMember || ''}-${targetMember || ''}`;
        if (!this.curveDirectionMap.has(linkKey)) {
            // Count existing links between this pair
            const existingLinksCount = Array.from(this.curveDirectionMap.keys())
                .filter(key => key.startsWith(pairKey))
                .length;

            // Alternate direction based on count
            this.curveDirectionMap.set(
                linkKey,
                baseDirection ? (existingLinksCount % 2 === 0) : (existingLinksCount % 2 !== 0)
            );
        }
        const flipCurve = this.curveDirectionMap.get(linkKey) || false;

        // Calculate center positions for direction determination
        const sourceCenterY = source.position.y + source.size.height / 2;
        const targetCenterY = target.position.y + target.size.height / 2;

        const dy = targetCenterY - sourceCenterY;

        let perpX = -dy;

        if (flipCurve) {
            perpX = -perpX;
        }

        const curveToRight = perpX > 0;

        return { flipCurve, curveToRight };
    }

    public static calculateCurveWithTangents(
        start: Point,
        end: Point,
        flipDirection: boolean = false
    ): CurveData {
        const dx = end.x - start.x;
        const dy = end.y - start.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        let perpX = -dy;
        let perpY = dx;

        if (flipDirection) {
            perpX = -perpX;
            perpY = -perpY;
        }

        const perpLength = Math.sqrt(perpX * perpX + perpY * perpY);
        const perpNormX = perpX / perpLength;
        const perpNormY = perpY / perpLength;

        const arcHeight = distance * 0.3;

        // Control points
        const cp1 = {
            x: start.x + dx * 0.25 + perpNormX * arcHeight,
            y: start.y + dy * 0.25 + perpNormY * arcHeight
        };

        const cp2 = {
            x: start.x + dx * 0.75 + perpNormX * arcHeight,
            y: start.y + dy * 0.75 + perpNormY * arcHeight
        };

        // Calculate tangent at start: tangent = 3(cp1 - start)
        const startTangentX = 3 * (cp1.x - start.x);
        const startTangentY = 3 * (cp1.y - start.y);
        const startTangentLength = Math.sqrt(startTangentX * startTangentX + startTangentY * startTangentY);

        // Calculate tangent at end: tangent = 3(end - cp2)
        const endTangentX = 3 * (end.x - cp2.x);
        const endTangentY = 3 * (end.y - cp2.y);
        const endTangentLength = Math.sqrt(endTangentX * endTangentX + endTangentY * endTangentY);

        // Calculate midpoint on bezier curve (t = 0.5)
        const t = 0.5;
        const u = 1 - t;
        const midPoint = {
            x: u * u * u * start.x + 3 * u * u * t * cp1.x + 3 * u * t * t * cp2.x + t * t * t * end.x,
            y: u * u * u * start.y + 3 * u * u * t * cp1.y + 3 * u * t * t * cp2.y + t * t * t * end.y
        };

        // Calculate tangent at midpoint
        const midTangentX = 3 * (u * u * (cp1.x - start.x) + 2 * u * t * (cp2.x - cp1.x) + t * t * (end.x - cp2.x));
        const midTangentY = 3 * (u * u * (cp1.y - start.y) + 2 * u * t * (cp2.y - cp1.y) + t * t * (end.y - cp2.y));
        const midTangentLength = Math.sqrt(midTangentX * midTangentX + midTangentY * midTangentY);

        const path = `M ${start.x},${start.y} C ${cp1.x},${cp1.y} ${cp2.x},${cp2.y} ${end.x},${end.y}`;

        return {
            path,
            startTangent: {
                x: startTangentX / startTangentLength,
                y: startTangentY / startTangentLength
            },
            endTangent: {
                x: endTangentX / endTangentLength,
                y: endTangentY / endTangentLength
            },
            cp1,
            cp2,
            midPoint,
            midTangent: {
                x: midTangentX / midTangentLength,
                y: midTangentY / midTangentLength
            }
        };
    }

    public static renderCurvedPath(
        curveData: CurveData,
        style: EdgeStyle
    ): VNode {
        return (
            <path
                d={curveData.path}
                stroke={style.color}
                stroke-width={style.thickness}
                stroke-dasharray={style.style === "DASHED" ? "5,5" : "none"}
                fill="none"
                class-sprotty-edge={true}
            />
        );
    }

    public static calculateArrowTransforms(
        sourceAnchor: Point,
        targetAnchor: Point,
        curveData: CurveData,
        headStartSize: number,
        headEndSize: number
    ): {
        startArrowPos: Point;
        endArrowPos: Point;
        startAngle: number;
        endAngle: number;
    } {
        const startArrowPos = {
            x: sourceAnchor.x + curveData.startTangent.x * headStartSize,
            y: sourceAnchor.y + curveData.startTangent.y * headStartSize
        };

        const endArrowPos = {
            x: targetAnchor.x - curveData.endTangent.x * headEndSize,
            y: targetAnchor.y - curveData.endTangent.y * headEndSize
        };

        const startAngle = Math.atan2(curveData.startTangent.y, curveData.startTangent.x) * 180 / Math.PI;
        const endAngle = Math.atan2(curveData.endTangent.y, curveData.endTangent.x) * 180 / Math.PI;

        return {
            startArrowPos,
            endArrowPos,
            startAngle,
            endAngle
        };
    }
}