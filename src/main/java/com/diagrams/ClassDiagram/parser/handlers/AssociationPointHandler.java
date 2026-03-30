/*
 * File: AssociationPointHandler.java
 * Author: Norman Babiak
 * Description: Handler for Association Point type entities
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import net.sourceforge.plantuml.abel.Entity;

public class AssociationPointHandler extends EntityHandler {
    public AssociationPointHandler(EntityHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(String leafType) {
        return "POINT_FOR_ASSOCIATION".equals(leafType);
    }

    @Override
    protected ClassEntity createEntity(Entity pumlEntity, String id) {
        return new ClassEntity(0, 0, id, "", "ASSOCIATION_POINT");
    }

    @Override
    protected SourceLocation findSourceLine(ClassEntity entity) {
        return new SourceLocation(-1);
    }

    @Override
    protected void extractAlias(ClassEntity entity, int line) {
    }
}