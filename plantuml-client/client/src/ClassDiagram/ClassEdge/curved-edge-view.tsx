import { GNode, Point } from '@eclipse-glsp/client';
import { VNode } from 'snabbdom';
import { svg } from '@eclipse-glsp/client';

/** @jsx svg */

interface CurveData {
    path: string;
    startTangent: Point;
    endTangent: Point;
}

interface EdgeStyle {
    style: string;
    headStart: string;
    headEnd: string;
    thickness: number;
    color: string;
}

const normalize = (x: number, y: number): Point => {
    const len = Math.hypot(x, y) || 1;
    return { x: x / len, y: y / len };
};

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

    public static calculateMemberAnchorPointClosest(
        entity: GNode,
        otherEntity: GNode,
        memberName: string | null | undefined,
        forceRight?: boolean
    ): Point {
        const memberYOffset = this.getMemberYOffset(entity, memberName);

        // Calculate entity edges
        const entityLeft = entity.position.x;
        const entityRight = entity.position.x + entity.size.width;

        let anchorX: number;

        if (forceRight !== undefined) {
            anchorX = forceRight ? entityRight : entityLeft;

        } else {
            const otherCenterX = otherEntity.position.x + otherEntity.size.width / 2;

            // Determine which side of this entity is closest to the other entity's center
            const distToLeft = Math.abs(entityLeft - otherCenterX);
            const distToRight = Math.abs(entityRight - otherCenterX);

            anchorX = distToRight < distToLeft ? entityRight : entityLeft;
        }

        return {
            x: anchorX,
            y: entity.position.y + memberYOffset
        };
    }

    public static getMemberIndex(entity: GNode, memberName: string | null | undefined): number {
        if (!memberName || memberName.trim() === '') {
            return 0;
        }

        const fieldLabels = entity.children?.filter(child => child.type === 'label:field') || [];
        const methodLabels = entity.children?.filter(child => child.type === 'label:method') || [];

        for (let i = 0; i < fieldLabels.length; i++) {
            const fieldText = (fieldLabels[i] as any).text || '';
            const cleanText = fieldText.replace(/^[+\-#~]\s*/, '').trim();

            if (cleanText.startsWith(memberName + ' ') ||
                cleanText.startsWith(memberName + ':') ||
                cleanText === memberName) {

                return i;
            }
        }

        for (let i = 0; i < methodLabels.length; i++) {
            const methodText = (methodLabels[i] as any).text || '';
            const cleanText = methodText.replace(/^[+\-#~]\s*/, '').trim();

            if (cleanText.startsWith(memberName + '(') ||
                cleanText.startsWith(memberName + ' ') ||
                cleanText === memberName) {

                return fieldLabels.length + i;
            }
        }

        return 0;
    }

    public static determineMemberLinkStyle(
        source: GNode,
        target: GNode,
        sourceMember: string | null | undefined,
        targetMember: string | null | undefined
    ): {
        sourceAnchor: Point;
        targetAnchor: Point;
        needsCurve: boolean;
        flipCurve: boolean;
    } {
        // Get the source member's index to determine alternation
        const memberIndex = this.getMemberIndex(source, sourceMember);

        const sourceCenterX = source.position.x + source.size.width / 2;
        const sourceCenterY = source.position.y + source.size.height / 2;
        const targetCenterX = target.position.x + target.size.width / 2;
        const targetCenterY = target.position.y + target.size.height / 2;

        // Determine if entities are horizontal or vertical
        const dx = Math.abs(targetCenterX - sourceCenterX);
        const dy = Math.abs(targetCenterY - sourceCenterY);

        const sourceLeft = source.position.x;
        const sourceRight = source.position.x + source.size.width;
        const targetLeft = target.position.x;
        const targetRight = target.position.x + target.size.width;

        const hasHorizontalOverlap = !(sourceRight < targetLeft || targetRight < sourceLeft);
        const isVerticallyArranged = hasHorizontalOverlap || dy > dx * 1.5;

        let sourceExitsRight: boolean;
        let targetExitsRight: boolean;
        let needsCurve: boolean;

        if (isVerticallyArranged) {

            const exitRight = memberIndex % 2 === 0;
            sourceExitsRight = exitRight;
            targetExitsRight = exitRight;
            needsCurve = true;

        } else {
            sourceExitsRight = targetCenterX > sourceCenterX;
            targetExitsRight = sourceCenterX > targetCenterX;

            needsCurve = false;
        }

        const sourceAnchor = this.calculateMemberAnchorPointClosest(
            source, target, sourceMember, sourceExitsRight
        );
        const targetAnchor = this.calculateMemberAnchorPointClosest(
            target, source, targetMember, targetExitsRight
        );

        // Determine curve direction
        const sourceY = sourceAnchor.y;
        const targetY = targetAnchor.y;
        let flipCurve: boolean;

        if (isVerticallyArranged) {
            flipCurve = sourceExitsRight;

        } else {
            flipCurve = sourceY < targetY;

            // If entities are roughly at same Y, use member index for variety
            if (Math.abs(sourceY - targetY) < 20) {
                flipCurve = memberIndex % 4 < 2;
            }
        }

        return {
            sourceAnchor,
            targetAnchor,
            needsCurve,
            flipCurve
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
        flip: boolean = false
    ): CurveData {
        const dx = end.x - start.x;
        const dy = end.y - start.y;
        const dist = Math.hypot(dx, dy);

        const sign = flip ? -1 : 1;
        const perp = normalize(-dy * sign, dx * sign);
        const bulge = dist * 0.3;

        const cp1 = {
            x: start.x + dx * 0.25 + perp.x * bulge,
            y: start.y + dy * 0.25 + perp.y * bulge
        };
        const cp2 = {
            x: start.x + dx * 0.75 + perp.x * bulge,
            y: start.y + dy * 0.75 + perp.y * bulge
        };

        return {
            path: `M ${start.x},${start.y} C ${cp1.x},${cp1.y} ${cp2.x},${cp2.y} ${end.x},${end.y}`,
            startTangent: normalize(cp1.x - start.x, cp1.y - start.y),
            endTangent: normalize(end.x - cp2.x, end.y - cp2.y)
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
                stroke-dasharray={
                    style.style === "DASHED"
                        ? "5,5"
                        : style.style === "DOTTED"
                            ? "1,5"
                            : "none"
                }
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