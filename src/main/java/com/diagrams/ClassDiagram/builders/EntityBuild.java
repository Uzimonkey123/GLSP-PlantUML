package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

import java.util.List;

public class EntityBuild {
    public GModelElement buildEntity(ClassEntity entity, double width, double height,
                                     List<String> methods, List<String> fields, List<String> bodyLines) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType())
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:entityName")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("width", width)
                .addArgument("visibility", entity.getVisibility())
                .addArgument("height", height)
                .addArgument("stereotypeName", entity.getStereotypeName())
                .addArgument("stereotypeChar", String.valueOf(entity.getStereotypeChar()))
                .addArgument("stereotypeColor", entity.getStereotypeColor())
                .addArgument("hasGeneric", entity.isGeneric())
                .build());

        if (entity.isGeneric()) {
            nodeBuilder.add(new GLabelBuilder("label:generic")
                    .id(entity.getId() + "generic")
                    .text(entity.getGeneric())
                    .build());
        }

        for (int i = 0; i < fields.size(); i++) {
            nodeBuilder.add(new GLabelBuilder("label:field")
                    .id(entity.getId() + "-field-" + i)
                    .text(fields.get(i))
                    .addArgument("visibility", entity.getFields().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        for (int i = 0; i < methods.size(); i++) {
            nodeBuilder.add(new GLabelBuilder("label:method")
                    .id(entity.getId() + "-method-" + i)
                    .text(methods.get(i))
                    .addArgument("visibility", entity.getMethods().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        for (int i = 0; i < bodyLines.size(); i++) {
            nodeBuilder.add(new GLabelBuilder("label:body")
                    .id(entity.getId() + "-body-" + i)
                    .text(bodyLines.get(i))
                    .addArgument("visibility", entity.getRawBody().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        return nodeBuilder.build();
    }

    public GModelElement buildCircleEntity(ClassEntity entity, double width, double height) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:circle")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:method")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("width", width)
                .addArgument("visibility", entity.getVisibility())
                .build());

        return nodeBuilder.build();
    }

    public GModelElement buildDiamondEntity(ClassEntity entity, double width) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:diamond")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, width)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("background", entity.getBackground());

        return nodeBuilder.build();
    }

    public GModelElement buildAssociationPoint(ClassEntity entity) {
        GNodeBuilder point = new GNodeBuilder("entity:association-point")
                .id(entity.getId())
                .position(entity.getX(), entity.getY())
                .size(8, 8);

        return point.build();
    }

    public GModelElement buildNoteEntity(ClassEntity entity, double width, double height) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:note")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", "note")
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:note")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", "note")
                .addArgument("width", width)
                .build());

        return nodeBuilder.build();
    }
}

