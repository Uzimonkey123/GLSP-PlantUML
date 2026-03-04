package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.utils.NewLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class ClassWriter {

    private final ClassModel model;
    private final File source;
    private final List<String> sourceLines;
    private final Map<Integer, NewLine> newLines;
    private final ClassLineMapper lineMap;

    private Map<Integer, String> lineNamespaceMap;
    private Map<String, String> entityNamespaceMap;

    public ClassWriter(ClassModel model, String sourceUri) throws IOException {
        this.model = model;
        this.source = new File(URI.create(sourceUri));
        this.sourceLines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);
        this.newLines = new HashMap<>();
        this.lineMap = model.getLineMapper();
        this.lineNamespaceMap = null;
        this.entityNamespaceMap = null;
    }

    public void write() throws IOException {
        sourceLines.clear();
        sourceLines.addAll(Files.readAllLines(source.toPath(), StandardCharsets.UTF_8));
        newLines.clear();
        lineNamespaceMap = null;
        entityNamespaceMap = null;

        for (int[] range : model.getLinesToDelete()) {
            changeLine(range[0], range[1], Collections.emptyList());
        }

        model.clearLinesToDelete();

        writeEntities();
        updateInheritanceReferences();
        writeMemberReferences();
        writePackages();
        writeLinks();
        writeNotes();
        writePageDetails();
        applyReplacements();
        saveAtomic();
    }

    private void changeLine(int start, int end, List<String> lines) {
        newLines.put(start, new NewLine(start, end, lines));
    }

    private String getEffectiveLine(int lineNum) {
        if (newLines.containsKey(lineNum)) {
            List<String> pending = newLines.get(lineNum).newLines();
            return pending.isEmpty() ? "" : pending.getFirst();
        }
        if (lineNum >= 0 && lineNum < sourceLines.size()) {
            return sourceLines.get(lineNum);
        }
        return "";
    }

    private String getNewToken(ClassEntity entity) {
        String alias = entity.getAlias();

        return isNotEmpty(alias) ? alias : entity.getName();
    }

    private String getEntityToken(ClassEntity entity) {
        String alias = entity.getAlias();

        if (isNotEmpty(alias)) {
            return alias;
        }

        return quoteIfNeeded(entity.getName());
    }

    private String detectSeparator() {
        for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
            String trimmed = info.originalText.trim();
            if (trimmed.startsWith("set separator")) {
                String sep = trimmed.substring("set separator".length()).trim();
                if (!sep.isEmpty() && !sep.equalsIgnoreCase("none")) return sep;
            }
        }

        return ".";
    }

    private String replaceReferenceLine(String current, ClassEntity entity, boolean strict) {
        return replaceReference(current, entity.getOriginalName(), getNewToken(entity), strict);
    }

    private void writeEntities() {
        for (ClassEntity entity : model.entities) {
            if (!entity.isModified()) continue;
            if (!entity.hasLine()) continue;
            if ("NOTE".equals(entity.getType())) continue;

            int start = entity.getSourceLineStart();
            int end = entity.getSourceLineEnd();

            ClassLineMapper.LineType startType = lineMap.getLineInfo(start).type;

            if (startType == ClassLineMapper.LineType.ENTITY_INLINE) {
                changeLine(start, end, List.of(buildEntityInline(entity)));

            } else if (startType == ClassLineMapper.LineType.ENTITY_DECLARATION) {
                boolean hadBlock = lineMap.getLineInfo(start).originalText.contains("{") || start != end;

                if (hadBlock) {
                    List<String> block = buildEntityBlock(entity);
                    changeLine(start, end, block);

                } else {
                    changeLine(start, end, List.of(buildEntityBare(entity)));
                }
            }

            for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
                if (info.type == ClassLineMapper.LineType.MEMBER
                        || info.type == ClassLineMapper.LineType.RELATIONSHIP
                        || info.type == ClassLineMapper.LineType.NOTE) {
                    updateReferenceLine(info.lineNumber, entity);
                }
            }

            writeStandaloneMembers(entity);
        }
    }

    private void writeStandaloneMembers(ClassEntity entity) {
        List<EntityMethod> members = entity.getRawBody();
        int memberCursor = 0;
        boolean conflicted = hasNameConflict(entity);

        for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.MEMBER) continue;
            if (conflicted && !shouldUpdateBareReference(info.lineNumber, entity)) continue;

            String line = getEffectiveLine(info.lineNumber);
            String trimmed = line.trim();

            String oldName = entity.getOriginalName();
            String alias = entity.getAlias();
            String token = isNotEmpty(alias) ? alias : entity.getName();

            boolean matchesOwner = trimmed.startsWith(oldName + " :")
                                    || trimmed.startsWith("\"" + oldName + "\" :")
                                    || trimmed.startsWith(token + " :")
                                    || trimmed.startsWith("\"" + token + "\" :");

            if (!matchesOwner) continue;
            if (memberCursor >= members.size()) {
                continue;
            }

            EntityMethod member = members.get(memberCursor++);
            String indent = extractIndentation(line);

            String entityToken = getEntityToken(entity);
            String origName = entity.getOriginalName();
            String sep = detectSeparator();
            if (origName.contains(sep)) {
                String prefix = origName.substring(0, origName.lastIndexOf(sep) + sep.length());
                entityToken = prefix + entityToken;
            }

            String rebuilt = entityToken + " : " + buildMemberLine(member);
            changeLine(info.lineNumber, info.lineNumber, List.of(applyIndentation(rebuilt, indent)));
        }
    }

    private String buildMemberLine(EntityMethod member) {
        StringBuilder sb = new StringBuilder();

        String vis = member.getVisibilityChar();
        if (isNotEmpty(vis)) {
            char symbol = visibilityToSymbol(vis);
            if (symbol != 0) sb.append(symbol).append(" ");
        }

        sb.append(member.getMethodName());

        return sb.toString();
    }

    private String buildEntityBare(ClassEntity entity) {
        String indent = extractIndentation(entity.getRawSourceText());
        return applyIndentation(buildDeclarationHeader(entity), indent);
    }

    private String buildEntityInline(ClassEntity entity) {
        String source = entity.getRawSourceText();
        String indent = extractIndentation(source);
        StringBuilder sb = new StringBuilder(buildDeclarationHeader(entity));

        sb.append(" {");
        for (EntityMethod m : entity.getRawBody()) {
            sb.append(" ").append(buildMemberLine(m));
        }
        sb.append(" }");

        return applyIndentation(sb.toString(), indent);
    }

    private List<String> buildEntityBlock(ClassEntity entity) {
        String source = entity.getRawSourceText();
        String indent = extractIndentation(source);

        List<String> lines = new ArrayList<>();
        lines.add(applyIndentation(buildDeclarationHeader(entity) + " {", indent));

        for (EntityMethod member : entity.getRawBody()) {
            lines.add(applyIndentation("  " + buildMemberLine(member), indent));
        }

        lines.add(applyIndentation("}", indent));
        return lines;
    }

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

        String raw = entity.getRawSourceText();
        String inheritance = extractInheritance(raw);
        if (inheritance != null && generic.isEmpty()) {
            sb.append(" ").append(inheritance);
        }

        String bg = entity.getExplicitBackground();
        if (isNotEmpty(bg)) {
            sb.append(" ").append(bg);
        }

        return sb.toString();
    }

    private String extractInheritance(String raw) {
        if (raw == null) return null;

        String trimmed = raw.trim();
        trimmed = trimmed.replaceAll("\\{.*", "");

        Matcher m = Pattern.compile("\\b(extends|implements)\\b(.*)", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (!m.find()) return null;

        String keyword = m.group(1);
        String tail = m.group(2);

        for (ClassEntity entity : model.entities) {
            if (!entity.isModified()) continue;
            tail = replaceWordBoundary(tail, entity.getOriginalName(), entity.getName());
        }

        return keyword + tail;
    }

    private void updateInheritanceReferences() {
        Map<String, String> nameChanges = new HashMap<>();

        for (ClassEntity entity : model.entities) {
            if (!entity.isModified()) continue;

            String oldName = entity.getOriginalName();
            String newName = entity.getName();

            if (!oldName.equals(newName)) {
                nameChanges.put(oldName, newName);
            }
        }

        if (nameChanges.isEmpty()) return;

        for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.ENTITY_DECLARATION
                    && info.type != ClassLineMapper.LineType.ENTITY_INLINE) {
                continue;
            }

            int lineNum = info.lineNumber;
            String line = getEffectiveLine(lineNum);

            Matcher m = Pattern.compile("\\b(extends|implements)\\b(.*)").matcher(line);
            if (!m.find()) continue;

            String beforeInheritance = line.substring(0, m.start());
            String keyword = m.group(1);
            String tail = m.group(2);

            String afterTail = "";
            int braceIdx = tail.indexOf('{');
            if (braceIdx >= 0) {
                afterTail = tail.substring(braceIdx);
                tail = tail.substring(0, braceIdx);
            }

            String originalTail = tail;
            for (Map.Entry<String, String> change : nameChanges.entrySet()) {
                tail = replaceWordBoundary(tail, change.getKey(), change.getValue());
            }

            if (!tail.equals(originalTail)) {
                String updated = beforeInheritance + keyword + tail + afterTail;
                changeLine(lineNum, lineNum, List.of(updated));
            }
        }
    }

    private void updateReferenceLine(int lineNum, ClassEntity entity) {
        String current = getEffectiveLine(lineNum);
        String oldName = entity.getOriginalName();
        String newToken = getNewToken(entity);

        if (oldName.equals(newToken)) return;

        String updated = current;
        String sep = detectSeparator();

        if (oldName.contains(sep)) {
            String prefix = oldName.substring(0, oldName.lastIndexOf(sep) + sep.length());
            String qualifiedNew = prefix + newToken;

            String regex = "(?<![A-Za-z0-9_])" + Pattern.quote(oldName) + "(?![A-Za-z0-9_])";
            updated = updated.replaceAll(regex, Matcher.quoteReplacement(qualifiedNew));

            regex = "(?<![A-Za-z0-9_])\"" + Pattern.quote(oldName) + "\"(?![A-Za-z0-9_])";
            updated = updated.replaceAll(regex, Matcher.quoteReplacement('"' + qualifiedNew + '"'));

        } else {
            boolean conflicted = hasNameConflict(entity);

            if (conflicted) {
                updated = replaceQualifiedReference(updated, entity);
            }

            if (shouldUpdateBareReference(lineNum, entity)) {
                updated = replaceReferenceLine(updated, entity, conflicted);
            }
        }

        if (!updated.equals(current)) {
            changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private String replaceQualifiedReference(String current, ClassEntity entity) {
        String entityNs = getEntityNamespaceMap().getOrDefault(entity.getId(), "");
        if (entityNs.isEmpty()) return current;

        String oldName = entity.getOriginalName();
        String newToken = getNewToken(entity);
        if (oldName.equals(newToken)) return current;

        String sep = detectSeparator();
        String qualifiedOld = entityNs + sep + oldName;
        String qualifiedNew = entityNs + sep + newToken;

        current = replaceWordBoundary(current, qualifiedOld, qualifiedNew);
        current = current.replace('"' + qualifiedOld + '"', '"' + qualifiedNew + '"');

        return current;
    }

    private void writeMemberReferences() {
        Map<ClassEntity, Map<String, String>> entityMemberMaps = new LinkedHashMap<>();
        for (ClassEntity entity : model.entities) {
            if ("NOTE".equals(entity.getType())) continue;

            Map<String, String> refMap = buildMemberRefMap(entity);

            if (!refMap.isEmpty()) {
                entityMemberMaps.put(entity, refMap);
            }
        }

        if (entityMemberMaps.isEmpty()) return;

        for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.RELATIONSHIP
                    && info.type != ClassLineMapper.LineType.NOTE
                    && info.type != ClassLineMapper.LineType.MEMBER) continue;

            String current = getEffectiveLine(info.lineNumber);
            if (!current.contains("::")) continue;

            String updated = current;

            for (var entry : entityMemberMaps.entrySet()) {
                ClassEntity entity = entry.getKey();
                Map<String, String> refMap = entry.getValue();

                for (String token : new String[]{entity.getOriginalName(), entity.getName(), entity.getAlias()}) {
                    if (isNullOrEmpty(token)) continue;
                    updated = replaceMemberRefsForToken(updated, token, refMap);
                }
            }

            if (!updated.equals(current)) {
                changeLine(info.lineNumber, info.lineNumber, List.of(updated));
            }
        }
    }

    private static Map<String, String> buildMemberRefMap(ClassEntity entity) {
        Map<String, String> map = new LinkedHashMap<>();

        for (EntityMethod member : entity.getRawBody()) {
            String oldParsed = parseRawMemberName(member.getOriginalName());
            String oldRef = deriveMemberRef(oldParsed);
            String newRef = deriveMemberRef(member.getMethodName());

            if (oldRef != null && newRef != null && !oldRef.equals(newRef)) {
                map.put(oldRef, newRef);
            }
        }

        return map;
    }

    private static String findNewMemberRef(ClassEntity entity, String oldRef) {
        if (isNullOrEmpty(oldRef)) return null;

        String bareRef = unquote(oldRef);

        for (EntityMethod member : entity.getRawBody()) {
            String oldParsed = parseRawMemberName(member.getOriginalName());
            String oldMemberRef = deriveMemberRef(oldParsed);

            if (bareRef.equals(oldMemberRef)) {
                String newMemberRef = deriveMemberRef(member.getMethodName());
                if (!oldMemberRef.equals(newMemberRef)) {
                    return newMemberRef;
                }

                return null;
            }
        }

        return null;
    }

    private Map<Integer, String> getLineNamespaceMap() {
        if (lineNamespaceMap == null) {
            lineNamespaceMap = buildLineNamespaceMap();
        }

        return lineNamespaceMap;
    }

    private Map<String, String> getEntityNamespaceMap() {
        if (entityNamespaceMap == null) {
            entityNamespaceMap = buildEntityNamespaceMap();
        }

        return entityNamespaceMap;
    }

    private Map<Integer, String> buildLineNamespaceMap() {
        Map<Integer, String> map = new HashMap<>();
        Deque<String> nsStack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        int braceDepth = 0;

        for (int i = 0; i < sourceLines.size(); i++) {
            String trimmed = sourceLines.get(i).trim();

            Matcher m = Pattern.compile("^(?:namespace|package)\\s+\"?([^\"\\s{#]+)\"?").matcher(trimmed);

            if (m.find()) {
                String nsName = m.group(1);
                int opens = countChar(trimmed, '{');
                int closes = countChar(trimmed, '}');

                if (opens > closes) {
                    nsStack.push(nsName);
                    depthStack.push(braceDepth);
                    braceDepth += (opens - closes);
                    map.put(i, nsName);
                    continue;
                }
            }

            int opens = countChar(trimmed, '{');
            int closes = countChar(trimmed, '}');
            braceDepth += opens;
            braceDepth -= closes;

            while (!depthStack.isEmpty() && braceDepth <= depthStack.peek()) {
                nsStack.pop();
                depthStack.pop();
            }

            map.put(i, nsStack.isEmpty() ? "" : nsStack.peek());
        }

        return map;
    }

    private Map<String, String> buildEntityNamespaceMap() {
        Map<String, String> map = new HashMap<>();

        for (ClassEntity entity : model.entities) {
            map.put(entity.getId(), "");
        }

        for (Package pkg : model.packages) {
            mapEntitiesInPackage(pkg, map);
        }

        return map;
    }

    private void mapEntitiesInPackage(Package pkg, Map<String, String> map) {
        for (ClassEntity entity : pkg.getEntities()) {
            map.put(entity.getId(), pkg.getName());
        }

        for (Package child : pkg.getChildPackages()) {
            mapEntitiesInPackage(child, map);
        }
    }

    private boolean hasNameConflict(ClassEntity entity) {
        String name = entity.getOriginalName();
        for (ClassEntity other : model.entities) {
            if (other != entity && name.equals(other.getOriginalName())) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldUpdateBareReference(int lineNum, ClassEntity entity) {
        if (!hasNameConflict(entity)) {
            return true;
        }

        String entityNs = getEntityNamespaceMap().getOrDefault(entity.getId(), "");
        String lineNs = getLineNamespaceMap().getOrDefault(lineNum, "");

        return entityNs.equals(lineNs);
    }

    private void writeLinks() {
        for (ClassLink link : model.links) {
            boolean linkModified = link.isModified() || link.getMessage().isModified()
                    || link.getQuantifier1().isModified() || link.getQuantifier2().isModified();
            boolean entityModified = link.getEntity1().isModified() || link.getEntity2().isModified();

            if (!linkModified && !entityModified) continue;

            int start = link.getSourceLineStart();
            int end = link.getSourceLineEnd();
            if (start == -1) continue;

            String indent = extractIndentation(sourceLines.get(start));

            if (link.getEntity1().getType().equals("ASSOCIATION_POINT") || link.getEntity2().getType().equals("ASSOCIATION_POINT")) {
                ClassEntity assoc = link.getEntity1().getType().equals("ASSOCIATION_POINT")
                        ? link.getEntity1() : link.getEntity2();

                List<ClassLink> assocLinks = model.links.stream()
                        .filter(l -> l.getEntity1() == assoc || l.getEntity2() == assoc)
                        .toList();

                List<ClassEntity> realEntities = new ArrayList<>();
                for (ClassLink l : assocLinks) {
                    if (l.getEntity1() != assoc && !realEntities.contains(l.getEntity1())) realEntities.add(l.getEntity1());
                    if (l.getEntity2() != assoc && !realEntities.contains(l.getEntity2())) realEntities.add(l.getEntity2());
                }

                if (realEntities.size() >= 2) {
                    ClassEntity a = realEntities.get(0);
                    ClassEntity b = realEntities.get(1);

                    ClassLabel q1 = assocLinks.get(0).getQuantifier1();
                    ClassLabel q2 = assocLinks.get(1).getQuantifier2();
                    ClassLabel msg = link.getMessage();

                    int assocLine = -1;
                    for (int i = 0; i < sourceLines.size(); i++) {
                        String line = sourceLines.get(i);
                        if (line.contains("(" + a.getOriginalName() + ", " + b.getOriginalName() + ")") &&
                                line.contains("..") || line.contains(".")) {
                            assocLine = i;
                            break;
                        }
                    }

                    if (assocLine != -1) {
                        String line = buildLinkLine(a, b, q1, q2, msg, link);
                        changeLine(assocLine, assocLine, List.of(applyIndentation(line, indent)));
                    }

                    continue;
                }
            }

            if (!linkModified) {
                rewriteLinkEntityReferences(link, start);

            } else {
                changeLine(start, end, List.of(applyIndentation(buildLinkLine(link), indent)));
            }
        }
    }

    private void rewriteLinkEntityReferences(ClassLink link, int lineNum) {
        String current = getEffectiveLine(lineNum);

        Map<String, String> replacements = new TreeMap<>((a, b) -> Integer.compare(b.length(), a.length()));
        for (ClassEntity entity : new ClassEntity[]{link.getEntity1(), link.getEntity2()}) {
            if (!entity.isModified()) continue;

            String oldName = entity.getOriginalName();
            String newToken = getNewToken(entity);

            if (!oldName.equals(newToken)) replacements.put(oldName, newToken);
        }

        if (replacements.isEmpty()) return;

        int labelIdx = current.indexOf(" : ");
        String entityPart = labelIdx >= 0 ? current.substring(0, labelIdx) : current;
        String labelPart = labelIdx >= 0 ? current.substring(labelIdx) : "";

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String regex = "(?<![A-Za-z0-9_.])" + Pattern.quote(entry.getKey()) + "(?![A-Za-z0-9_.])";
            entityPart = entityPart.replaceAll(regex, Matcher.quoteReplacement(entry.getValue()));
        }

        String updated = entityPart + labelPart;
        if (!updated.equals(current)) {
            changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private String buildLinkLine(ClassLink link) {
        StringBuilder sb = new StringBuilder();
        String e1Token = getLinkTokenFromLine(link.getEntity1(), link.getSourceLineStart());
        String e2Token = getLinkTokenFromLine(link.getEntity2(), link.getSourceLineStart());

        String srcMember = link.getSourceMember();
        if (isNotEmpty(srcMember) && !e1Token.contains("::")) {
            String newRef = findNewMemberRef(link.getEntity1(), srcMember);
            String ref = (newRef != null) ? formatMemberRef(newRef) : srcMember;
            e1Token = e1Token + "::" + ref;
        }

        String tgtMember = link.getTargetMember();
        if (isNotEmpty(tgtMember) && !e2Token.contains("::")) {
            String newRef = findNewMemberRef(link.getEntity2(), tgtMember);
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

    private String buildLinkLine(ClassEntity e1, ClassEntity e2,
                                 ClassLabel q1, ClassLabel q2,
                                 ClassLabel message, ClassLink link) {
        StringBuilder sb = new StringBuilder();
        String e1Token = getEntityToken(e1);
        String e2Token = getEntityToken(e2);

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

    private String getLinkTokenFromLine(ClassEntity entity, int lineNum) {
        String line = getEffectiveLine(lineNum);

        String oldName = Pattern.quote(entity.getOriginalName());
        Pattern pattern = Pattern.compile("([\\w.]*\"?" + oldName + "\"?)" + "(?:::(\"[^\"]+\"|[\\w]+))?");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String tokenInLine = matcher.group(1);
            String memberRef = matcher.group(2);

            String replacement = getNewToken(entity);
            String origName = entity.getOriginalName();
            String sep = detectSeparator();

            if (hasNameConflict(entity)) {
                String tokenBare = tokenInLine.replace("\"", "");
                if (tokenBare.contains(sep)) {
                    String tokenPrefix = tokenBare.substring(0, tokenBare.lastIndexOf(sep));
                    String entityNs = getEntityNamespaceMap().getOrDefault(entity.getId(), "");

                    if (!tokenPrefix.equals(entityNs)) {
                        String result = tokenInLine;
                        if (memberRef != null) result += "::" + memberRef;

                        return result;
                    }
                }
            }

            String result;
            if (origName.contains(sep)) {
                String prefix = origName.substring(0, origName.lastIndexOf(sep) + sep.length());
                result = tokenInLine.replace(origName, prefix + replacement);

            } else {
                result = tokenInLine.replace(origName, replacement);
            }

            if (memberRef != null) {
                String newRef = findNewMemberRef(entity, memberRef);
                result += "::" + (newRef != null ? formatMemberRef(newRef) : memberRef);
            }

            return result;
        }

        return getEntityToken(entity);
    }

    private String buildClassArrow(ClassLink link) {
        StringBuilder sb = new StringBuilder();

        sb.append(decoratorSymbol(link.getDecorator2(), true));
        String lineChar = link.getType().contains("DOTTED") || link.getType().contains("DASHED") ? "." : "-";

        // Check original line for bracket style
        String originalLine = getEffectiveLine(link.getSourceLineStart());
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

            if (hasColor || hasThickness) {
                sb.append(lineChar);
                sb.append("[");
                if (hasColor) sb.append(color);

                if (hasThickness) {
                    if (hasColor) sb.append(",");

                    sb.append("thickness=").append((int) thickness);
                }

                sb.append("]");
                sb.append(lineChar);

            } else {
                sb.append(lineChar.repeat(Math.max(0, link.getLength())));
            }
        }

        sb.append(decoratorSymbol(link.getDecorator1(), false));
        return sb.toString();
    }

    private void writeNotes() {
        for (ClassEntity note : model.notes) {
            if (!note.isModified()) continue;
            if (!note.hasLine()) continue;

            int start = note.getSourceLineStart();
            int end = note.getSourceLineEnd();

            changeLine(start, end, (start != end) ? rebuildMultilineNote(note) : rebuildSingleLineNote(note));
        }
    }

    private List<String> rebuildSingleLineNote(ClassEntity note) {
        String source = getEffectiveLine(note.getSourceLineStart());
        String indent = extractIndentation(source);
        String trimmed = source.trim();

        List<String> lines = new ArrayList<>();

        if (trimmed.matches("(?i)^note\\s+\".*\"\\s+as\\s+.*$")) {
            String newText = note.getName().replace("<br>", "\\n");
            String updatedLine = trimmed.replaceAll("\".*?\"", "\"" + Matcher.quoteReplacement(newText) + "\"");

            lines.add(applyIndentation(updatedLine, indent));

        } else if (trimmed.contains(":")) {
            String header = trimmed.substring(0, trimmed.indexOf(":")).trim();
            String content = note.getName().replaceAll("(\r?\n)+$", "").replace("<br>", "\\n");

            lines.add(applyIndentation(header + ": " + content, indent));

        } else {
            lines.add(applyIndentation(trimmed, indent));
        }

        return lines;
    }

    private List<String> rebuildMultilineNote(ClassEntity note) {

        String headerSource = getEffectiveLine(note.getSourceLineStart());
        String indent = extractIndentation(headerSource);

        String trimmedHeader = headerSource.trim();

        List<String> lines = new ArrayList<>();

        lines.add(applyIndentation(trimmedHeader, indent));

        for (String part : note.getName().split("<br>")) {
            lines.add(applyIndentation(part, indent));
        }

        lines.add(applyIndentation("end note", indent));

        return lines;
    }

    private void writePageDetails() {
        if (model.titleModified) {
            changeLine(model.titleLineStart, model.titleLineEnd,
                    buildPageDetail("title", model.title, model.titleLineStart, model.titleLineEnd));
        }
        if (model.headerModified) {
            changeLine(model.headerLineStart, model.headerLineEnd,
                    buildPageDetail("header", model.header, model.headerLineStart, model.headerLineEnd));
        }
        if (model.footerModified) {
            changeLine(model.footerLineStart, model.footerLineEnd,
                    buildPageDetail("footer", model.footer, model.footerLineStart, model.footerLineEnd));
        }
    }

    private List<String> buildPageDetail(String keyword, String content, int startLine, int endLine) {
        List<String> lines = new ArrayList<>();
        String indent = extractIndentation(sourceLines.get(startLine));

        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            lines.add(applyIndentation(keyword, indent));

            for (String part : content.split("<br>")) {
                lines.add(applyIndentation(part, indent));
            }

            lines.add(applyIndentation("end " + keyword, indent));

        } else {
            String text = content.replace("<br>", "\\n");
            lines.add(applyIndentation(keyword + " " + text, indent));
        }

        return lines;
    }

    private void writePackages() {
        List<Package> modified = model.packages.stream()
                .filter(Package::isModified)
                .sorted(Comparator.comparingInt(this::getDepth).reversed())
                .toList();

        for (Package pkg : modified) {
            if (!pkg.isModified()) continue;
            if (pkg.hasLine()) {
                rewritePackageDeclaration(pkg);
            }

            String oldPath = buildFullPath(pkg, true);
            String newPath = buildFullPath(pkg, false);
            if (oldPath.equals(newPath)) continue;

            boolean needsQuotes = newPath.contains(" ");
            String quotedNew = needsQuotes ? '"' + newPath + '"' : newPath;

            for (ClassLineMapper.LineInfo info : lineMap.getLineInfos()) {
                if (info.type == ClassLineMapper.LineType.PACKAGE_DECLARATION
                        || info.type == ClassLineMapper.LineType.ENTITY_DECLARATION
                        || info.type == ClassLineMapper.LineType.ENTITY_INLINE
                        || info.type == ClassLineMapper.LineType.RELATIONSHIP
                        || info.type == ClassLineMapper.LineType.MEMBER
                        || info.type == ClassLineMapper.LineType.UNKNOWN) {
                    updatePackageReferenceLine(info.lineNumber, oldPath, quotedNew);
                }
            }
        }
    }

    private void rewritePackageDeclaration(Package pkg) {

        int lineNum = pkg.getSourceLineStart();
        String line = getEffectiveLine(lineNum);

        String oldName = pkg.getOriginalName();
        String newName = pkg.getName();

        if (oldName.equals(newName)) return;

        String updated = line;
        updated = replaceWordBoundary(updated, oldName, newName);
        updated = updated.replace("\"" + oldName + "\"", "\"" + newName + "\"");

        if (!updated.equals(line)) {
            changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private String buildFullPath(Package pkg, boolean useOriginal) {
        List<String> parts = new ArrayList<>();
        Package current = pkg;

        while (current != null) {
            parts.add(useOriginal ? current.getOriginalName() : current.getName());
            current = current.getParentPackage();
        }

        Collections.reverse(parts);
        return String.join(detectSeparator(), parts);
    }

    private void updatePackageReferenceLine(int lineNum, String oldPath, String newPath) {
        String current = getEffectiveLine(lineNum);

        ClassLineMapper.LineType type = lineMap.getLineInfo(lineNum).type;
        String updated;

        if (type == ClassLineMapper.LineType.RELATIONSHIP || type == ClassLineMapper.LineType.MEMBER) {
            int labelIdx = current.indexOf(" : ");

            if (labelIdx >= 0) {
                String before = current.substring(0, labelIdx);
                String after = current.substring(labelIdx);
                before = replaceWordBoundary(before, oldPath, newPath);
                before = before.replace('"' + oldPath + '"', newPath);
                updated = before + after;

            } else {
                updated = replaceWordBoundary(current, oldPath, newPath);
                updated = updated.replace('"' + oldPath + '"', newPath);
            }

        } else {
            updated = replaceWordBoundary(current, oldPath, newPath);
            updated = updated.replace('"' + oldPath + '"', newPath);
        }

        if (!updated.equals(current)) {
            changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private int getDepth(Package pkg) {
        int depth = 0;

        while (pkg != null) {
            depth++;
            pkg = pkg.getParentPackage();
        }

        return depth;
    }

    private void applyReplacements() {
        List<Integer> sortedLines = new ArrayList<>(newLines.keySet());
        sortedLines.sort(Collections.reverseOrder());

        for (int startLine : sortedLines) {
            NewLine replacement = newLines.get(startLine);
            System.err.println("Applying replacement: " + replacement);

            // Remove old lines
            for (int i = replacement.endLine(); i >= replacement.startLine(); i--) {
                if (i < sourceLines.size()) {
                    sourceLines.remove(i);
                }
            }

            // Insert new lines
            sourceLines.addAll(replacement.startLine(), replacement.newLines());
        }
    }

    private void saveAtomic() throws IOException {
        File tempFile = new File(source.getParent(), source.getName() + ".tmp");

        Files.write(tempFile.toPath(), sourceLines, StandardCharsets.UTF_8);
        Files.move(
                tempFile.toPath(),
                source.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }
}