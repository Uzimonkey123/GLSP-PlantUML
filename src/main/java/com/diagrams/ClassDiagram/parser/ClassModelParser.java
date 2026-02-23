package com.diagrams.ClassDiagram.parser;

import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.model.Visibility;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.SourceElement;
import com.diagrams.ClassDiagram.state.ClassModelState;
import com.google.inject.Inject;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.abel.Link;
import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.decoration.symbol.USymbol;
import net.sourceforge.plantuml.decoration.symbol.USymbols;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.skin.VisibilityModifier;
import net.sourceforge.plantuml.text.Guillemet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ClassModelParser implements PlantUMLParser<ClassModel>  {

    @Inject
    ClassModelState modelState;

    ClassModel model;
    ClassDiagram classDiagram;
    private final TipsHandler tipsHandler = new TipsHandler();

    Map<Entity, ClassEntity> entityMapping = new HashMap<>();
    Map<Entity, Package> packageMapping = new HashMap<>();
    private int packageCounter = 0;
    private int entityCounter = 0;

    private ClassLineMapper lineMapper;
    private ClassLineFinder lineFinder;
    private final Map<Object, Integer> elementToLineMap = new HashMap<>();

    @Override
    public ClassModel parse(File file) throws IOException {
        String originalText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        this.model = new ClassModel();

        entityMapping.clear();
        packageMapping.clear();
        elementToLineMap.clear();
        packageCounter = 0;
        entityCounter = 0;

        // Create mapper and finder from the original text
        lineMapper = new ClassLineMapper(originalText);
        lineFinder = new ClassLineFinder(lineMapper, elementToLineMap);

        // Prepare text for PlantUML
        String text = originalText;
        if (!text.contains("@startuml")) {
            text = "@startuml\n" + originalText + "\n@enduml";
        }

        // Parse with PlantUML
        SourceStringReader reader = new SourceStringReader(text);
        List<BlockUml> blocks = reader.getBlocks();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();

            if (d instanceof ClassDiagram cd) {
                classDiagram = cd;

                handleTitle();
                handleHeader();
                handleFooter();

                Entity root = cd.getRootGroup();
                processEntityRecursively(root, null);

                List<Link> links = cd.getLinks();

                for (Link link : links) {
                    handleLink(link);
                }

                markNoteLinks();
            }
        }

        model.setMapper(lineMapper);
        return model;
    }

    private void addMapperInfo(SourceElement element, int lineNum) {
        if (lineNum >= 0) {
            element.setSourceLines(lineNum, lineNum);
            ClassLineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    private void addMapperInfo(SourceElement element, int startLine, int endLine) {
        if (startLine >= 0) {
            element.setSourceLines(startLine, endLine >= 0 ? endLine : startLine);
            ClassLineMapper.LineInfo info = lineMapper.getLineInfo(startLine);

            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    private void processEntityRecursively(Entity entity, Package parentPackage) {
        for (Entity group : entity.groups()) {
            if (!group.isHidden() && !group.isRemoved()) {
                Package pkg = handlePackage(group, parentPackage);
                processEntityRecursively(group, pkg);
            }
        }

        for (Entity leaf : entity.leafs()) {
            if (!leaf.isHidden() && !leaf.isRemoved()) {
                handleEntity(leaf, parentPackage);
            }
        }
    }

    private Package handlePackage(Entity entity, Package parentPackage) {
        String id = "pkg-" + packageCounter++;
        String name = String.join(" ", entity.getDisplay());
        String type = getPackageType(entity);

        Package pkg = new Package(id, name, type);

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            pkg.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        // Add to parent if exists
        if (parentPackage != null) {
            parentPackage.addChildPackage(pkg);
        }

        int line = lineFinder.findPackageLine(name, pkg);
        addMapperInfo(pkg, line);

        model.packages.add(pkg);
        packageMapping.put(entity, pkg);

        return pkg;
    }

    private String getPackageType(Entity entity) {
        USymbol symbol = entity.getUSymbol();

        if (symbol == null) return "folder";
        if (symbol == USymbols.PACKAGE) return "folder";
        if (symbol == USymbols.NODE) return "node";
        if (symbol == USymbols.DATABASE) return "database";
        if (symbol == USymbols.CLOUD) return "cloud";
        if (symbol == USymbols.FRAME) return "frame";
        if (symbol == USymbols.RECTANGLE) return "rectangle";

        return "folder";
    }

    private void handleEntity(Entity entity, Package parentPackage) {
        String id = "ent-" + entityCounter++;
        String type = entity.getLeafType().toString();

        if (type.equals("TIPS")) {
            return;
        }

        switch (type) {
            case "POINT_FOR_ASSOCIATION" -> {
                handleAssociationPoint(entity, id, parentPackage);
                return;
            }
            case "CIRCLE", "DESCRIPTION" -> {
                handleCircleEntity(entity, id, parentPackage);
                return;
            }
            case "STATE_CHOICE", "ASSOCIATION" -> {
                handleDiamondEntity(entity, id, parentPackage);
                return;
            }
            case "LOLLIPOP_FULL" -> {
                handleLollipop(entity, id, parentPackage);
                return;
            }
            case "NOTE" -> {
                handleNoteEntity(entity, id, parentPackage);
                return;
            }
        }

        String name = String.join("<br>", entity.getDisplay());

        List<EntityMethod> body = new ArrayList<>();
        List<EntityMethod> methods = new ArrayList<>();
        List<EntityMethod> fields = new ArrayList<>();

        try {
            for (CharSequence item : entity.getBodier().getRawBody()) {
                EntityMethod bodyItem = new EntityMethod(item.toString());
                body.add(bodyItem);
            }

        } catch (Exception e) {
            System.err.println("WARNING: Cannot get raw body for " + name);
        }

        try {
            for (CharSequence method : entity.getBodier().getMethodsToDisplay()) {
                EntityMethod entityMethod = new EntityMethod(method.toString());
                methods.add(entityMethod);
            }

        } catch (UnsupportedOperationException e) {
            for (EntityMethod item : body) {
                String itemStr = item.getMethodName();
                if (itemStr.contains("(") && itemStr.contains(")")) {
                    methods.add(item);
                }
            }
        }

        try {
            for (CharSequence field : entity.getBodier().getFieldsToDisplay()) {
                EntityMethod entityMethod = new EntityMethod(field.toString());
                fields.add(entityMethod);
            }

        } catch (UnsupportedOperationException e) {
            for (EntityMethod item : body) {
                String itemStr = item.getMethodName();
                if (!itemStr.contains("(") || !itemStr.contains(")")) {
                    fields.add(item);
                }
            }
        }

        ClassEntity newEntity = new ClassEntity(0, 0, id, name, type, methods, fields, body);
        model.entities.add(newEntity);
        entityMapping.put(entity, newEntity);

        int line = lineFinder.findEntityLine(name, newEntity);
        if (line >= 0) {
            String alias = ClassLineFinder.extractAlias(lineMapper.getLineInfo(line).originalText);

            if (alias != null && !alias.isEmpty() && !alias.equals(name)) {
                newEntity.setAlias(alias);
            }

            int endLine = line;
            if (lineMapper.getLineInfo(line).type == ClassLineMapper.LineType.ENTITY_DECLARATION) {
                endLine = findBlockEnd(line);
            }

            addMapperInfo(newEntity, line, endLine);

        } else {
            addMapperInfo(newEntity, -1);
        }

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            newEntity.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        if (entity.getVisibilityModifier() != null) {
            handleEntityVisibility(newEntity, entity);
        }

        if (entity.getStereotype() != null) {
            handleEntityStereotype(newEntity, entity);
        }

        if (entity.getGeneric() != null) {
            newEntity.setGeneric(entity.getGeneric());
        }

        if (parentPackage != null) {
            parentPackage.addEntity(newEntity);
        }

        System.err.println(newEntity);
    }

    private void handleLollipop(Entity entity, String id, Package parentPackage) {
        String type = "LOLLIPOP";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity lollipop = new ClassEntity(0, 0, id, name, type);
        model.entities.add(lollipop);
        entityMapping.put(entity, lollipop);

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            lollipop.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        int line = lineFinder.findEntityLine(name, lollipop);
        addMapperInfo(lollipop, line);

        if (parentPackage != null) {
            parentPackage.addEntity(lollipop);
        }
    }

    private void handleAssociationPoint(Entity entity, String id, Package parentPackage) {
        String type = "ASSOCIATION_POINT";
        String name = "";

        ClassEntity pointEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(pointEntity);
        entityMapping.put(entity, pointEntity);

        if (parentPackage != null) {
            parentPackage.addEntity(pointEntity);
        }
    }

    private void handleCircleEntity(Entity entity, String id, Package parentPackage) {
        String type = "CIRCLE";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity circleEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(circleEntity);
        entityMapping.put(entity, circleEntity);

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            circleEntity.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        int line = lineFinder.findEntityLine(name, circleEntity);
        addMapperInfo(circleEntity, line);

        if (parentPackage != null) {
            parentPackage.addEntity(circleEntity);
        }
    }

    private void handleDiamondEntity(Entity entity, String id, Package parentPackage) {
        String type = "DIAMOND";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity diamondEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(diamondEntity);
        entityMapping.put(entity, diamondEntity);

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            diamondEntity.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        int line = lineFinder.findEntityLine(name, diamondEntity);
        addMapperInfo(diamondEntity, line);

        if (parentPackage != null) {
            parentPackage.addEntity(diamondEntity);
        }
    }

    private void handleNoteEntity(Entity entity, String id, Package parentPackage) {
        String type = "NOTE";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity noteEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(noteEntity);
        entityMapping.put(entity, noteEntity);

        if (entity.getColors().getColor(ColorType.BACK) != null) {
            noteEntity.setBackground(entity.getColors().getColor(ColorType.BACK).asString());
        }

        int startLine = lineFinder.findNoteLine(name, noteEntity);
        int endLine   = lineFinder.findNoteEndLine(noteEntity);
        addMapperInfo(noteEntity, startLine, endLine);

        if (parentPackage != null) {
            parentPackage.addEntity(noteEntity);
        }
    }

    private void handleEntityVisibility(ClassEntity newEntity, Entity entity) {
        String visibility = switch(entity.getVisibilityModifier()) {
            case VisibilityModifier.PRIVATE_METHOD -> "-";
            case VisibilityModifier.PROTECTED_METHOD -> "#";
            case VisibilityModifier.PACKAGE_PRIVATE_METHOD -> "~";
            case VisibilityModifier.PUBLIC_METHOD -> "+";
            default -> "";
        };

        if (!visibility.isEmpty()) {
            newEntity.setVisibility(Visibility.fromChar(visibility.charAt(0)));
        }
    }

    private void handleEntityStereotype(ClassEntity newEntity, Entity entity) {
        newEntity.setStereotype(true);
        newEntity.setStereotypeName(entity.getStereotype().getLabel(Guillemet.DOUBLE_COMPARATOR));
        newEntity.setStereotype(entity.getStereotype().getCharacter());
        if (entity.getStereotype().getHtmlColor() != null) {
            newEntity.setStereotypeColor(entity.getStereotype().getHtmlColor().asString());
        }
    }

    private ClassEntity resolveEntity(Entity entity) {
        ClassEntity classEntity = entityMapping.get(entity);
        if (classEntity != null) return classEntity;

        Package pkg = packageMapping.get(entity);
        if (pkg != null) {
            return new ClassEntity(
                    (int) pkg.getX(), (int) pkg.getY(),
                    pkg.getAnchorId(), pkg.getName(), "PACKAGE"
            );
        }

        return null;
    }

    private boolean isLeafType(Entity entity, String type) {
        try {
            return entity.getLeafType().toString().equals(type);

        } catch (Exception e) {
            return false;
        }
    }

    private void handleLink(Link link) {
        Entity linkEntity1 = link.getEntity1();
        Entity linkEntity2 = link.getEntity2();

        if (linkEntity1.isRemoved() || linkEntity2.isRemoved() || linkEntity1.isHidden() || linkEntity2.isHidden()) {
            return;
        }

        if (isLeafType(linkEntity1, "TIPS")) {
            tipsHandler.applyTipsToEntity(linkEntity1, entityMapping.get(linkEntity2));
            return;
        }

        if (isLeafType(linkEntity2, "TIPS")) {
            tipsHandler.applyTipsToEntity(linkEntity2, entityMapping.get(linkEntity1));
            return;
        }

        String id = "link-" + model.links.size();
        ClassEntity entity1 = resolveEntity(linkEntity1);
        ClassEntity entity2 = resolveEntity(linkEntity2);

        if (entity1 == null || entity2 == null) {
            return;
        }

        String type = link.getType().toString();
        String message = String.join("<br>", link.getLabel().toString());
        if (message.equals("NULL")) {
            message = "";
        }

        if (link.isHidden()) {
            type = "INVISIBLE";
            message = "";
        }

        int length = link.getLength();
        String decorator1 = link.getType().getDecor1().toString();
        String decorator2 = link.getType().getDecor2().toString();
        String quant1 = link.getQuantifier1();
        String quant2 = link.getQuantifier2();
        String member1 = link.getPortName1();
        String member2 = link.getPortName2();

        ClassLink newLink = new ClassLink(id, entity1, entity2, type, message, length,
                decorator1, decorator2, quant1, quant2);

        if (member1 != null && !member1.isEmpty()) {
            newLink.setSourceMember(member1);
        }

        if (member2 != null && !member2.isEmpty()) {
            newLink.setTargetMember(member2);
        }

        String kal1 = link.getLinkArg().getKal1();
        String kal2 = link.getLinkArg().getKal2();

        if (kal1 != null && !kal1.isEmpty()) {
            newLink.setSourceQualifier(kal1);
        }

        if (kal2 != null && !kal2.isEmpty()) {
            newLink.setTargetQualifier(kal2);
        }

        if (link.getNote() != null) {
            String noteText = String.join("<br>", link.getNote().getDisplay());
            newLink.setNoteOnLink(noteText);
            newLink.setNotePosition(link.getNote().getPosition().toString());

            if (link.getNote().getColors().getColor(ColorType.BACK) != null) {
                newLink.setNoteColor(link.getNote().getColors().getColor(ColorType.BACK).asString());
            }
        }

        int line = lineFinder.findRelationshipLine(entity1.getName(), entity2.getName(), newLink);
        addMapperInfo(newLink, line);

        model.links.add(newLink);
        linkAttributes(newLink, link);
        System.err.println(newLink);
    }

    private void linkAttributes(ClassLink newLink, Link link) {
        if (link.getColors().getColor(ColorType.LINE) != null) {
            String color = link.getColors().getColor(ColorType.LINE).asString();
            newLink.setColor(color);
        }

        UStroke stroke = link.getType().getStroke3(null);
        newLink.setThickness(stroke.getThickness());
    }

    private int findBlockEnd(int startLine) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();
        int depth = 0;
        for (int i = startLine; i < all.size(); i++) {
            for (char c : all.get(i).originalText.toCharArray()) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }

        return startLine;
    }

    private void markNoteLinks() {
        Map<String, Integer> noteConnections = new HashMap<>();

        for (ClassLink link : model.links) {
            if (link.getEntity1().getType().equals("NOTE")) {
                noteConnections.merge(link.getEntity1().getId(), 1, Integer::sum);
            }
            if (link.getEntity2().getType().equals("NOTE")) {
                noteConnections.merge(link.getEntity2().getId(), 1, Integer::sum);
            }
        }

        for (ClassLink link : model.links) {
            boolean isNoteWithOneLink =
                    (link.getEntity1().getType().equals("NOTE") && noteConnections.get(link.getEntity1().getId()) == 1) ||
                            (link.getEntity2().getType().equals("NOTE") && noteConnections.get(link.getEntity2().getId()) == 1);

            if (isNoteWithOneLink) {
                link.setNoteLink(true);
            }
        }
    }

    private void handleTitle() {
        model.title = "";

        if (classDiagram.getTitle() != null
                && classDiagram.getTitle().getDisplay() != null
                && classDiagram.getTitle().getDisplay().size() > 0) {
            model.title = String.join("<br>", classDiagram.getTitle().getDisplay());

            int start = lineFinder.findTitleLine();
            int end   = lineFinder.findEndTitleLine();

            if (start >= 0) {
                model.titleLineStart = start;
                model.titleLineEnd   = end >= 0 ? end : start;
            }
        }
    }

    private void handleHeader() {
        model.header = "";

        if (classDiagram.getHeader() != null
                && classDiagram.getHeader().getDisplay() != null
                && classDiagram.getHeader().getDisplay().size() > 0) {
            model.header = String.join("<br>", classDiagram.getHeader().getDisplay());

            int start = lineFinder.findHeaderLine();
            int end   = lineFinder.findEndHeaderLine();

            if (start >= 0) {
                model.headerLineStart = start;
                model.headerLineEnd   = end >= 0 ? end : start;
            }
        }
    }

    private void handleFooter() {
        model.footer = "";

        if (classDiagram.getFooter() != null
                && classDiagram.getFooter().getDisplay() != null
                && classDiagram.getFooter().getDisplay().size() > 0) {
            model.footer = String.join("<br>", classDiagram.getFooter().getDisplay());

            int start = lineFinder.findFooterLine();
            int end   = lineFinder.findEndFooterLine();

            if (start >= 0) {
                model.footerLineStart = start;
                model.footerLineEnd   = end >= 0 ? end : start;
            }
        }
    }
}