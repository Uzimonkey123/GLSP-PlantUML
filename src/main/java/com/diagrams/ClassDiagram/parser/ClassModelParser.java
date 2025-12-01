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
import net.sourceforge.plantuml.abel.LeafType;
import net.sourceforge.plantuml.abel.Link;
import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.decoration.LinkDecor;
import net.sourceforge.plantuml.skin.VisibilityModifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ClassModelParser implements PlantUMLParser<ClassModel>  {

    @Inject
    ClassModelState modelState;

    ClassModel model;

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

                for (Entity entity : root.leafs()) {
                    handleEntity(entity);
                }

                List<Link> links = cd.getLinks();

                for (Link link : links) {
                    handleLink(link);
                }
            }
        }

        return model;
    }

    private void handleEntity(Entity entity) {
        String id = "ent-" + model.entities.size();
        String type = entity.getLeafType().toString();
        if (type.equals("CIRCLE") || type.equals("DESCRIPTION")) {
            handleCircleEntity(entity, id);
        }

        if (type.equals("DIAMOND") || type.equals("ASSOCIATION")) {
            handleDiamondEntity(entity, id);
        }

        String name = String.join("<br>", entity.getDisplay().toString())
                        .replaceAll("^\\[|]$", "");

        List<EntityMethod> body = new ArrayList<>();
        for (CharSequence item : entity.getBodier().getRawBody()) {
            EntityMethod bodyItem = new EntityMethod(item.toString());
            body.add(bodyItem);
        }

        List<EntityMethod> methods = new ArrayList<>();
        for (CharSequence method : entity.getBodier().getMethodsToDisplay()) {
            EntityMethod entityMethod = new EntityMethod(method.toString());
            methods.add(entityMethod);
        }

        List<EntityMethod> fields = new ArrayList<>();
        for (CharSequence field : entity.getBodier().getFieldsToDisplay()) {
            EntityMethod entityMethod = new EntityMethod(field.toString());
            fields.add(entityMethod);
        }

        ClassEntity newEntity = new ClassEntity(0, 0, id, name, type, methods, fields, body);
        model.entities.add(newEntity);

        if (entity.getVisibilityModifier() != null) {
            handleEntityVisibility(newEntity, entity);
        }

        System.err.println(newEntity);
    }

    private void handleCircleEntity(Entity entity, String id) {
        String type = "CIRCLE";
        String name = String.join("<br>", entity.getDisplay().toString())
                .replaceAll("^\\[|]$", "");

        ClassEntity circleEntity = new ClassEntity(0, 0, id, name, type);
        model.entities.add(circleEntity);

    }

    private void handleDiamondEntity(Entity entity, String id) {
        String type = "DIAMOND";

        ClassEntity diamondEntity = new ClassEntity(0, 0, id, type);
        model.entities.add(diamondEntity);
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

    private void handleLink(Link link) {
        String id = "link-" + model.links.size();
        ClassEntity entity1 = model.getClassEntity(
                                String.join("<br>", link.getEntity1().getDisplay().toString())
                                        .replaceAll("^\\[|]$", ""));
        ClassEntity entity2 = model.getClassEntity(
                                String.join("<br>", link.getEntity2().getDisplay().toString())
                                        .replaceAll("^\\[|]$", ""));
        String type = link.getType().toString();
        String message = String.join("<br>", link.getLabel().toString());

        int length = link.getLength();
        String decorator1 = link.getType().getDecor1().toString();
        String decorator2 = link.getType().getDecor2().toString();

        ClassLink newLink = new ClassLink(id, entity1, entity2, type, message, length, decorator1, decorator2);
        model.links.add(newLink);
        System.err.println(newLink);
    }
}
