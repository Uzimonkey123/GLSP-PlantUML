/*
 * File: DiamondEntityHandler.java
 * Author: Norman Babiak
 * Description: Handler for diamond entity type
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import net.sourceforge.plantuml.abel.Entity;

public class DiamondEntityHandler extends EntityHandler {

    public DiamondEntityHandler(EntityHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(String leafType) {
        return "STATE_CHOICE".equals(leafType) || "ASSOCIATION".equals(leafType);
    }

    @Override
    protected ClassEntity createEntity(Entity pumlEntity, String id) {
        String name = String.join("<br>", pumlEntity.getName());
        return new ClassEntity(0, 0, id, name, "DIAMOND");
    }
}