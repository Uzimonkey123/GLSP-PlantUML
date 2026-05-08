/*
 * File: LinkWriter.java
 * Author: Norman Babiak
 * Description: Writes modified links back to source
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class LinkWriter {

    private final WriterContext ctx;

    public LinkWriter(WriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Rewrites all links that have been modified or whose entities were renamed
     */
    public void write() {
        for (ClassLink link : ctx.getModel().links) {
            boolean linkModified = link.isModified() || link.getMessage().isModified()
                    || link.getQuantifier1().isModified() || link.getQuantifier2().isModified();
            boolean entityModified = link.getEntity1().isModified() || link.getEntity2().isModified();

            if (!linkModified && !entityModified) continue;

            int start = link.getSourceLineStart();
            int end = link.getSourceLineEnd();
            if (start == -1) continue;

            String indent = extractIndentation(ctx.getSourceLines().get(start));

            // Handle association class links specially
            if (isAssociationLink(link)) {
                writeAssociationLink(link, indent);
                continue;
            }

            if (!linkModified) {
                // Only entity was renamed
                rewriteLinkEntityReferences(link, start);

            } else {
                ctx.changeLine(start, end, List.of(applyIndentation(buildLinkLine(link), indent)));
            }
        }
    }

    private boolean isAssociationLink(ClassLink link) {
        return link.getEntity1().getType().equals("ASSOCIATION_POINT") || link.getEntity2().getType().equals("ASSOCIATION_POINT");
    }

    /**
     * Rewrites an association class link by finding the two real entities connected through the association point
     */
    private void writeAssociationLink(ClassLink link, String indent) {
        // Find which end is the invisible association point
        ClassEntity assoc = link.getEntity1().getType().equals("ASSOCIATION_POINT")
                ? link.getEntity1() : link.getEntity2();

        List<ClassLink> assocLinks = ctx.getModel().links.stream()
                .filter(l -> l.getEntity1() == assoc || l.getEntity2() == assoc)
                .toList();

        // Extract the two real entities connected through the association point
        List<ClassEntity> realEntities = new ArrayList<>();
        for (ClassLink l : assocLinks) {
            if (l.getEntity1() != assoc && !realEntities.contains(l.getEntity1()))
                realEntities.add(l.getEntity1());
            if (l.getEntity2() != assoc && !realEntities.contains(l.getEntity2()))
                realEntities.add(l.getEntity2());
        }

        if (realEntities.size() < 2) return;

        ClassEntity a = realEntities.get(0);
        ClassEntity b = realEntities.get(1);
        ClassLabel q1 = assocLinks.get(0).getQuantifier1();
        ClassLabel q2 = assocLinks.get(1).getQuantifier2();
        ClassLabel msg = link.getMessage();

        int assocLine = findAssociationLine(a, b);
        if (assocLine != -1) {
            String line = buildLinkLineFromEntities(a, b, q1, q2, msg, link);
            ctx.changeLine(assocLine, assocLine, List.of(applyIndentation(line, indent)));
        }
    }

    /**
     * Finds the source line containing the "(EntityA, EntityB)" association class syntax
     */
    private int findAssociationLine(ClassEntity a, ClassEntity b) {
        List<String> sourceLines = ctx.getSourceLines();

        for (int i = 0; i < sourceLines.size(); i++) {
            String line = sourceLines.get(i);
            if (line.contains("(" + a.getOriginalName() + ", " + b.getOriginalName() + ")")
                    && (line.contains("..") || line.contains("."))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Updates only the entity name references in a link line without rebuilding the full arrow syntax
     */
    private void rewriteLinkEntityReferences(ClassLink link, int lineNum) {
        String current = ctx.getEffectiveLine(lineNum);

        Map<String, String> replacements = new TreeMap<>(
                (a, b) -> Integer.compare(b.length(), a.length()));

        for (ClassEntity entity : new ClassEntity[]{link.getEntity1(), link.getEntity2()}) {
            if (!entity.isModified()) continue;

            String oldName = entity.getOriginalName();
            String newToken = ctx.getNewToken(entity);

            if (!oldName.equals(newToken)) replacements.put(oldName, newToken);
        }

        if (replacements.isEmpty()) return;

        // Split at : to only replace in the entity reference part, not the label
        int labelIdx = current.indexOf(" : ");
        String entityPart = labelIdx >= 0 ? current.substring(0, labelIdx) : current;
        String labelPart = labelIdx >= 0 ? current.substring(labelIdx) : "";

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            // Dot included in lookaround because qualified names (com.pkg.Entity) should match as a unit
            String regex = "(?<![A-Za-z0-9_.])" + Pattern.quote(entry.getKey()) + "(?![A-Za-z0-9_.])";
            entityPart = entityPart.replaceAll(regex, Matcher.quoteReplacement(entry.getValue()));
        }

        String updated = entityPart + labelPart;
        if (!updated.equals(current)) {
            ctx.changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    /**
     * Builds a complete link line from a ClassLink: entity tokens, member refs, qualifiers, arrow, quantifiers, and label
     */
    private String buildLinkLine(ClassLink link) {
        StringBuilder sb = new StringBuilder();
        String e1Token = getLinkTokenFromLine(link.getEntity1(), link.getSourceLineStart());
        String e2Token = getLinkTokenFromLine(link.getEntity2(), link.getSourceLineStart());

        // Append member reference
        String srcMember = link.getSourceMember();
        if (isNotEmpty(srcMember) && !e1Token.contains("::")) {
            String newRef = WriterContext.findNewMemberRef(link.getEntity1(), srcMember);
            String ref = (newRef != null) ? formatMemberRef(newRef) : srcMember;
            e1Token = e1Token + "::" + ref;
        }

        String tgtMember = link.getTargetMember();
        if (isNotEmpty(tgtMember) && !e2Token.contains("::")) {
            String newRef = WriterContext.findNewMemberRef(link.getEntity2(), tgtMember);
            String ref = (newRef != null) ? formatMemberRef(newRef) : tgtMember;
            e2Token = e2Token + "::" + ref;
        }

        String srcQual = link.getSourceQualifier();
        if (isNotEmpty(srcQual)) {
            sb.append(e1Token).append(" [").append(srcQual).append("]");

        } else {
            sb.append(e1Token);
        }

        String q1 = link.getQuantifier1().getLabel();
        if (isNotEmpty(q1)) {
            sb.append(" \"").append(q1).append("\"");
        }

        sb.append(" ");
        // Lollipop interfaces use special syntax
        if (link.getEntity1().getType().equals("LOLLIPOP") || link.getEntity2().getType().equals("LOLLIPOP")) {
            sb.append("()-");

        } else {
            sb.append(buildClassArrow(link));
        }
        sb.append(" ");

        String q2 = link.getQuantifier2().getLabel();
        if (isNotEmpty(q2)) {
            sb.append("\"").append(q2).append("\" ");
        }

        String tgtQual = link.getTargetQualifier();
        if (isNotEmpty(tgtQual)) {
            sb.append("[").append(tgtQual).append("] ").append(e2Token);

        } else {
            sb.append(e2Token);
        }

        String label = link.getMessage().getLabel();
        if (isNotEmpty(label)) sb.append(" : ").append(label);

        return sb.toString();
    }

    /**
     * Builds a link line from two entities directly, used for association class rewrites
     */
    private String buildLinkLineFromEntities(ClassEntity e1, ClassEntity e2,
                                             ClassLabel q1, ClassLabel q2,
                                             ClassLabel message, ClassLink link) {
        StringBuilder sb = new StringBuilder();
        String e1Token = ctx.getEntityToken(e1);
        String e2Token = ctx.getEntityToken(e2);

        if (q1 != null && isNotEmpty(q1.getLabel())) {
            sb.append(e1Token).append(" \"").append(q1.getLabel()).append("\"");

        } else {
            sb.append(e1Token);
        }

        sb.append(" ").append(buildClassArrow(link)).append(" ");

        if (q2 != null && isNotEmpty(q2.getLabel())) {
            sb.append("\"").append(q2.getLabel()).append("\" ");
        }

        sb.append(e2Token);

        if (message != null && isNotEmpty(message.getLabel())) {
            sb.append(" : ").append(message.getLabel());
        }

        return sb.toString();
    }

    /**
     * Builds the arrow string from decorators, line style, color, thickness, and bracket overrides
     */
    private String buildClassArrow(ClassLink link) {
        StringBuilder sb = new StringBuilder();

        sb.append(decoratorSymbol(link.getDecorator2(), true));
        String lineChar = link.getType().contains("DOTTED") || link.getType().contains("DASHED")
                ? "." : "-";

        // Check if the original line had bracket style, if so, preserve it
        String originalLine = ctx.getEffectiveLine(link.getSourceLineStart());
        Pattern bracketPattern = Pattern.compile("-\\[(.*?)]->|\\.\\.\\[(.*?)]\\.|-\\[(.*?)]-");
        Matcher m = bracketPattern.matcher(originalLine);

        String bracket = null;
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) {
                    bracket = m.group(i);

                    break;
                }
            }
        }

        if (isNotEmpty(bracket)) {
            sb.append(lineChar).append("[").append(bracket).append("]").append(lineChar);

        } else {
            String color = link.getColor();
            boolean hasColor = isNotEmpty(color) && !color.equals("#000000");
            double thickness = link.getThickness();
            boolean hasThickness = thickness != 1.0;

            // Build bracket from model properties
            if (hasColor || hasThickness) {
                sb.append(lineChar).append("[");
                if (hasColor) sb.append(color);
                if (hasThickness) {
                    if (hasColor) sb.append(",");
                    sb.append("thickness=").append((int) thickness);
                }
                sb.append("]").append(lineChar);

            } else {
                sb.append(lineChar.repeat(Math.max(0, link.getLength())));
            }
        }

        sb.append(decoratorSymbol(link.getDecorator1(), false));
        return sb.toString();
    }

    /**
     * Resolves the entity token from the original source line, preserving namespace prefixes and member refs
     */
    private String getLinkTokenFromLine(ClassEntity entity, int lineNum) {
        String line = ctx.getEffectiveLine(lineNum);

        String oldName = Pattern.quote(entity.getOriginalName());
        // Match optional namespace prefix + entity name + ::member
        Pattern pattern = Pattern.compile(
                "([\\w.]*\"?" + oldName + "\"?)" + "(?:::(\"[^\"]+\"|[\\w]+))?");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String tokenInLine = matcher.group(1);
            String memberRef = matcher.group(2);

            String replacement = ctx.getNewToken(entity);
            String origName = entity.getOriginalName();
            String sep = ctx.detectSeparator();

            // Name conflict check if the token in the source has a different namespace, meaning different entity, don't change
            if (ctx.hasNameConflict(entity)) {
                String tokenBare = tokenInLine.replace("\"", "");
                if (tokenBare.contains(sep)) {
                    String tokenPrefix = tokenBare.substring(0, tokenBare.lastIndexOf(sep));
                    String entityNs = ctx.getEntityNamespaceMap().getOrDefault(entity.getId(), "");

                    if (!tokenPrefix.equals(entityNs)) {
                        String result = tokenInLine;
                        if (memberRef != null) result += "::" + memberRef;
                        return result;
                    }
                }
            }

            String result;
            // Preserve namespace prefix for qualified names
            if (origName.contains(sep)) {
                String prefix = origName.substring(0, origName.lastIndexOf(sep) + sep.length());
                result = tokenInLine.replace(origName, prefix + replacement);

            } else {
                result = tokenInLine.replace(origName, replacement);
            }

            if (memberRef != null) {
                String newRef = WriterContext.findNewMemberRef(entity, memberRef);
                result += "::" + (newRef != null ? formatMemberRef(newRef) : memberRef);
            }

            return result;
        }

        return ctx.getEntityToken(entity);
    }
}
