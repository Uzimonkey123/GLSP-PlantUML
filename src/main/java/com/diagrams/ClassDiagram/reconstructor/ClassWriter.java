/*
 * File: ClassWriter.java
 * Author: Norman Babiak
 * Description: Handles write operations and deletions
 * Date: 5.5.2026
 */

package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.reconstructor.writers.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

public class ClassWriter {

    private final ClassModel model;
    private final WriterContext context;

    public ClassWriter(ClassModel model, String sourceUri) throws IOException {
        this.model = model;
        this.context = new WriterContext(model, new File(URI.create(sourceUri)));
    }

    /**
     * Runs the full write pipeline: deletions, entity/package/link/note/page rewrites,
     * reference updates, then applies all changes and saves
     */
    public void write() throws IOException {
        context.reset();

        // Apply pending deletions from delete operations
        for (int[] range : model.getLinesToDelete()) {
            context.changeLine(range[0], range[1], Collections.emptyList());
        }
        model.clearLinesToDelete();

        new EntityWriter(context).write();

        // Update references for entities
        ReferenceUpdater refUpdater = new ReferenceUpdater(context);
        refUpdater.updateInheritanceReferences();
        refUpdater.updateMemberReferences();

        new PackageWriter(context).write();
        new LinkWriter(context).write();
        new NoteWriter(context).write();
        new PageDetailWriter(context).write();

        // Group up changes and save
        context.applyReplacements();
        context.saveAtomic();
    }
}