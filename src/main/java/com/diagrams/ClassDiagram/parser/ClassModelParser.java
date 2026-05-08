/*
 * File: ClassModelParser.java
 * Author: Norman Babiak
 * Description: Parser to internal model from PlantUML public API
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.parser;

import com.GLSPPlantUML.parser.PlantUMLParser;
import com.GLSPPlantUML.utils.ErrorMessage;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.parser.handlers.EntityHandlerContext;
import com.diagrams.ClassDiagram.parser.handlers.EntityHandlerRegistry;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
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
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.color.ColorType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.diagrams.ClassDiagram.utils.MapperInfo.addMapperInfo;

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

    private EntityHandlerRegistry handlerRegistry;

    /**
     * Reads the PlantUML file, parses it via SourceStringReader, and populates the internal model with entities,
     * packages, links, and page details
     */
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
        lineMapper = new ClassLineMapper(originalText, model);
        lineFinder = new ClassLineFinder(lineMapper, elementToLineMap);

        EntityHandlerContext ctx = new EntityHandlerContext(model, lineMapper, lineFinder, entityMapping);
        handlerRegistry = new EntityHandlerRegistry(ctx);

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
            if (d instanceof PSystemError) {
                String error = String.join("<br>", ((PSystemError) d).getPureAsciiFormatted());
                modelState.setError(new ErrorMessage(error));
                return model;
            }

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
        model.setLineFinder(lineFinder);
        return model;
    }

    /**
     * Method to handle entities and packages recursively, skipping hidden or removed ones
     */
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

    /**
     * Creates a Package from a PlantUML group entity, attaches it to its parent, and maps it to source lines
     */
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
        addMapperInfo(pkg, line, lineMapper);

        model.packages.add(pkg);
        packageMapping.put(entity, pkg);

        return pkg;
    }

    /**
     * Maps a PlantUML USymbol to the internal package type string
     */
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

    /**
     * Delegates entity creation to the handler registry, which picks the correct handler by leaf type
     */
    private void handleEntity(Entity entity, Package parentPackage) {
        String id = "ent-" + entityCounter++;
        handlerRegistry.handle(entity, id, parentPackage);
    }

    /**
     * Returns entities for link if they are entity, if they are package, they create an empty entity for package links
     */
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

    /**
     * Parses a PlantUML Link into an internal ClassLink with decorators, quantifiers, qualifiers,
     * member ports, colors, and optional note-on-link attachment
     */
    private void handleLink(Link link) {
        Entity linkEntity1 = link.getEntity1();
        Entity linkEntity2 = link.getEntity2();

        if (linkEntity1.isRemoved() || linkEntity2.isRemoved() || linkEntity1.isHidden() || linkEntity2.isHidden()) {
            return;
        }

        // Tip entities are handled different, due to the link for the member of the entity
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

        // Parse basic setup for the link
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

        String alias1 = entity1.getAlias();
        String alias2 = entity2.getAlias();

        if (alias1 == null) {
            alias1 = entity1.getName();
        }

        if (alias2 == null) {
            alias2 = entity2.getName();
        }

        int line = lineFinder.findRelationshipLine(alias1, alias2, newLink);
        addMapperInfo(newLink, line, lineMapper);

        // Create note entity if there is one attached to the link
        if (link.getNote() != null) {
            String noteText = String.join("<br>", link.getNote().getDisplay());
            String noteId = "note-" + id;

            ClassEntity newNote = new ClassEntity(0, 0, noteId, noteText, "NOTE");
            newLink.setNoteOnLink(newNote);
            newLink.setNotePosition(link.getNote().getPosition().toString());

            if (link.getNote().getColors().getColor(ColorType.BACK) != null) {
                newNote.setBackground(link.getNote().getColors().getColor(ColorType.BACK).asString());
            }

            model.notes.add(newNote);

            int startLine = lineFinder.findNoteLine(noteText, newNote);
            int endLine   = lineFinder.findNoteEndLine(startLine, newNote);
            addMapperInfo(newNote, startLine, endLine, lineMapper);
        }

        model.links.add(newLink);
        linkAttributes(newLink, link);
    }

    /**
     * Extracts line color and stroke thickness from the PlantUML Link object
     */
    private void linkAttributes(ClassLink newLink, Link link) {
        if (link.getColors().getColor(ColorType.LINE) != null) {
            String color = link.getColors().getColor(ColorType.LINE).asString();
            newLink.setColor(color);
        }

        UStroke stroke = link.getType().getStroke3(null);
        newLink.setThickness(stroke.getThickness());
    }

    /**
     * Method to distinguish between note links and notes connected with multiple links to other entities
     */
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

    /**
     * Extracts the diagram title and maps it to source lines
     */
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

    /**
     * Extracts the diagram header and maps it to source lines
     */
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

    /**
     * Extracts the diagram footer and maps it to source lines
     */
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