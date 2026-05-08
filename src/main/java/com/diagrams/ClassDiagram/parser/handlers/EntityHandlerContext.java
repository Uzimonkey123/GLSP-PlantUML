/*
 * File: EntityHandlerContext.java
 * Author: Norman Babiak
 * Description: Context for the handlers, handles register, source lines, aliases...
 * Date: 5.5.2026
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

    /**
     * Adds the entity to the model and mapping, applies background color, and assigns it to its package
     */
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

    /**
     * Maps an entity to a single source line
     */
    public void mapToSource(ClassEntity entity, int line) {
        addMapperInfo(entity, line, lineMapper);
    }

    /**
     * Maps an entity to a source line range
     */
    public void mapToSource(ClassEntity entity, int startLine, int endLine) {
        addMapperInfo(entity, startLine, endLine, lineMapper);
    }

    /**
     * Extracts the alias from a source line if present (class Foo as F)
     */
    public String extractAlias(int line) {
        if (line < 0) return null;

        ClassLineMapper.LineInfo info = lineMapper.getLineInfo(line);
        if (info == null) return null;

        return ClassLineFinder.extractAlias(info.originalText);
    }

    /**
     * Extracts and sets the alias on the entity if it differs from the entity name
     */
    public void applyAlias(ClassEntity entity, String name, int line) {
        String alias = extractAlias(line);

        if (alias != null && !alias.isEmpty() && !alias.equals(name)) {
            entity.setAlias(alias);
        }
    }

    /**
     * Checks if the given source line ends with "{" indicating block-style
     */
    public boolean opensBlock(int line) {
        ClassLineMapper.LineInfo info = lineMapper.getLineInfo(line);
        return info != null && info.originalText.trim().endsWith("{");
    }

    /**
     * Finds the closing "}" line for a block that opens at startLine by tracking brace depth
     */
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

    /**
     * Applies background color from the PlantUML entity if one is set
     */
    private void applyBackground(Entity pumlEntity, ClassEntity classEntity) {
        if (pumlEntity.getColors().getColor(ColorType.BACK) != null) {
            classEntity.setBackground(pumlEntity.getColors().getColor(ColorType.BACK).asString());
        }
    }

    public ClassModel getModel() {
        return model;
    }
}