/*
 * File: EntityBuild.java
 * Author: Norman Babiak
 * Description: Builder for entity elements
 * Date: 4.5.2026
 */

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

    /**
     * Computes the header compartment height, which is taller when a stereotype line is present
     *
     * @param entity the class entity to inspect
     * @return header height in pixels
     */
    private int computeHeaderH(ClassEntity entity) {
        String stereoName = entity.getStereotypeName();
        return (stereoName != null && !stereoName.isEmpty()) ? 44 : 30;
    }

    /**
     * Computes the field compartment height based on the number of field lines
     *
     * @param fields list of field display strings
     * @return field compartment height
     */
    private int computeFieldH(List<String> fields) {
        int minSectionHeight = 10;

        return !fields.isEmpty()
                ? fields.size() * lineHeight + padding * 2
                : minSectionHeight;
    }

    /**
     * Builds a standard rectangular class entity node with header
     *
     * @param entity the parsed class entity with position and metadata
     * @param width pre-calculated entity width
     * @param height pre-calculated entity height
     * @param methods formatted method display strings
     * @param fields formatted field display strings
     * @param bodyLines raw body lines
     * @return the GNode graph element
     */
    public GModelElement buildEntity(ClassEntity entity, double width, double height,
                                     List<String> methods, List<String> fields, List<String> bodyLines) {

        int headerH = computeHeaderH(entity);
        int fieldH  = computeFieldH(fields);

        String genericText = entity.isGeneric() ? entity.getGeneric() : "";
        int charWidth   = 7;
        int boxPadding  = 10;
        int genericBoxW = entity.isGeneric() ? (genericText.length() * charWidth + boxPadding) : 50;
        int genericBoxH = 20;
        double genericBoxX = width - genericBoxW;
        double nameLabelX  = entity.isGeneric() ? genericBoxX / 2.0 : width / 2.0;

        boolean hasStereotype = entity.getStereotypeName() != null && !entity.getStereotypeName().isEmpty();
        double stereotypeY = 10;

        GNodeBuilder nodeBuilder = new GNodeBuilder("entity")
                .id(entity.getId())
                .layout("freeform")
                .position(entity.getX(), entity.getY())
                .size(width, height)
                .addArgument("type", entity.getType())
                .addArgument("background", entity.getBackground());

        if (hasStereotype) {
            nodeBuilder.add(new GLabelBuilder("label:stereotype")
                    .id(entity.getId() + "-label-stereotype")
                    .text(entity.getStereotypeName())
                    .position(nameLabelX, stereotypeY)
                    .size(width, 14)
                    .addArgument("boxWidth", width)
                    .build());
        }

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
                    .id(entity.getId() + "-generic")
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
                    .addArgument("isField", entity.getFields().get(i).isField())
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
                    .addArgument("isField", entity.getRawBody().get(i).isField())
                    .build());
        }

        return nodeBuilder.build();
    }

    /**
     * Builds a circle-shaped entity node with a single name label below
     *
     * @param entity the parsed circle entity
     * @param width pre-calculated width
     * @param height pre-calculated height
     * @return theGNode graph element
     */
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

    /**
     * Builds a lollipop entity node with a name label below
     *
     * @param entity the parsed lollipop entity
     * @param width pre-calculated width
     * @param height pre-calculated height
     * @return the assembled GNode graph element
     */
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

    /**
     * Builds a diamond entity node
     *
     * @param entity the parsed diamond entity
     * @param width the side length
     * @return the GNode graph element
     */
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

    /**
     * Builds a small filled dot node used as the center point
     *
     * @param entity the parsed association point entity
     * @return theGNode graph element
     */
    public GModelElement buildAssociationPoint(ClassEntity entity) {
        GNodeBuilder point = new GNodeBuilder("entity:association-point")
                .id(entity.getId())
                .position(entity.getX(), entity.getY())
                .size(8, 8);

        return point.build();
    }

    /**
     * Builds a note shaped entity with a single text label inside
     *
     * @param entity the parsed note entity
     * @param width pre-calculated width
     * @param height pre-calculated height
     * @return theGNode graph element
     */
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

    /**
     * Builds a package with a positioned header label
     *
     * @param pkg the parsed package
     * @param width pre-calculated package width
     * @param height pre-calculated package height
     * @return the GCompartment graph element
     */
    public GCompartment buildPackage(Package pkg, double width, double height) {
        GCompartmentBuilder packageContainer = new GCompartmentBuilder("package-" + pkg.getType().toLowerCase())
                .id(pkg.getId())
                .layout("freeform")
                .position(pkg.getX(), pkg.getY())
                .size(width, height);

        double headerX = pkg.estimateLabelWidth() / 2.0;
        double headerY = 20;

        switch (pkg.getType().toLowerCase()) {
            case "node", "cloud" -> { headerY = 25;}
            case "folder", "frame" -> {
                headerX = pkg.estimateLabelWidth() / 2.0;
            }
            case "database" -> { headerY = 14;}
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

        packageContainer.addArgument("background", pkg.getBackground());
        packageContainer.addArgument("depth", String.valueOf(pkg.getDepth()));
        packageContainer.addArgument("isTopLevel", String.valueOf(pkg.isTopLevel()));
        packageContainer.addArgument("headerHeight", String.valueOf(pkg.getHeaderHeight()));
        packageContainer.addArgument("labelWidth", String.valueOf((int) pkg.estimateLabelWidth()));

        return packageContainer.build();
    }

    /**
     * Builds an invisible node
     * @param pkg the package containing the anchor coordinates
     * @return the invisible GNode graph element
     */
    public GModelElement buildPackageAnchor(Package pkg) {
        double absoluteX = pkg.getX() + pkg.getAnchorX();
        double absoluteY = pkg.getY() + pkg.getAnchorY();

        return new GNodeBuilder("entity:invis")
                .id(pkg.getAnchorId())
                .position(absoluteX, absoluteY)
                .size(1, 1)
                .build();
    }

    /**
     * Builds and adds the diagram header, title, and footer labels
     *
     * @param elements the element list to append the labels to
     * @param model the class model containing header/title/footer text
     * @param dimensions pre-calculated entity dimensions for bounding box
     * @param entities the list of entities to compute the bounding box from
     */
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

    /**
     * Computes the vertical space needed for a multi-line text string
     *
     * @param lines the text string
     * @return the total height
     */
    private double yOffset(String lines) {
        if (lines == null) {
            return 0;
        }

        int labelHeight = 14;
        int lineCount = lines.split("<br>").length;
        return lineCount > 1 ? lineCount * labelHeight : 6;
    }
}

