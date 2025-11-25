package com.diagrams.ClassDiagram.builders;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

import java.util.List;

public class EntityBuild {
    public GModelElement buildEntity(ClassEntity entity, double width, double height,
                                     List<String> methods, List<String> fields) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType());

        nodeBuilder.add(new GLabelBuilder("label:entityName")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .build());

        for (int i = 0; i < fields.size(); i++) {
            nodeBuilder.add(new GLabelBuilder("label:field")
                    .id(entity.getId() + "-field-" + i)
                    .text(fields.get(i))
                    .build());
        }

        for (int i = 0; i < methods.size(); i++) {
            nodeBuilder.add(new GLabelBuilder("label:method")
                    .id(entity.getId() + "-method-" + i)
                    .text(methods.get(i))
                    .build());
        }

        return nodeBuilder.build();
    }
}
