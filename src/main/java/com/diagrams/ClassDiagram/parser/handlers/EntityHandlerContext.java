/*
 * File: EntityHandlerContext.java
 * Author: Norman Babiak
 * Description: Context for the handlers, handles register, source lines, aliases...
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser.handlers;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.klimt.color.ColorType;

import java.util.List;
import java.util.Map;

import static com.diagrams.ClassDiagram.utils.MapperInfo.addMapperInfo;

public class EntityHandlerContext {

    private final ClassModel model;
    private final ClassLineMapper lineMapper;
    private final ClassLineFinder lineFinder;
    private final Map<Entity, ClassEntity> entityMap;

    public EntityHandlerContext(ClassModel model, ClassLineMapper lineMapper, ClassLineFinder lineFinder,
                                Map<Entity, ClassEntity> entityMapping) {
        this.model = model;
        this.lineMapper = lineMapper;
        this.lineFinder = lineFinder;
        this.entityMap = entityMapping;
    }

    public void register(Entity pumlEntity, ClassEntity classEntity, Package parentPackage) {
        model.entities.add(classEntity);
        entityMap.put(pumlEntity, classEntity);
        applyBackground(pumlEntity, classEntity);

        if (parentPackage != null) {
            parentPackage.addEntity(classEntity);
        }
    }

    public int findEntityLine(String name, ClassEntity entity) {
        return lineFinder.findEntityLine(name, entity);
    }

    public int findNoteLine(String name, ClassEntity entity) {
        return lineFinder.findNoteLine(name, entity);
    }

    public int findNoteEndLine(int startLine, ClassEntity entity) {
        return lineFinder.findNoteEndLine(startLine, entity);
    }

    public void mapToSource(ClassEntity entity, int line) {
        addMapperInfo(entity, line, lineMapper);
    }

    public void mapToSource(ClassEntity entity, int startLine, int endLine) {
        addMapperInfo(entity, startLine, endLine, lineMapper);
    }

    public String extractAlias(int line) {
        if (line < 0) return null;

        ClassLineMapper.LineInfo info = lineMapper.getLineInfo(line);
        if (info == null) return null;

        return ClassLineFinder.extractAlias(info.originalText);
    }

    public void applyAlias(ClassEntity entity, String name, int line) {
        String alias = extractAlias(line);

        if (alias != null && !alias.isEmpty() && !alias.equals(name)) {
            entity.setAlias(alias);
        }
    }

    public boolean opensBlock(int line) {
        ClassLineMapper.LineInfo info = lineMapper.getLineInfo(line);
        return info != null && info.originalText.trim().endsWith("{");
    }

    public int findBlockEnd(int startLine) {
        List<ClassLineMapper.LineInfo> all = lineMapper.getLineInfos();
        int depth = 0;

        for (int i = startLine; i < all.size(); i++) {
            for (char c : all.get(i).originalText.toCharArray()) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }

        return startLine;
    }

    private void applyBackground(Entity pumlEntity, ClassEntity classEntity) {
        if (pumlEntity.getColors().getColor(ColorType.BACK) != null) {
            classEntity.setBackground(pumlEntity.getColors().getColor(ColorType.BACK).asString());
        }
    }

    public ClassModel getModel() {
        return model;
    }
}