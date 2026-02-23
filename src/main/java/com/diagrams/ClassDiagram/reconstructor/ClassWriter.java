package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.utils.NewLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ClassWriter {

    private final ClassModel model;
    private final File source;
    private final List<String> sourceLines;
    private final Map<Integer, NewLine> newLines;
    private final ClassLineMapper lineMap;

    public ClassWriter(ClassModel model, String sourceUri) throws IOException {
        this.model = model;
        this.source = new File(URI.create(sourceUri));
        this.sourceLines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);
        this.newLines = new HashMap<>();
        this.lineMap = model.getLineMapper();
    }

    public void write() throws IOException {
        sourceLines.clear();
        sourceLines.addAll(Files.readAllLines(source.toPath(), StandardCharsets.UTF_8));
        newLines.clear();

        writeEntities();
        writeLinks();
        writePageDetails();
        applyReplacements();
        saveAtomic();
    }

    private void changeLine(int start, int end, List<String> lines) {
        newLines.put(start, new NewLine(start, end, lines));
    }

    private static String extractIndentation(String line) {
        if (line == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') sb.append(c);
            else break;
        }

        return sb.toString();
    }

    private static String applyIndentation(String line, String indent) {
        return indent + line;
    }

    private void writeEntities() {
        for (ClassEntity entity : model.entities) {
            System.err.println("[is modified]: " + entity + " : " + entity.hasLine());
            if (!entity.isModified()) continue;
            if (!entity.hasLine()) continue;

            System.err.println("[To write]: " + entity);

            int start = entity.getSourceLineStart();
            int end   = entity.getSourceLineEnd();

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
                        || info.type == ClassLineMapper.LineType.RELATIONSHIP) {
                    updateReferenceLine(info.lineNumber, entity);
                }
            }
        }
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
            sb.append(" ").append(m.getMethodName());
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
            lines.add(applyIndentation("  " + member.getMethodName(), indent));
        }
        lines.add(applyIndentation("}", indent));

        return lines;
    }

    private String buildDeclarationHeader(ClassEntity entity) {
        StringBuilder sb = new StringBuilder();
        String vis = entity.getVisibility();

        if (vis != null && !vis.isEmpty()) {
            char symbol = visibilityToSymbol(vis);
            if (symbol != 0) sb.append(symbol);
        }

        String type = entity.getType();
        switch (type) {
            case "INTERFACE" -> sb.append("interface");
            case "ENUM" -> sb.append("enum");
            case "ANNOTATION" -> sb.append("annotation");
            case "ABSTRACT", "ABSTRACT_CLASS" -> {
                String raw = entity.getRawSourceText();
                if (raw != null && raw.trim().startsWith("abstract class")) {
                    sb.append("abstract class");

                } else {
                    sb.append("abstract");
                }
            }
            case "DATACLASS" -> sb.append("dataclass");
            case "ENTITY" -> sb.append("entity");
            case "EXCEPTION" -> sb.append("exception");
            case "METACLASS" -> sb.append("metaclass");
            case "PROTOCOL" -> sb.append("protocol");
            case "RECORD" -> sb.append("record");
            case "STEREOTYPE" -> sb.append("stereotype");
            case "STRUCT" -> sb.append("struct");
            case "DIAMOND" -> sb.append("diamond");
            case "CIRCLE" -> sb.append("circle");
            default -> sb.append("class");
        }
        sb.append(" ");

        String name  = entity.getName();
        String alias = entity.getAlias();

        boolean needsAlias = alias != null && !alias.isEmpty() && !alias.equals(name);
        boolean nameNeedsQuotes = !name.matches("[A-Za-z0-9_<>, ]+") || name.contains(" ");

        if (needsAlias) {
            if (nameNeedsQuotes) {
                sb.append("\"").append(name).append("\" as ").append(alias);

            } else {
                sb.append(alias).append(" as \"").append(name).append("\"");
            }

        } else {
            if (nameNeedsQuotes) {
                sb.append("\"").append(name).append("\"");

            } else {
                sb.append(name);
            }
        }

        String generic = entity.getGeneric();
        if (generic != null && !generic.isEmpty()) {
            sb.append("<").append(generic).append(">");
        }

        if (entity.isStereotype()) {
            sb.append(" << ");
            char c = entity.getStereotypeChar();
            String sColor = entity.getStereotypeColor();
            if (c != ' ' && c != 0) {
                sb.append("(").append(c);
                if (sColor != null && !sColor.isEmpty()) {
                    sb.append(",").append(sColor);
                }

                sb.append(") ");
            }

            String sName = entity.getStereotypeName();
            if (sName != null && !sName.isEmpty()) {
                sName = sName.replace("«", "").replace("»", "")
                        .replace("<<", "").replace(">>", "").trim();
                if (!sName.isEmpty()) sb.append(sName);
            }

            sb.append(" >>");
        }

        String bg = entity.getExplicitBackground();
        if (bg != null && !bg.isEmpty()) {
            sb.append(" ").append(bg);
        }

        return sb.toString();
    }

    private static char visibilityToSymbol(String visibilityValue) {
        return switch (visibilityValue) {
            case "private" -> '-';
            case "protected" -> '#';
            case "package_private" -> '~';
            case "public" -> '+';
            default -> 0;
        };
    }

    private void updateReferenceLine(int lineNum, ClassEntity entity) {
        String current;

        if (newLines.containsKey(lineNum)) {
            List<String> pending = newLines.get(lineNum).newLines();
            current = pending.isEmpty() ? "" : pending.getFirst();

        } else {
            current = sourceLines.get(lineNum);
        }

        String updated = replaceReferenceLine(current, entity);

        if (!updated.equals(current)) {
            changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private String replaceReferenceLine(String current, ClassEntity entity) {
        String oldName = entity.getOriginalName();
        String alias = entity.getAlias();
        String newToken = (alias != null && !alias.isEmpty()) ? alias : entity.getName();

        boolean needsQuotes = newToken.contains(" ") || !newToken.matches("[A-Za-z0-9_]+");
        String quotedNew = needsQuotes ? '"' + newToken + '"' : newToken;

        if (oldName.equals(newToken)) {
            return current;
        }

        String updated = replaceWordBoundary(current, oldName, quotedNew);
        updated = updated.replace('"' + oldName + '"', quotedNew);

        return updated;
    }

    private static String replaceWordBoundary(String text, String oldWord, String replacement) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            int idx = text.indexOf(oldWord, i);
            if (idx < 0) {
                result.append(text.substring(i)); break;
            }

            char before = idx > 0 ? text.charAt(idx - 1) : ' ';
            char after  = idx + oldWord.length() < text.length()
                    ? text.charAt(idx + oldWord.length()) : ' ';

            boolean ok = !Character.isLetterOrDigit(before) && before != '_'
                    && !Character.isLetterOrDigit(after)  && after  != '_';

            if (ok) {
                result.append(text, i, idx).append(replacement);
                i = idx + oldWord.length();

            } else {
                result.append(text.charAt(idx));
                i = idx + 1;
            }
        }

        return result.toString();
    }

    private void writeLinks() {
        for (ClassLink link : model.links) {
            if (!link.isModified() && !link.getMessage().isModified()
                    && !link.getQuantifier1().isModified() && !link.getQuantifier2().isModified()) continue;

            int start = link.getSourceLineStart();
            int end   = link.getSourceLineEnd();
            String indent = extractIndentation(sourceLines.get(start));

            changeLine(start, end, List.of(applyIndentation(buildLinkLine(link), indent)));
        }
    }

    private String buildLinkLine(ClassLink link) {
        StringBuilder sb = new StringBuilder();
        String e1Token = getEntityToken(link.getEntity1());
        String e2Token = getEntityToken(link.getEntity2());
        String srcQual = link.getSourceQualifier();

        if (srcQual != null && !srcQual.isEmpty()) {
            sb.append(e1Token).append(" [").append(srcQual).append("]");

        } else {
            sb.append(e1Token);
        }

        String q1 = link.getQuantifier1().getLabel();
        if (q1 != null && !q1.isEmpty()) {
            sb.append(" \"").append(q1).append("\"");
        }

        sb.append(" ");
        sb.append(buildClassArrow(link));
        sb.append(" ");

        String q2 = link.getQuantifier2().getLabel();
        if (q2 != null && !q2.isEmpty()) {
            sb.append("\"").append(q2).append("\" ");
        }

        String tgtQual = link.getTargetQualifier();
        if (tgtQual != null && !tgtQual.isEmpty()) {
            sb.append("[").append(tgtQual).append("] ").append(e2Token);

        } else {
            sb.append(e2Token);
        }

        String label = link.getMessage().getLabel();
        if (label != null && !label.isEmpty()) {
            sb.append(" : ").append(label);
        }

        return sb.toString();
    }

    private String getEntityToken(ClassEntity entity) {
        String alias = entity.getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }

        String name = entity.getName();
        if (name.contains(" ") || !name.matches("[A-Za-z0-9_]+")) {
            return "\"" + name + "\"";
        }

        return name;
    }

    private String buildClassArrow(ClassLink link) {
        StringBuilder sb = new StringBuilder();

        sb.append(decoratorToLeft(link.getDecorator2()));
        String lineChar = link.getType().contains("DOTTED") || link.getType().contains("DASHED") ? "." : "-";
        String color = link.getColor();
        boolean hasColor = color != null && !color.equals("#000000") && !color.isEmpty();
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

        sb.append(decoratorToRight(link.getDecorator1()));
        return sb.toString();
    }

    private static String decoratorToLeft(String dec) {
        if (dec == null || dec.isEmpty()) return "";

        return switch (dec) {
            case "ARROW" -> "<";
            case "EXTENDS" -> "<|";
            case "COMPOSITION" -> "*";
            case "AGREGATION" -> "o";
            case "PLUS" -> "+";
            case "SQUARE" -> "#";
            case "CROWFOOT" -> "}";
            default -> "";
        };
    }

    private static String decoratorToRight(String dec) {
        if (dec == null || dec.isEmpty()) return "";

        return switch (dec) {
            case "ARROW" -> ">";
            case "EXTENDS" -> "|>";
            case "COMPOSITION" -> "*";
            case "AGREGATION" -> "o";
            case "PLUS" -> "+";
            case "SQUARE" -> "#";
            case "CROWFOOT" -> "{";
            default -> "";
        };
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