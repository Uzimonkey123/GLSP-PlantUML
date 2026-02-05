package com.diagrams.ClassDiagram.parser;

import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.Visibility;
import com.diagrams.ClassDiagram.state.ClassModelState;
import com.google.inject.Inject;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.abel.Link;
import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.skin.VisibilityModifier;
import net.sourceforge.plantuml.text.Guillemet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassModelParser implements PlantUMLParser<ClassModel>  {

    @Inject
    ClassModelState modelState;

    ClassModel model;
    private final TipsHandler tipsHandler = new TipsHandler();

    Map<String, Entity> tipsEntities = new HashMap<>();
    Map<Entity, ClassEntity> entityMapping = new HashMap<>();

    @Override
    public ClassModel parse(File file) throws IOException {
        String originalText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        this.model = new ClassModel();

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
                Entity root = cd.getRootGroup();
                processEntityRecursively(root);

                List<Link> links = cd.getLinks();

                for (Link link : links) {
                    handleLink(link);
                }

                markNoteLinks();
            }
        }

        return model;
    }

    private void processEntityRecursively(Entity entity) {
        for (Entity leaf : entity.leafs()) {
            if (!leaf.isHidden() && !leaf.isRemoved()) {
                handleEntity(leaf);
            }
        }

        for (Entity group : entity.groups()) {
            if (!group.isHidden() && !group.isRemoved()) {
                processEntityRecursively(group);
            }
        }
    }

    private void handleEntity(Entity entity) {
        String id = "ent-" + model.entities.size();
        String type = entity.getLeafType().toString();

        if (type.equals("TIPS")) {
            tipsEntities.put(id, entity);
            return;
        }

        switch (type) {
            case "POINT_FOR_ASSOCIATION" -> {
                handleAssociationPoint(entity, id);
                return;
            }
            case "CIRCLE", "DESCRIPTION" -> {
                handleCircleEntity(entity, id);
                return;
            }
            case "STATE_CHOICE", "ASSOCIATION" -> {
                handleDiamondEntity(entity, id);
                return;
            }
            case "NOTE" -> {
                handleNoteEntity(entity, id);
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

        if (entity.getVisibilityModifier() != null) {
            handleEntityVisibility(newEntity, entity);
        }

        if (entity.getStereotype() != null) {
            handleEntityStereotype(newEntity, entity);
        }

        if (entity.getGeneric() != null) {
            newEntity.setGeneric(entity.getGeneric());
        }

        System.err.println(newEntity);
    }

    private void handleAssociationPoint(Entity entity, String id) {
        String type = "ASSOCIATION_POINT";
        String name = "";

        ClassEntity pointEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(pointEntity);
    }

    private void handleCircleEntity(Entity entity, String id) {
        String type = "CIRCLE";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity circleEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(circleEntity);
    }

    private void handleDiamondEntity(Entity entity, String id) {
        String type = "DIAMOND";
        String name = String.join("<br>", entity.getDisplay());

        ClassEntity diamondEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(diamondEntity);
    }

    private void handleNoteEntity(Entity entity, String id) {
        String type = "NOTE";
        String rawDisplay = entity.getDisplay().toString();
        System.err.println("RAW NOTE DISPLAY: '" + rawDisplay + "'");
        System.err.println("RAW NOTE DISPLAY BYTES: " + java.util.Arrays.toString(rawDisplay.getBytes()));

        String name = String.join("<br>", entity.getDisplay());

        ClassEntity noteEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(noteEntity);
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
    }

    private void handleLink(Link link) {
        Entity linkEntity1 = link.getEntity1();
        Entity linkEntity2 = link.getEntity2();

        if (linkEntity1.isRemoved() || linkEntity2.isRemoved() || linkEntity1.isHidden() || linkEntity2.isHidden()) {
            return;
        }

        if (linkEntity1.getLeafType().toString().equals("TIPS")) {
            tipsHandler.applyTipsToEntity(linkEntity1, entityMapping.get(linkEntity2));
            return;
        }

        if (linkEntity2.getLeafType().toString().equals("TIPS")) {
            tipsHandler.applyTipsToEntity(linkEntity2, entityMapping.get(linkEntity1));
            return;
        }

        String id = "link-" + model.links.size();
        ClassEntity entity1 = entityMapping.get(linkEntity1);
        ClassEntity entity2 = entityMapping.get(linkEntity2);

        if (entity1 == null || entity2 == null) {
            return;
        }

        String type = link.getType().toString();
        String message = String.join("<br>", link.getLabel().toString());
        if (message.equals("NULL")) {
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
        double thickness = stroke.getThickness();
        newLink.setThickness(thickness);
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
}