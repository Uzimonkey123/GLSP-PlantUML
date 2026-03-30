/*
 * File: ClassEntityHandler.java
 * Author: Norman Babiak
 * Description: Handler for all (except special) types of entities
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.Visibility;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.skin.VisibilityModifier;
import net.sourceforge.plantuml.text.Guillemet;

import java.util.ArrayList;
import java.util.List;

public class ClassEntityHandler extends EntityHandler {

    public ClassEntityHandler(EntityHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(String leafType) {
        // Except special entity types, everything same process as "class"
        return switch (leafType) {
            case "TIPS", "POINT_FOR_ASSOCIATION", "CIRCLE", "DESCRIPTION",
                 "STATE_CHOICE", "ASSOCIATION", "LOLLIPOP_FULL", "NOTE" -> false;
            default -> true;
        };
    }

    @Override
    protected ClassEntity createEntity(Entity pumlEntity, String id) {
        String type = pumlEntity.getLeafType().toString();
        String name = String.join("<br>", pumlEntity.getDisplay());

        List<EntityMethod> body = parseRawBody(pumlEntity, name);
        List<EntityMethod> methods = parseMethods(pumlEntity, body);
        List<EntityMethod> fields = parseFields(pumlEntity, body);

        return new ClassEntity(0, 0, id, name, type, methods, fields, body);
    }

    @Override
    protected void postProcess(Entity pumlEntity, ClassEntity entity) {
        if (pumlEntity.getVisibilityModifier() != null) {
            applyVisibility(entity, pumlEntity);
        }

        if (pumlEntity.getStereotype() != null) {
            applyStereotype(entity, pumlEntity);
        }

        if (pumlEntity.getGeneric() != null) {
            entity.setGeneric(pumlEntity.getGeneric());
        }
    }

    /**
     * Method for parsing every item in the body of the given entity, mostly for the custom body
     */
    private List<EntityMethod> parseRawBody(Entity entity, String name) {
        List<EntityMethod> body = new ArrayList<>();

        try {
            for (CharSequence item : entity.getBodier().getRawBody()) {
                String itemStr = item.toString();
                if (itemStr.isEmpty() || itemStr.isBlank()) continue;

                EntityMethod bodyItem = new EntityMethod(itemStr);
                if (itemStr.contains("(") && itemStr.contains(")")) {
                    bodyItem.setField(false);
                }
                body.add(bodyItem);
            }
        } catch (Exception e) {
            System.err.println("WARNING: Cannot get raw body for " + name);
        }

        return body;
    }

    private List<EntityMethod> parseMethods(Entity entity, List<EntityMethod> fallbackBody) {
        List<EntityMethod> methods = new ArrayList<>();

        try {
            for (CharSequence method : entity.getBodier().getMethodsToDisplay()) {
                String itemStr = method.toString();
                if (itemStr.isEmpty() || itemStr.isBlank()) continue;

                EntityMethod entityMethod = new EntityMethod(method.toString());
                entityMethod.setField(false);
                methods.add(entityMethod);
            }
        } catch (UnsupportedOperationException e) {
            // If parsing fails, parses every item with ( ), considering them as methods
            for (EntityMethod item : fallbackBody) {
                String name = item.getMethodName();
                if (name.contains("(") && name.contains(")")) {
                    methods.add(item);
                }
            }
        }

        return methods;
    }

    private List<EntityMethod> parseFields(Entity entity, List<EntityMethod> fallbackBody) {
        List<EntityMethod> fields = new ArrayList<>();

        try {
            for (CharSequence field : entity.getBodier().getFieldsToDisplay()) {
                String itemStr = field.toString();
                if (itemStr.isEmpty() || itemStr.isBlank()) continue;

                EntityMethod entityMethod = new EntityMethod(field.toString());
                entityMethod.setField(true);
                fields.add(entityMethod);
            }
        } catch (UnsupportedOperationException e) {
            // If parsing fails, parses every item without ( ) as field
            for (EntityMethod item : fallbackBody) {
                String name = item.getMethodName();
                if (!name.contains("(") || !name.contains(")")) {
                    fields.add(item);
                }
            }
        }

        return fields;
    }

    /**
     * Apply visibility of the entity by the modifier type
     */
    private void applyVisibility(ClassEntity newEntity, Entity entity) {
        String visibility = switch (entity.getVisibilityModifier()) {
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

    private void applyStereotype(ClassEntity newEntity, Entity entity) {
        newEntity.setStereotype(true);
        newEntity.setStereotypeName(entity.getStereotype().getLabel(Guillemet.DOUBLE_COMPARATOR));
        newEntity.setStereotype(entity.getStereotype().getCharacter());

        if (entity.getStereotype().getHtmlColor() != null) {
            newEntity.setStereotypeColor(entity.getStereotype().getHtmlColor().asString());
        }
    }
}