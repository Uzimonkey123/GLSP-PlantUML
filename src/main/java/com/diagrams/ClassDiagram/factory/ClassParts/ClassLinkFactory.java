package com.diagrams.ClassDiagram.factory.ClassParts;

import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;

import java.util.List;

public class ClassLinkFactory {
    private final ClassModel model;
    private final LinkBuild linkBuild;

    private final List<GModelElement> elements;
    private final ClassEntityFactory entityFactory;

    public ClassLinkFactory(ClassModel model, List<GModelElement> elements, ClassEntityFactory entityFactory) {
        this.model = model;
        this.elements = elements;
        this.linkBuild = new LinkBuild();
        this.entityFactory = entityFactory;
    }

    public void createLinks() {
        for (ClassLink link : model.links) {
            if(link.getType().equals("INVISIBLE")) {
                continue;
            }

            elements.add(linkBuild.buildLink(link));
        }

        createTipLinks();
    }

    private void createTipLinks() {
        for (ClassEntityFactory.TipInfo tipInfo : entityFactory.tipInfoList) {
            GEdgeBuilder edge = new GEdgeBuilder("link:note")
                    .id("edge-" + tipInfo.tipId)
                    .sourceId(tipInfo.parentEntityId)
                    .targetId(tipInfo.tipId)
                    .addArgument("memberName", tipInfo.memberName);

            elements.add(edge.build());
        }
    }
}