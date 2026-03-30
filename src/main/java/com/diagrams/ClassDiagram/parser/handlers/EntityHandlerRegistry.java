/*
 * File: EntityHandlerRegistry.java
 * Author: Norman Babiak
 * Description: Registry that handles picking of the correct handler according to type
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import net.sourceforge.plantuml.abel.Entity;

import java.util.List;

public class EntityHandlerRegistry {
    private final List<EntityHandler> handlers;

    public EntityHandlerRegistry(EntityHandlerContext ctx) {
        this.handlers = List.of(
                new AssociationPointHandler(ctx),
                new CircleEntityHandler(ctx),
                new DiamondEntityHandler(ctx),
                new LollipopEntityHandler(ctx),
                new NoteEntityHandler(ctx),
                new ClassEntityHandler(ctx)
        );
    }

    public ClassEntity handle(Entity pumlEntity, String id, Package parentPackage) {
        String leafType = pumlEntity.getLeafType().toString();

        if ("TIPS".equals(leafType)) {
            return null;
        }

        for (EntityHandler handler : handlers) {
            if (handler.canHandle(leafType)) {
                return handler.handle(pumlEntity, id, parentPackage);
            }
        }

        throw new IllegalStateException("No handler found for type: " + leafType);
    }
}