package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceNote;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

import java.util.List;

public class NoteBuild {
    public GModelElement buildNote(SequenceNote note, String source, String target,
                                   double x1, double x2, double y1, double y2) {
        return new GEdgeBuilder("edge:notes")
                .id("msg-" + note.getId())
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

    public GModelElement buildNoteLabel(SequenceNote note, double x1, double x2, double y) {
        return new GLabelBuilder("label:html")
                .id(note.getId())
                .text(note.getLabel())
                .position((x1 + x2) / 2, y)
                .size(10, 10)
                .build();
    }
}
