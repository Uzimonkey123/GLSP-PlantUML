/*
 * File: ReferenceUpdater.java
 * Author: Norman Babiak
 * Description: Updates entity references or members when changed
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class ReferenceUpdater {

    private final WriterContext ctx;

    public ReferenceUpdater(WriterContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Updates "extends/implements OldName" clauses in entity declarations when the parent entity was renamed
     */
    public void updateInheritanceReferences() {
        Map<String, String> nameChanges = new HashMap<>();

        for (ClassEntity entity : ctx.getModel().entities) {
            if (!entity.isModified()) continue;

            String oldName = entity.getOriginalName();
            String newName = entity.getName();

            if (!oldName.equals(newName)) {
                nameChanges.put(oldName, newName);
            }
        }

        if (nameChanges.isEmpty()) return;

        // Only entity declarations can have inheritance clauses
        for (ClassLineMapper.LineInfo info : ctx.getLineMapper().getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.ENTITY_DECLARATION && info.type != ClassLineMapper.LineType.ENTITY_INLINE) {
                continue;
            }

            int lineNum = info.lineNumber;
            String line = ctx.getEffectiveLine(lineNum);

            Matcher m = Pattern.compile("\\b(extends|implements)\\b(.*)").matcher(line);
            if (!m.find()) continue;

            // Split into: before inheritance - keyword - parent names - block opening
            String beforeInheritance = line.substring(0, m.start());
            String keyword = m.group(1);
            String tail = m.group(2);

            // Separate the brace from the tail so we only replace in the parent name part
            String afterTail = "";
            int braceIdx = tail.indexOf('{');
            if (braceIdx >= 0) {
                afterTail = tail.substring(braceIdx);
                tail = tail.substring(0, braceIdx);
            }

            String originalTail = tail;
            for (Map.Entry<String, String> change : nameChanges.entrySet()) {
                tail = replaceWordBoundary(tail, change.getKey(), change.getValue());
            }

            if (!tail.equals(originalTail)) {
                String updated = beforeInheritance + keyword + tail + afterTail;
                ctx.changeLine(lineNum, lineNum, List.of(updated));
            }
        }
    }

    /**
     * Updates "Entity::oldMember" references in relationship, note, and member lines when members have been renamed
     */
    public void updateMemberReferences() {
        Map<ClassEntity, Map<String, String>> entityMemberMaps = new LinkedHashMap<>();

        for (ClassEntity entity : ctx.getModel().entities) {
            if ("NOTE".equals(entity.getType())) continue;

            Map<String, String> refMap = WriterContext.buildMemberRefMap(entity);

            if (!refMap.isEmpty()) {
                entityMemberMaps.put(entity, refMap);
            }
        }

        if (entityMemberMaps.isEmpty()) return;

        for (ClassLineMapper.LineInfo info : ctx.getLineMapper().getLineInfos()) {
            if (info.type != ClassLineMapper.LineType.RELATIONSHIP
                    && info.type != ClassLineMapper.LineType.NOTE
                    && info.type != ClassLineMapper.LineType.MEMBER) continue;

            String current = ctx.getEffectiveLine(info.lineNumber);
            // Skip lines without :: - no member references to update
            if (!current.contains("::")) continue;

            String updated = current;

            for (var entry : entityMemberMaps.entrySet()) {
                ClassEntity entity = entry.getKey();
                Map<String, String> refMap = entry.getValue();

                // Try all possible tokens the entity might be referenced by
                for (String token : new String[]{
                        entity.getOriginalName(), entity.getName(), entity.getAlias()}) {
                    if (isNullOrEmpty(token)) continue;
                    updated = replaceMemberRefsForToken(updated, token, refMap);
                }
            }

            if (!updated.equals(current)) {
                ctx.changeLine(info.lineNumber, info.lineNumber, List.of(updated));
            }
        }
    }
}
