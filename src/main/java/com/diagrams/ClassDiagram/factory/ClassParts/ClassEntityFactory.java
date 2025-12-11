package com.diagrams.ClassDiagram.factory.ClassParts;

import com.GLSPPlantUML.utils.WidthCalculator;
import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import org.eclipse.glsp.graph.GModelElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassEntityFactory {

    private final ClassModel model;
    private final EntityBuild entityBuild;

    private final List<GModelElement> elements;
    private final ClassLayout layoutEngine;

    private final int horizontalPadding = 20;
    private double cursor = 40;

    public ClassEntityFactory(ClassModel model, EntityBuild entityBuild, List<GModelElement> elements) {
        this.model = model;
        this.entityBuild = entityBuild;
        this.elements = elements;
        this.layoutEngine = new ClassLayout();
    }

    public void createEntities() {
        Map<String, ClassLayout.Size> dimensions = new HashMap<>();

        for (ClassEntity entity : model.entities) {
            double width, height;

            if (entity.getType().equals("CIRCLE")) {
                width = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
                height = entityLength(entity);

            } else if (entity.getType().equals("DIAMOND")) {
                width = 30;
                height = 30;

            } else {
                double methodWidth = entityAttributesLength(entity.getMethods());
                double fieldsWidth = entityAttributesLength(entity.getFields());
                double attributesWidth = Math.max(methodWidth, fieldsWidth);
                width = Math.max(
                        WidthCalculator.calculateWidth(entity.getName(), horizontalPadding),
                        attributesWidth);
                height = entityLength(entity);
            }

            dimensions.put(entity.getId(), new ClassLayout.Size(width, height));
        }

        layoutEngine.layoutEntities(model.entities, model.links, dimensions);

        for (ClassEntity entity : model.entities) {
            if (entity.getType().equals("CIRCLE")) {
                createCircleEntity(entity);
                continue;
            }

            if (entity.getType().equals("DIAMOND")) {
                createDiamondEntity(entity);
                continue;
            }

            ClassLayout.Size size = dimensions.get(entity.getId());
            double entityWidth = size.width;
            double entityHeight = size.height;

            List<String> methodNames = new ArrayList<>();
            for (EntityMethod method : entity.getMethods()) {
                methodNames.add(method.getMethodName());
            }

            List<String> fieldNames = new ArrayList<>();
            for (EntityMethod fields : entity.getFields()) {
                fieldNames.add(fields.getMethodName());
            }

            List<String> bodyLines = new ArrayList<>();
            for (EntityMethod item : entity.getRawBody()) {
                bodyLines.add(item.getMethodName());
            }

            elements.add(entityBuild.buildEntity(entity, entityWidth, entityHeight, methodNames, fieldNames, bodyLines));
        }
    }

    private void createCircleEntity(ClassEntity entity) {
        double entityWidth = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
        double entityHeight = entityLength(entity);

        entity.setX(cursor);

        elements.add(entityBuild.buildCircleEntity(entity, entityWidth, entityHeight));
    }

    private void createDiamondEntity(ClassEntity entity) {
        double entityWidth = 30;

        entity.setX(cursor);

        elements.add(entityBuild.buildDiamondEntity(entity, entityWidth));
    }

    private double entityAttributesLength(List<EntityMethod> attribute) {
        double length = 0;

        for (EntityMethod method : attribute) {
            length = Math.max(WidthCalculator.calculateWidth(method.getMethodName(), horizontalPadding / 2), length);
        }

        return length;
    }

    private double entityLength(ClassEntity entity) {
        int lineHeight = 14;
        int verticalPadding = 5;
        int emptyHeight = 10;
        int headerHeight = 30;

        int fieldsHeight = !entity.getFields().isEmpty()
                ? entity.getFields().size() * lineHeight + verticalPadding * 2
                : emptyHeight;

        int methodsHeight = !entity.getMethods().isEmpty()
                ? entity.getMethods().size() * lineHeight + verticalPadding * 2
                : emptyHeight;

        return headerHeight + fieldsHeight + methodsHeight;
    }
}
