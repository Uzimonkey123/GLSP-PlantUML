/*
 * File: WriterContext.java
 * Author: Norman Babiak
 * Description: Shared context for all writer modules
 * Date: 31.3.2026
 */

package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.utils.NewLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class WriterContext {

    private final ClassModel model;
    private final File source;
    private final List<String> sourceLines;
    private final Map<Integer, NewLine> pendingChanges;
    private final ClassLineMapper lineMapper;

    private Map<Integer, String> lineNamespaceMap;
    private Map<String, String> entityNamespaceMap;

    public WriterContext(ClassModel model, File source) throws IOException {
        this.model = model;
        this.source = source;
        this.sourceLines = new ArrayList<>(Files.readAllLines(source.toPath(), StandardCharsets.UTF_8));
        this.pendingChanges = new HashMap<>();
        this.lineMapper = model.getLineMapper();
    }

    public ClassModel getModel() {
        return model;
    }

    public ClassLineMapper getLineMapper() {
        return lineMapper;
    }

    public List<String> getSourceLines() {
        return sourceLines;
    }


    /**
     * Schedules a replacement, replaces when applyReplacement is called
     */
    public void changeLine(int start, int end, List<String> lines) {
        pendingChanges.put(start, new NewLine(start, end, lines));
    }

    /**
     * Returns the effective content of a line
     */
    public String getEffectiveLine(int lineNum) {
        if (pendingChanges.containsKey(lineNum)) {
            List<String> pending = pendingChanges.get(lineNum).newLines();

            return pending.isEmpty() ? "" : pending.getFirst();
        }

        if (lineNum >= 0 && lineNum < sourceLines.size()) {
            return sourceLines.get(lineNum);
        }

        return "";
    }

    /**
     * Returns the token to use when replacing references to this entity in relationship and member lines
     */
    public String getNewToken(ClassEntity entity) {
        String alias = entity.getAlias();
        return isNotEmpty(alias) ? alias : entity.getName();
    }

    /**
     * Returns the token to use in PlantUML syntax
     */
    public String getEntityToken(ClassEntity entity) {
        String alias = entity.getAlias();

        if (isNotEmpty(alias)) {
            return alias;
        }

        return quoteIfNeeded(entity.getName());
    }


    /**
     * Detects the namespace separator from the source
     */
    public String detectSeparator() {
        for (ClassLineMapper.LineInfo info : lineMapper.getLineInfos()) {
            String trimmed = info.originalText.trim();

            if (trimmed.startsWith("set separator")) {
                String sep = trimmed.substring("set separator".length()).trim();
                if (!sep.isEmpty() && !sep.equalsIgnoreCase("none")) return sep;
            }
        }

        return ".";
    }

    public Map<String, String> getEntityNamespaceMap() {
        if (entityNamespaceMap == null) {
            entityNamespaceMap = buildEntityNamespaceMap();
        }
        return entityNamespaceMap;
    }

    public Map<Integer, String> getLineNamespaceMap() {
        if (lineNamespaceMap == null) {
            lineNamespaceMap = buildLineNamespaceMap();
        }
        return lineNamespaceMap;
    }

    private Map<String, String> buildEntityNamespaceMap() {
        Map<String, String> map = new HashMap<>();

        for (ClassEntity entity : model.entities) {
            map.put(entity.getId(), "");
        }

        for (Package pkg : model.packages) {
            mapEntitiesInPackage(pkg, map);
        }

        return map;
    }

    private void mapEntitiesInPackage(Package pkg, Map<String, String> map) {
        for (ClassEntity entity : pkg.getEntities()) {
            map.put(entity.getId(), pkg.getName());
        }

        for (Package child : pkg.getChildPackages()) {
            mapEntitiesInPackage(child, map);
        }
    }

    private Map<Integer, String> buildLineNamespaceMap() {
        Map<Integer, String> map = new HashMap<>();
        Deque<String> nsStack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        int braceDepth = 0;

        for (int i = 0; i < sourceLines.size(); i++) {
            String trimmed = sourceLines.get(i).trim();

            Matcher m = Pattern.compile("^(?:namespace|package)\\s+\"?([^\"\\s{#]+)\"?").matcher(trimmed);

            if (m.find()) {
                String nsName = m.group(1);
                int opens = countChar(trimmed, '{');
                int closes = countChar(trimmed, '}');

                // Only push if the block actually opens
                if (opens > closes) {
                    nsStack.push(nsName);
                    depthStack.push(braceDepth);
                    braceDepth += (opens - closes);
                    map.put(i, nsName);
                    continue;
                }
            }

            int opens = countChar(trimmed, '{');
            int closes = countChar(trimmed, '}');
            braceDepth += opens;
            braceDepth -= closes;

            // Pop namespace when braces drop back to the level before the namespace opened
            while (!depthStack.isEmpty() && braceDepth <= depthStack.peek()) {
                nsStack.pop();
                depthStack.pop();
            }

            map.put(i, nsStack.isEmpty() ? "" : nsStack.peek());
        }

        return map;
    }

    public boolean hasNameConflict(ClassEntity entity) {
        String name = entity.getOriginalName();

        for (ClassEntity other : model.entities) {
            if (other != entity && name.equals(other.getOriginalName())) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldUpdateBareReference(int lineNum, ClassEntity entity) {
        if (!hasNameConflict(entity)) {
            return true;
        }

        // Same namespace safe to update bare name
        // different skip to avoid wrong match
        String entityNs = getEntityNamespaceMap().getOrDefault(entity.getId(), "");
        String lineNs = getLineNamespaceMap().getOrDefault(lineNum, "");

        return entityNs.equals(lineNs);
    }

    public static Map<String, String> buildMemberRefMap(ClassEntity entity) {
        Map<String, String> map = new LinkedHashMap<>();

        for (EntityMethod member : entity.getRawBody()) {
            String oldParsed = parseRawMemberName(member.getOriginalName());
            String oldRef = deriveMemberRef(oldParsed);
            String newRef = deriveMemberRef(member.getMethodName());

            if (oldRef != null && newRef != null && !oldRef.equals(newRef)) {
                map.put(oldRef, newRef);
            }
        }

        return map;
    }

    public static String findNewMemberRef(ClassEntity entity, String oldRef) {
        if (isNullOrEmpty(oldRef)) return null;

        String bareRef = unquote(oldRef);

        for (EntityMethod member : entity.getRawBody()) {
            String oldParsed = parseRawMemberName(member.getOriginalName());
            String oldMemberRef = deriveMemberRef(oldParsed);

            if (bareRef.equals(oldMemberRef)) {
                String newMemberRef = deriveMemberRef(member.getMethodName());
                if (!oldMemberRef.equals(newMemberRef)) {
                    return newMemberRef;
                }
                
                return null;
            }
        }

        return null;
    }

    /**
     * Applies all pending changes to sourceLines in reverse order
     */
    public void applyReplacements() {
        // Reverse order later lines first so earlier line numbers aren't shifted
        List<Integer> sortedLines = new ArrayList<>(pendingChanges.keySet());
        sortedLines.sort(Collections.reverseOrder());

        for (int startLine : sortedLines) {
            NewLine replacement = pendingChanges.get(startLine);

            for (int i = replacement.endLine(); i >= replacement.startLine(); i--) {
                if (i < sourceLines.size()) {
                    sourceLines.remove(i);
                }
            }

            sourceLines.addAll(replacement.startLine(), replacement.newLines());
        }
    }

    /**
     * Atomically writes the source lines back to disk.
     */
    public void saveAtomic() throws IOException {
        File tempFile = new File(source.getParent(), source.getName() + ".tmp");

        Files.write(tempFile.toPath(), sourceLines, StandardCharsets.UTF_8);
        Files.move(
                tempFile.toPath(),
                source.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }

    /**
     * Resets state for a fresh write cycle.
     */
    public void reset() throws IOException {
        sourceLines.clear();
        sourceLines.addAll(Files.readAllLines(source.toPath(), StandardCharsets.UTF_8));
        pendingChanges.clear();
        lineNamespaceMap = null;
        entityNamespaceMap = null;
    }
}
