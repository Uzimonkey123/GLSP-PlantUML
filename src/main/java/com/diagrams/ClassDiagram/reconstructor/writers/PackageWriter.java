/*
 * File: PackageWriter.java
 * Author: Norman Babiak
 * Description: Writes modified packages back to source
 * Date: 31.3.2026
 */

package com.diagrams.ClassDiagram.reconstructor.writers;

import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.reconstructor.WriterContext;

import java.util.*;

import static com.diagrams.ClassDiagram.utils.WriterUtils.*;

public class PackageWriter {

    private final WriterContext ctx;

    public PackageWriter(WriterContext ctx) {
        this.ctx = ctx;
    }

    public void write() {
        // Deepest packages first so child renames happen before parent path rebuilds
        List<Package> modified = ctx.getModel().packages.stream()
                .filter(Package::isModified)
                .sorted(Comparator.comparingInt(PackageWriter::getDepth).reversed())
                .toList();

        for (Package pkg : modified) {
            if (!pkg.isModified()) continue;

            if (pkg.hasLine()) {
                rewritePackageDeclaration(pkg);
            }

            updatePackageReferences(pkg);
        }
    }

    private void rewritePackageDeclaration(Package pkg) {
        int lineNum = pkg.getSourceLineStart();
        String line = ctx.getEffectiveLine(lineNum);

        String oldName = pkg.getOriginalName();
        String newName = pkg.getName();
        if (oldName.equals(newName)) return;

        // Replace both bare and quoted forms of the package name
        String updated = replaceWordBoundary(line, oldName, newName);
        updated = updated.replace("\"" + oldName + "\"", "\"" + newName + "\"");

        if (!updated.equals(line)) {
            ctx.changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    private void updatePackageReferences(Package pkg) {
        // Build full qualified path
        String oldPath = buildFullPath(pkg, true);
        String newPath = buildFullPath(pkg, false);
        if (oldPath.equals(newPath)) return;

        boolean needsQuotes = newPath.contains(" ");
        String quotedNew = needsQuotes ? '"' + newPath + '"' : newPath;

        for (ClassLineMapper.LineInfo info : ctx.getLineMapper().getLineInfos()) {
            if (info.type == ClassLineMapper.LineType.PACKAGE_DECLARATION
                    || info.type == ClassLineMapper.LineType.ENTITY_DECLARATION
                    || info.type == ClassLineMapper.LineType.ENTITY_INLINE
                    || info.type == ClassLineMapper.LineType.RELATIONSHIP
                    || info.type == ClassLineMapper.LineType.MEMBER
                    || info.type == ClassLineMapper.LineType.UNKNOWN) {
                updatePackageReferenceLine(info.lineNumber, oldPath, quotedNew);
            }
        }
    }

    private void updatePackageReferenceLine(int lineNum, String oldPath, String newPath) {
        String current = ctx.getEffectiveLine(lineNum);

        ClassLineMapper.LineType type = ctx.getLineMapper().getLineInfo(lineNum).type;
        String updated;

        if (type == ClassLineMapper.LineType.RELATIONSHIP
                || type == ClassLineMapper.LineType.MEMBER) {
            // Only replace before : to avoid corrupting the label text
            int labelIdx = current.indexOf(" : ");

            if (labelIdx >= 0) {
                String before = current.substring(0, labelIdx);
                String after = current.substring(labelIdx);
                before = replaceWordBoundary(before, oldPath, newPath);
                before = before.replace('"' + oldPath + '"', newPath);
                updated = before + after;

            } else {
                updated = replaceWordBoundary(current, oldPath, newPath);
                updated = updated.replace('"' + oldPath + '"', newPath);
            }

        } else {
            updated = replaceWordBoundary(current, oldPath, newPath);
            updated = updated.replace('"' + oldPath + '"', newPath);
        }

        if (!updated.equals(current)) {
            ctx.changeLine(lineNum, lineNum, List.of(updated));
        }
    }

    /**
     * Builds "parent.child.pkg" by walking up the parent chain and reversing.
     */
    private String buildFullPath(Package pkg, boolean useOriginal) {
        List<String> parts = new ArrayList<>();
        Package current = pkg;

        while (current != null) {
            parts.add(useOriginal ? current.getOriginalName() : current.getName());
            current = current.getParentPackage();
        }

        Collections.reverse(parts);
        return String.join(ctx.detectSeparator(), parts);
    }

    private static int getDepth(Package pkg) {
        int depth = 0;

        while (pkg != null) {
            depth++;
            pkg = pkg.getParentPackage();
        }

        return depth;
    }
}
