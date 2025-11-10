package com.diagrams.SequenceDiagram;

import org.eclipse.glsp.server.diagram.BaseDiagramConfiguration;
import org.eclipse.glsp.server.layout.ServerLayoutKind;
import org.eclipse.glsp.server.types.EdgeTypeHint;
import org.eclipse.glsp.server.types.ShapeTypeHint;

import java.util.List;

public class SequenceDiagramConfiguration extends BaseDiagramConfiguration {
    @Override
    public List<ShapeTypeHint> getShapeTypeHints() {
        return List.of();
    }

    @Override
    public List<EdgeTypeHint> getEdgeTypeHints() {
        return List.of();
    }

    @Override
    public ShapeTypeHint createDefaultShapeTypeHint(final String elementId) {
        return new ShapeTypeHint(elementId, true, true, true, true);
    }

    @Override
    public String getDiagramType() {
        return "sequence-diagram";
    }

    @Override
    public ServerLayoutKind getLayoutKind() {
        return ServerLayoutKind.MANUAL;
    }

    @Override
    public boolean needsClientLayout() {
        return false;
    }
}
