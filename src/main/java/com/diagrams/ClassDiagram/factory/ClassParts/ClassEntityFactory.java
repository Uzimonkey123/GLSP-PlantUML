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

            entity.setX(entity.getX() + entityWidth);

            List<String> methodNames = new ArrayList<>();
            for (EntityMethod method : entity.getMethods()) {
                methodNames.add(method.getMethodName());
            }

            List<String> fieldNames = new ArrayList<>();
            for (EntityMethod fields : entity.getFields()) {
                fieldNames.add(fields.getMethodName());
            }

            elements.add(entityBuild.buildEntity(entity, entityWidth, entityHeight, methodNames, fieldNames));
        }
    }

    private double entityAttributesLength(List<EntityMethod> attribute) {
        double length = 0;

        for (EntityMethod method : attribute) {
            length = Math.max(WidthCalculator.calculateWidth(method.getMethodName(), padding), length);
        }

        return length;
    }

    private double entityLength(ClassEntity entity) {
        double length = 0;

        length += entity.getMethods().size() + entity.getFields().size() + padding;
        length += WidthCalculator.calculateWidth(entity.getName(), 7);

        return length;
    }

}
