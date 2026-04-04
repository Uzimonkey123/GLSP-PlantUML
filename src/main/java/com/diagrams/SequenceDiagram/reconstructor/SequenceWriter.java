/*
 * File: SequenceWriter.java
 * Author: Norman Babiak
 * Description: Orchestrates writing of modified sequence diagram elements back to PlantUML source.
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.reconstructor.writers.*;
import com.diagrams.SequenceDiagram.utils.NewLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SequenceWriter {
    private final SequenceModel model;
    private final File source;
    private final List<String> sourceLines;
    private final SequenceWriterContext ctx;

    public SequenceWriter(SequenceModel model, String sourceUri) throws IOException {
        this.model = model;
        this.source = new File(URI.create(sourceUri));
        this.sourceLines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);

        this.ctx = new SequenceWriterContext(model, sourceLines, model.getLineMapper());
    }

    /**
     * Writes all pending modifications to the PlantUML source file. Processes deletions first, then delegates to each element-specific writer,
     * applies all accumulated replacements, and saves atomically.
     */
    public void write() throws IOException {
        ctx.clearReplacements();

        // Process pending line deletions from the delete handler
        for (int[] range : model.getLinesToDelete()) {
            ctx.changeLine(range[0], range[1], Collections.emptyList());
        }
        model.clearLinesToDelete();

        // Delegate to element-specific writers
        new ParticipantWriter(ctx).write();
        new MessageWriter(ctx).write();
        new AnchorWriter(ctx).write();
        new GroupWriter(ctx).write();
        new PageDetailsWriter(ctx).write();
        new EngloberWriter(ctx).write();

        applyReplacements();
        saveAtomic();
    }

    /**
     * Applies all accumulated line replacements in reverse order
     */
    private void applyReplacements() {
        List<Integer> sortedLines = new ArrayList<>(ctx.getNewLines().keySet());
        sortedLines.sort(Collections.reverseOrder());

        for (int startLine : sortedLines) {
            NewLine replacement = ctx.getNewLines().get(startLine);

            // Remove old lines
            for (int i = replacement.endLine(); i >= replacement.startLine(); i--) {
                if (i < sourceLines.size()) {
                    sourceLines.remove(i);
                }
            }

            // Insert new lines
            sourceLines.addAll(replacement.startLine(), replacement.newLines());
        }
    }

    /**
     * Writes to a temporary file first, then atomically replaces the original
     */
    private void saveAtomic() throws IOException {
        File tempFile = new File(source.getParent(), source.getName() + ".tmp");

        Files.write(tempFile.toPath(), sourceLines, StandardCharsets.UTF_8);
        Files.move(
                tempFile.toPath(),
                source.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }
}