/*
 * File: NoteEntityHandler.java
 * Author: Norman Babiak
 * Description: Handler for note entity type
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import net.sourceforge.plantuml.abel.Entity;

public class NoteEntityHandler extends EntityHandler {

    public NoteEntityHandler(EntityHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(String leafType) {
        return "NOTE".equals(leafType);
    }

    @Override
    protected ClassEntity createEntity(Entity pumlEntity, String id) {
        String name = String.join("<br>", pumlEntity.getDisplay());
        return new ClassEntity(0, 0, id, name, "NOTE");
    }

    /**
     * Note uses different syntax for source lines, searching for note-like syntax instead of declaration
     */
    @Override
    protected SourceLocation findSourceLine(ClassEntity entity) {
        int startLine = ctx.findNoteLine(entity.getName(), entity);
        int endLine = ctx.findNoteEndLine(startLine, entity);

        return new SourceLocation(startLine, endLine);
    }

    /**
     * Notes check both startLine and endLine before extracting alias.
     */
    @Override
    protected void extractAlias(ClassEntity entity, int line) {
        ctx.applyAlias(entity, entity.getName(), line);
    }
}