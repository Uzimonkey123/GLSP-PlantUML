/*
 * File: NoteBuild.java
 * Author: Norman Babiak
 * Description: GModelElement builder for notes
 * Date: 6.5.2026
 */

package com.diagrams.SequenceDiagram.builders;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNote;
import com.GLSPPlantUML.utils.WidthCalculator;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

public class NoteBuild {
    /**
     * Builds a GEdge note object on the given msgId coordinates
     */
    public GModelElement buildNote(String msgId, SequenceNote note, String source, String target,
                                   double x1, double x2, double y1, double y2) {
        return new GEdgeBuilder("edge:notes")
                .id(msgId + "-note")
                .sourceId(source)
                .targetId(target)
                .addArgument("x1", x1)
                .addArgument("x2", x2)
                .addArgument("y1", y1)
                .addArgument("y2", y2)
                .addArgument("color", note.getBackground())
                .addArgument("shape", note.getShape())
                .build();
    }

    /**
     * Builds the GLabel for the note
     */
    public GModelElement buildNoteLabel(SequenceNote note, double x1, double x2, double y) {
        return new GLabelBuilder("label:html")
                .id(note.getId())
                .text(note.getLabel())
                .position((x1 + x2) / 2, y)
                .size(WidthCalculator.calculateWidth(note.getLabel(), 0), 10)
                .build();
    }
}
