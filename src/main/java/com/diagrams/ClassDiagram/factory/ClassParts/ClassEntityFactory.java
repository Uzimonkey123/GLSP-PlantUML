package com.diagrams.ClassDiagram.factory.ClassParts;

import com.GLSPPlantUML.utils.WidthCalculator;
import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import org.eclipse.glsp.graph.GModelElement;

import java.util.ArrayList;
import java.util.List;

public class ClassEntityFactory {

    private final ClassModel model;
    private final EntityBuild entityBuild;

    private final List<GModelElement> elements;

    private final int padding = 20;
    private double cursor = 40;

    public ClassEntityFactory(ClassModel model, EntityBuild entityBuild, List<GModelElement> elements) {
        this.model = model;
        this.entityBuild = entityBuild;
        this.elements = elements;
    }

    public void createEntities() {
        for (ClassEntity entity : model.entities) {
            double methodWidth = entityAttributesLength(entity.getMethods());
            double fieldsWidth = entityAttributesLength(entity.getFields());
            double attributesWidth = Math.max(methodWidth, fieldsWidth);
            double entityWidth = Math.max(WidthCalculator.calculateWidth(entity.getName(), padding), attributesWidth);

            double entityHeight = entityLength(entity);

            entity.setX(cursor);

            List<String> methodNames = new ArrayList<>();
            for (EntityMethod method : entity.getMethods()) {
                methodNames.add(method.getMethodName());
            }

            List<String> fieldNames = new ArrayList<>();
            for (EntityMethod fields : entity.getFields()) {
                fieldNames.add(fields.getMethodName());
            }

            elements.add(entityBuild.buildEntity(entity, entityWidth, entityHeight, methodNames, fieldNames));

            cursor += entityWidth;
        }
    }

    private double entityAttributesLength(List<EntityMethod> attribute) {
        double length = 0;

        for (EntityMethod method : attribute) {
            length = Math.max(WidthCalculator.calculateWidth(method.getMethodName(), padding / 2), length);
        }

        return length;
    }

    private double entityLength(ClassEntity entity) {
        int lineHeight = 14;

        int name = entity.getName().split("<br>").length;
        int headerHeight = name * lineHeight + padding / 2;

        int fieldsHeight = entity.getFields().size() * lineHeight;
        int methodsHeight = entity.getMethods().size() * lineHeight;

        int separators = (entity.getFields().isEmpty() ? 0 : 1)
                + (entity.getMethods().isEmpty() ? 0 : 1);

        return headerHeight + fieldsHeight + methodsHeight + separators * (padding / 2);
    }

}
