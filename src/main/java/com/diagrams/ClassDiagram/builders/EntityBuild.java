package com.diagrams.ClassDiagram.builders;

import com.GLSPPlantUML.utils.WidthCalculator;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import org.eclipse.glsp.graph.GCompartment;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GCompartmentBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;

import java.util.List;
import java.util.Map;

public class EntityBuild {
    private final int lineHeight = 14;
    private final int padding = 5;

    private int computeHeaderH(ClassEntity entity) {
        String stereoName = entity.getStereotypeName();
        return (stereoName != null && !stereoName.isEmpty()) ? 44 : 30;
    }

    private int computeFieldH(List<String> fields) {
        int minSectionHeight = 10;

        return !fields.isEmpty()
                ? fields.size() * lineHeight + padding * 2
                : minSectionHeight;
    }

    public GModelElement buildEntity(ClassEntity entity, double width, double height,
                                     List<String> methods, List<String> fields, List<String> bodyLines) {

        int headerH = computeHeaderH(entity);
        int fieldH  = computeFieldH(fields);

        String genericText = entity.isGeneric() ? entity.getGeneric() : "";
        int charWidth   = 7;
        int boxPadding  = 10;
        int genericBoxW = entity.isGeneric()
                ? (genericText.length() * charWidth + boxPadding)
                : 50;
        int genericBoxH = 20;
        double genericBoxX = width - genericBoxW;
        double nameLabelX  = entity.isGeneric() ? genericBoxX / 2.0 : width / 2.0;

        GNodeBuilder nodeBuilder = new GNodeBuilder("entity")
                .id(entity.getId())
                .layout("freeform")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType())
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:entityName")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .position(nameLabelX, headerH / 2.0)
                .size(width - nameLabelX, headerH)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("width", width)
                .addArgument("visibility", entity.getVisibility())
                .addArgument("height", height)
                .addArgument("stereotypeName", entity.getStereotypeName())
                .addArgument("stereotypeChar", String.valueOf(entity.getStereotypeChar()))
                .addArgument("stereotypeColor", entity.getStereotypeColor())
                .addArgument("hasGeneric", entity.isGeneric())
                .build());

        if (entity.isGeneric()) {
            nodeBuilder.add(new GLabelBuilder("label:generic")
                    .id(entity.getId() + "generic")
                    .text(entity.getGeneric())
                    .position(genericBoxX + genericBoxW / 2.0, genericBoxH / 2.0)
                    .size(genericBoxW, genericBoxH)
                    .build());
        }

        for (int i = 0; i < fields.size(); i++) {
            double fieldY = headerH + padding + (i * lineHeight) + 5;

            nodeBuilder.add(new GLabelBuilder("label:field")
                    .id(entity.getId() + "-field-" + i)
                    .text(fields.get(i))
                    .position(width / 2.0, fieldY)
                    .size(width / 2.0, lineHeight)
                    .addArgument("visibility", entity.getFields().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        for (int i = 0; i < methods.size(); i++) {
            double methodY = headerH + fieldH + padding + (i * lineHeight) + 5;

            nodeBuilder.add(new GLabelBuilder("label:method")
                    .id(entity.getId() + "-method-" + i)
                    .text(methods.get(i))
                    .position(width / 2.0, methodY)
                    .size(width / 2.0, lineHeight)
                    .addArgument("visibility", entity.getMethods().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        for (int i = 0; i < bodyLines.size(); i++) {
            double bodyY = headerH + 10 + (i * lineHeight);
            nodeBuilder.add(new GLabelBuilder("label:body")
                    .id(entity.getId() + "-body-" + i)
                    .text(bodyLines.get(i))
                    .position(width / 2.0, bodyY)
                    .size(width / 2.0, lineHeight)
                    .addArgument("visibility", entity.getRawBody().get(i).getVisibilityChar())
                    .addArgument("boxWidth", width)
                    .build());
        }

        return nodeBuilder.build();
    }

    public GModelElement buildCircleEntity(ClassEntity entity, double width, double height) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:circle")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:method")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("width", width)
                .addArgument("visibility", entity.getVisibility())
                .build());

        return nodeBuilder.build();
    }

    public GModelElement buildLollipopEntity(ClassEntity entity, double width, double height) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:lollipop")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:method")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("width", width)
                .addArgument("visibility", entity.getVisibility())
                .build());

        return nodeBuilder.build();
    }

    public GModelElement buildDiamondEntity(ClassEntity entity, double width) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:diamond")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, width)
                .addArgument("type", entity.getType().toLowerCase())
                .addArgument("background", entity.getBackground());

        return nodeBuilder.build();
    }

    public GModelElement buildAssociationPoint(ClassEntity entity) {
        GNodeBuilder point = new GNodeBuilder("entity:association-point")
                .id(entity.getId())
                .position(entity.getX(), entity.getY())
                .size(8, 8);

        return point.build();
    }

    public GModelElement buildNoteEntity(ClassEntity entity, double width, double height) {
        GNodeBuilder nodeBuilder = new GNodeBuilder("entity:note")
                .id(entity.getId())
                .layout("vbox")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", "note")
                .addArgument("background", entity.getBackground());

        nodeBuilder.add(new GLabelBuilder("label:note")
                .id(entity.getId() + "-label-name")
                .text(entity.getName())
                .addArgument("type", "note")
                .addArgument("width", width)
                .build());

        return nodeBuilder.build();
    }

    public GCompartment buildPackage(Package pkg, double width, double height) {
        GCompartmentBuilder packageContainer = new GCompartmentBuilder("package-" + pkg.getType().toLowerCase())
                .id(pkg.getId())
                .layout("freeform")
                .position(pkg.getX(), pkg.getY())
                .size(width, height);

        double headerX = 50;
        double headerY = 20;

        switch (pkg.getType().toLowerCase()) {
            case "node" -> { headerY = 25;}
            case "folder", "frame" -> { headerX = 40;}
            case "database" -> { headerY = 14;}
            case "cloud" -> { headerX = 55; headerY = 25;}
        }

        // Add package header with name
        GCompartmentBuilder header = new GCompartmentBuilder("comp:header")
                .id(pkg.getId() + "-header")
                .layout("hbox")
                .position(headerX, headerY);

        // Package name label
        GLabelBuilder nameLabel = new GLabelBuilder("label:heading")
                .id(pkg.getId() + "-name")
                .text(pkg.getName())
                .position(0, 0);

        header.add(nameLabel.build());
        packageContainer.add(header.build());

        if (pkg.getAnchorX() >= 0) {
            GNodeBuilder anchor = new GNodeBuilder("entity:invis")
                    .id(pkg.getAnchorId())
                    .position(pkg.getAnchorX(), pkg.getAnchorY())
                    .size(1, 1);

            packageContainer.add(anchor.build());
        }

        packageContainer.addArgument("background", pkg.getBackground());
        packageContainer.addArgument("depth", String.valueOf(pkg.getDepth()));
        packageContainer.addArgument("isTopLevel", String.valueOf(pkg.isTopLevel()));
        packageContainer.addArgument("headerHeight", String.valueOf(pkg.getHeaderHeight()));
        packageContainer.addArgument("labelWidth", String.valueOf((int) pkg.estimateLabelWidth()));

        return packageContainer.build();
    }

    public void buildPageDetails(List<GModelElement> elements, ClassModel model,
                                 Map<String, ClassLayout.Size> dimensions,
                                 List<ClassEntity> entities) {

        if (entities.isEmpty()) return;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (ClassEntity entity : entities) {
            ClassLayout.Size size = dimensions.get(entity.getId());
            if (size == null) continue;

            minX = Math.min(minX, entity.getX());
            minY = Math.min(minY, entity.getY());
            maxX = Math.max(maxX, entity.getX() + size.width);
            maxY = Math.max(maxY, entity.getY() + size.height);
        }

        double centerX = (minX + maxX) / 2;
        int padding = 30;

        elements.add(new GLabelBuilder("label:body")
                .id("header")
                .size(WidthCalculator.calculateWidth(model.header, 0), yOffset(model.header))
                .position(maxX, minY - padding - yOffset(model.header))
                .text(model.header)
                .build());

        elements.add(new GLabelBuilder("label:body")
                .id("title")
                .size(WidthCalculator.calculateWidth(model.title, 0), yOffset(model.title))
                .position(centerX, minY - padding - yOffset(model.title))
                .text(model.title)
                .build());

        elements.add(new GLabelBuilder("label:body")
                .id("footer")
                .size(WidthCalculator.calculateWidth(model.footer, 0), yOffset(model.footer))
                .position(centerX, maxY + padding)
                .text(model.footer)
                .build());
    }

    private double yOffset(String lines) {
        if (lines == null) {
            return 0;
        }

        int labelHeight = 14;
        int lineCount = lines.split("<br>").length;
        return lineCount > 1 ? lineCount * labelHeight : 6;
    }
}

