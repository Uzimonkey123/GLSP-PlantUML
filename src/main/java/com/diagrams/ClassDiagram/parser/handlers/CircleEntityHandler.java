/*
 * File: CircleEntityHandler.java
 * Author: Norman Babiak
 * Description: Handler for CIRCLE type entities
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import net.sourceforge.plantuml.abel.Entity;


public class CircleEntityHandler extends EntityHandler {

    public CircleEntityHandler(EntityHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(String leafType) {
        return "CIRCLE".equals(leafType) || "DESCRIPTION".equals(leafType);
    }

    @Override
    protected ClassEntity createEntity(Entity pumlEntity, String id) {
        String name = String.join("<br>", pumlEntity.getDisplay());
        return new ClassEntity(0, 0, id, name, "CIRCLE");
    }
}