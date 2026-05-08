/*
 * File: EntityHandler.java
 * Author: Norman Babiak
 * Description: Setup for entity handlers, abstract class
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import net.sourceforge.plantuml.abel.Entity;

public abstract class EntityHandler {
    protected final EntityHandlerContext ctx;

    protected EntityHandler(EntityHandlerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * @return true if this handler can process the given PlantUML leaf type
     */
    public abstract boolean canHandle(String leafType);

    /**
     * Creates, registers, locates source lines, parses alias and handles post process
     * Handlers extend this by the type they are, flow stays same
     */
    public final ClassEntity handle(Entity pumlEntity, String id, Package parentPackage) {
        ClassEntity entity = createEntity(pumlEntity, id);

        // Register entity into model, mapping and its package
        ctx.register(pumlEntity, entity, parentPackage);

        SourceLocation location = findSourceLine(entity);
        if (location.isMultiLine()) {
            ctx.mapToSource(entity, location.startLine(), location.endLine());

        } else {
            ctx.mapToSource(entity, location.startLine());
        }

        if (location.startLine() >= 0) {
            extractAlias(entity, location.startLine());
        }

        postProcess(pumlEntity, entity);

        return entity;
    }

    /**
     * Creates the internal ClassEntity from the PlantUML entity
     */
    protected abstract ClassEntity createEntity(Entity pumlEntity, String id);

    /**
     * Find where this entity lives in the PlantUML source text.
     * Default uses entity line search, notes override to use note search.
     */
    protected SourceLocation findSourceLine(ClassEntity entity) {
        int line = ctx.findEntityLine(entity.getName(), entity);

        if (line >= 0 && ctx.opensBlock(line)) {
            int endLine = ctx.findBlockEnd(line);

            return new SourceLocation(line, endLine);
        }

        return new SourceLocation(line);
    }

    /**
     * Extract and apply alias from the source line.
     */
    protected void extractAlias(ClassEntity entity, int line) {
        ctx.applyAlias(entity, entity.getName(), line);
    }

    /**
     * Hook for type-specific processing, like setting stereotype, generic...
     */
    protected void postProcess(Entity pumlEntity, ClassEntity entity) {
    }

    /**
     * Simple value object for line range results.
     */
    public record SourceLocation(int startLine, int endLine) {
        public SourceLocation(int singleLine) {
            this(singleLine, singleLine);
        }

        public boolean isMultiLine() {
            return startLine >= 0 && endLine != startLine;
        }
    }
}
