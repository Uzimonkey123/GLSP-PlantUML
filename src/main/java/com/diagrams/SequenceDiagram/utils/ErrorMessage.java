package com.diagrams.SequenceDiagram.utils;

import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

public class ErrorMessage {
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public GModelElement buildError() {
        return new GLabelBuilder("label:html")
                .position(0, 0)
                .text(message)
                .build();
    }
}
