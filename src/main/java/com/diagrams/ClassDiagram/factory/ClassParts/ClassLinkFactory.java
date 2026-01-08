package com.diagrams.ClassDiagram.factory.ClassParts;

import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.GModelElement;

import java.util.List;

public class ClassLinkFactory {
    private final ClassModel model;
    private final LinkBuild linkBuild;

    private final List<GModelElement> elements;

    public ClassLinkFactory(ClassModel model, List<GModelElement> elements) {
        this.model = model;
        this.elements = elements;
        this.linkBuild = new LinkBuild();
    }

    public void createLinks() {
        for (ClassLink link : model.links) {
            elements.add(linkBuild.buildLink(link));
        }
    }
}
