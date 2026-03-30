/*
 * File: ClassDiagramConfiguration.java
 * Author: Norman Babiak
 * Description: Configuration file for class diagram, containing shapes and edge types
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram;

import org.eclipse.glsp.server.diagram.BaseDiagramConfiguration;
import org.eclipse.glsp.server.layout.ServerLayoutKind;
import org.eclipse.glsp.server.types.EdgeTypeHint;
import org.eclipse.glsp.server.types.ShapeTypeHint;

import java.util.List;

public class ClassDiagramConfiguration extends BaseDiagramConfiguration {
    @Override
    public List<ShapeTypeHint> getShapeTypeHints() {
        return List.of(
                new ShapeTypeHint("entity", true, true, false, false),
                new ShapeTypeHint("entity:circle", true, true, false, false),
                new ShapeTypeHint("entity:diamond", true, true, false, false),
                new ShapeTypeHint("entity:association-point", true, true, false, false),
                new ShapeTypeHint("entity:note", true, true, false, false),
                new ShapeTypeHint("entity:lollipop", true, true, false, false),
                new ShapeTypeHint("entity:invis", false, false, false, false),
                new ShapeTypeHint("package-folder", false, true, false, false),
                new ShapeTypeHint("package-rectangle", false, true, false, false),
                new ShapeTypeHint("package-frame", false, true, false, false),
                new ShapeTypeHint("package-node", false, true, false, false),
                new ShapeTypeHint("package-database", false, true, false, false),
                new ShapeTypeHint("package-cloud", false, true, false, false)
        );
    }

    @Override
    public List<EdgeTypeHint> getEdgeTypeHints() {
        List<String> allNodes = List.of(
                "entity", "entity:circle", "entity:diamond",
                "entity:association-point", "entity:lollipop", "entity:note"
        );
        return List.of(
                new EdgeTypeHint("link", false, true, false, allNodes, allNodes),
                new EdgeTypeHint("link:note", false, true, false, allNodes, List.of("entity:note"))
        );
    }

    @Override
    public ShapeTypeHint createDefaultShapeTypeHint(final String elementId) {
        return new ShapeTypeHint(elementId, true, true, true, true);
    }

    @Override
    public String getDiagramType() {
        return "class-diagram";
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
