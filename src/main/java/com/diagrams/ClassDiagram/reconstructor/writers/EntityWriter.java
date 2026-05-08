/*
 * File: EntityWriter.java
 * Author: Norman Babiak
 * Description: Writes modified entities back to source
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class EntityWriter {

    private final WriterContext ctx;

    public EntityWriter(WriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Iterates over all modified entities and rewrites their declarations, updates name references, and rebuilds standalone member lines
     */
    public void write() {
        for (ClassEntity entity : ctx.getModel().entities) {
            if (!entity.isModified()) continue;
            if (!entity.hasLine()) continue;
            if ("NOTE".equals(entity.getType())) continue;

            writeEntityDeclaration(entity);
            updateReferencesForEntity(entity);
            writeStandaloneMembers(entity);
        }
    }

    /**
     * Rewrites the entity declaration line based on whether it's inline, block, or bare
     */
    private void writeEntityDeclaration(ClassEntity entity) {
        int start = entity.getSourceLineStart();
        int end = entity.getSourceLineEnd();

        ClassLineMapper.LineType startType = ctx.getLineMapper().getLineInfo(start).type;

        if (startType == ClassLineMapper.LineType.ENTITY_INLINE) {
            ctx.changeLine(start, end, List.of(buildEntityInline(entity)));

        } else if (startType == ClassLineMapper.LineType.ENTITY_DECLARATION) {
            boolean hadBlock = ctx.getLineMapper().getLineInfo(start).originalText.contains("{")
                    || start != end;

            if (hadBlock) {
                ctx.changeLine(start, end, buildEntityBlock(entity));

            } else {
                ctx.changeLine(start, end, List.of(buildEntityBare(entity)));
            }
        }
    }

    /**
     * Builds a bare entity declaration without a body block
     */
    private String buildEntityBare(ClassEntity entity) {
        String indent = extractIndentation(entity.getRawSourceText());
        return applyIndentation(buildDeclarationHeader(entity), indent);
    }

    /**
     * Builds an inline entity with all members on one line inside { }
     */
    private String buildEntityInline(ClassEntity entity) {
        String indent = extractIndentation(entity.getRawSourceText());
        StringBuilder sb = new StringBuilder(buildDeclarationHeader(entity));

        // All members on one line separated by spaces
        sb.append(" {");
        for (EntityMethod m : entity.getRawBody()) {
            sb.append(" ").append(buildMemberLine(m));
        }
        sb.append(" }");

        return applyIndentation(sb.toString(), indent);
    }

    /**
     * Builds a multi-line entity block with one member per line
     */
    private List<String> buildEntityBlock(ClassEntity entity) {
        String indent = extractIndentation(entity.getRawSourceText());
        List<String> lines = new ArrayList<>();

        lines.add(applyIndentation(buildDeclarationHeader(entity) + " {", indent));

        for (EntityMethod member : entity.getRawBody()) {
            lines.add(applyIndentation("  " + buildMemberLine(member), indent));
        }

        lines.add(applyIndentation("}", indent));
        return lines;
    }

    /**
     * Builds the declaration header: visibility, keyword, name/alias, generics, stereotype, inheritance, and background
     */
    private String buildDeclarationHeader(ClassEntity entity) {
        StringBuilder sb = new StringBuilder();
        String vis = entity.getVisibility();

        if (isNotEmpty(vis)) {
            char symbol = visibilityToSymbol(vis);
            if (symbol != 0) sb.append(symbol);
        }

        sb.append(entityTypeToKeyword(entity.getType(), entity.getRawSourceText()));
        sb.append(" ");

        String name = entity.getName();
        String alias = entity.getAlias();
        boolean needsAlias = isNotEmpty(alias) && !alias.equals(name);
        boolean nameNeedsQuotes = !name.matches("[A-Za-z0-9_<>, ]+") || name.contains(" ");

        if (needsAlias) {
            if (nameNeedsQuotes) {
                sb.append("\"").append(name).append("\" as ").append(alias);

            } else {
                sb.append(alias).append(" as \"").append(name).append("\"");
            }

        } else {
            sb.append(nameNeedsQuotes ? "\"" + name + "\"" : name);
        }

        String generic = entity.getGeneric();
        if (isNotEmpty(generic)) {
            sb.append("<").append(generic).append(">");
        }

        if (entity.isStereotype()) {
            appendStereotype(sb, entity);
        }

        String raw = entity.getRawSourceText();
        String inheritance = extractInheritance(raw);
        // Skip inheritance if entity has generics
        if (inheritance != null && generic.isEmpty()) {
            sb.append(" ").append(inheritance);
        }

        String bg = entity.getExplicitBackground();
        if (isNotEmpty(bg)) {
            sb.append(" ").append(bg);
        }

        return sb.toString();
    }

    /**
     * Appends the stereotype (<< (C,color) name >>) to the declaration
     */
    private void appendStereotype(StringBuilder sb, ClassEntity entity) {
        sb.append(" << ");
        char c = entity.getStereotypeChar();
        String sColor = entity.getStereotypeColor();

        if (c != ' ' && c != 0) {
            sb.append("(").append(c);
            if (isNotEmpty(sColor)) {
                sb.append(",").append(sColor);
            }
            sb.append(") ");
        }

        String sName = entity.getStereotypeName();
        if (isNotEmpty(sName)) {
            sName = sName.replace("«", "").replace("»", "")
                    .replace("<<", "").replace(">>", "").trim();
            if (!sName.isEmpty()) sb.append(sName);
        }

        sb.append(" >>");
    }

    /**
     * Extracts "extends Foo" or "implements Bar" from the original source line, updating parent names if they were renamed.
     */
    private String extractInheritance(String raw) {
        if (raw == null) return null;

        // Strip everything after { to avoid matching keywords inside the body
        String trimmed = raw.trim().replaceAll("\\{.*", "");

        Matcher m = Pattern.compile("\\b(extends|implements)\\b(.*)", Pattern.CASE_INSENSITIVE)
                .matcher(trimmed);
        if (!m.find()) return null;

        String keyword = m.group(1);
        String tail = m.group(2);

        // Update parent class names if they were renamed in this edit
        for (ClassEntity entity : ctx.getModel().entities) {
            if (!entity.isModified()) continue;
            tail = replaceWordBoundary(tail, entity.getOriginalName(), entity.getName());
        }

        return keyword + tail;
    }

    /**
     * Builds a single member line with optional visibility prefix
     */
    String buildMemberLine(EntityMethod member) {
        StringBuilder sb = new StringBuilder();

        String vis = member.getVisibilityChar();
        if (isNotEmpty(vis)) {
            char symbol = visibilityToSymbol(vis);
            if (symbol != 0) sb.append(symbol).append(" ");
        }

        sb.append(member.getMethodName());
        return sb.toString();
    }

    /**
     * Updates references to a renamed entity in relationship, member, and note lines.
     */
    private void updateReferencesForEntity(ClassEntity entity) {
        for (ClassLineMapper.LineInfo info : ctx.getLineMapper().getLineInfos()) {
            if (info.type == ClassLineMapper.LineType.MEMBER
                    || info.type == ClassLineMapper.LineType.RELATIONSHIP
                    || info.type == ClassLineMapper.LineType.NOTE) {
                updateReferenceLine(info.lineNumber, entity);
            }
        }
    }

    /**
     * Replaces old entity name with new token in a single line, handling namespaces and conflicts
     */
    private void updateReferenceLine(int lineNum, ClassEntity entity) {
        String current = ctx.getEffectiveLine(lineNum);
        String oldName = entity.getOriginalName();
        String newToken = ctx.getNewToken(entity);

        if (oldName.equals(newToken)) return;

        String updated = current;
        String sep = ctx.detectSeparator();

        if (oldName.contains(sep)) {
            String prefix = oldName.substring(0, oldName.lastIndexOf(sep) + sep.length());
            String qualifiedNew = prefix + newToken;

            String regex = "(?<![A-Za-z0-9_])" + Pattern.quote(oldName) + "(?![A-Za-z0-9_])";
            updated = updated.replaceAll(regex, Matcher.quoteReplacement(qualifiedNew));

            regex = "(?<![A-Za-z0-9_])\"" + Pattern.quote(oldName) + "\"(?![A-Za-z0-9_])";
            updated = updated.replaceAll(regex, Matcher.quoteReplacement('"' + qualifiedNew + '"'));

        } else {
            boolean conflicted = ctx.hasNameConflict(entity);

            if (conflicted) {
                updated = replaceQualifiedReference(updated, entity);
            }

            if (ctx.shouldUpdateBareReference(lineNum, entity)) {
                updated = replaceReference(updated, oldName, newToken, conflicted);
            }
        }

        if (!updated.equals(current)) {
            ctx.changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    /**
     * Replaces "namespace.OldName" with "namespace.NewName" for entities inside packages.
     */
    private String replaceQualifiedReference(String current, ClassEntity entity) {
        String entityNs = ctx.getEntityNamespaceMap().getOrDefault(entity.getId(), "");
        if (entityNs.isEmpty()) return current;

        String oldName = entity.getOriginalName();
        String newToken = ctx.getNewToken(entity);
        if (oldName.equals(newToken)) return current;

        String sep = ctx.detectSeparator();
        String qualifiedOld = entityNs + sep + oldName;
        String qualifiedNew = entityNs + sep + newToken;

        current = replaceWordBoundary(current, qualifiedOld, qualifiedNew);
        current = current.replace('"' + qualifiedOld + '"', '"' + qualifiedNew + '"');

        return current;
    }

    /**
     * Rewrites standalone member declarations that live outside the entity block.
     */
    private void writeStandaloneMembers(ClassEntity entity) {
        List<EntityMethod> members = entity.getRawBody();
        int memberCursor = 0;
        boolean conflicted = ctx.hasNameConflict(entity);

        for (ClassLineMapper.LineInfo info : ctx.getLineMapper().getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.MEMBER) continue;
            // Skip lines in a different namespace when names conflict
            if (conflicted && !ctx.shouldUpdateBareReference(info.lineNumber, entity)) continue;

            String line = ctx.getEffectiveLine(info.lineNumber);
            String trimmed = line.trim();

            String oldName = entity.getOriginalName();
            String alias = entity.getAlias();
            String token = isNotEmpty(alias) ? alias : entity.getName();

            // Check if this member line belongs to this entity
            boolean matchesOwner = trimmed.startsWith(oldName + " :")
                    || trimmed.startsWith("\"" + oldName + "\" :")
                    || trimmed.startsWith(token + " :")
                    || trimmed.startsWith("\"" + token + "\" :");

            if (!matchesOwner) continue;
            if (memberCursor >= members.size()) continue;

            EntityMethod member = members.get(memberCursor++);
            String indent = extractIndentation(line);

            String entityToken = ctx.getEntityToken(entity);
            String origName = entity.getOriginalName();
            String sep = ctx.detectSeparator();

            // Preserve namespace prefix for qualified names
            if (origName.contains(sep)) {
                String prefix = origName.substring(0, origName.lastIndexOf(sep) + sep.length());
                entityToken = prefix + entityToken;
            }

            String rebuilt = entityToken + " : " + buildMemberLine(member);
            ctx.changeLine(info.lineNumber, info.lineNumber, List.of(applyIndentation(rebuilt, indent)));
        }
    }
}
