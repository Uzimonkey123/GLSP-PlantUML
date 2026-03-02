package com.diagrams.ClassDiagram.factory.ClassParts;

import com.GLSPPlantUML.utils.WidthCalculator;
import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.factory.ClassLayout;
import org.eclipse.glsp.graph.GCompartment;
import org.eclipse.glsp.graph.GModelElement;

import java.util.*;

import static com.diagrams.ClassDiagram.utils.MapperInfo.addMapperInfo;

public class ClassEntityFactory {

    private final ClassModel model;
    private final EntityBuild entityBuild;

    private final List<GModelElement> elements;
    private final ClassLayout layoutEngine;

    private final int horizontalPadding = 20;

    public List<TipInfo> tipInfoList = new ArrayList<>();
    private final Map<String, ClassLayout.Size> dimensions = new HashMap<>();

    public static class TipInfo {
        public String tipId;
        public String parentEntityId;
        public String memberName;
        public double tipX;
        public double tipY;
        public double tipWidth;
        public double tipHeight;

        public TipInfo(String tipId, String parentEntityId, String memberName,
                       double tipX, double tipY, double tipWidth, double tipHeight) {
            this.tipId = tipId;
            this.parentEntityId = parentEntityId;
            this.memberName = memberName;
            this.tipX = tipX;
            this.tipY = tipY;
            this.tipWidth = tipWidth;
            this.tipHeight = tipHeight;
        }
    }

    public ClassEntityFactory(ClassModel model, EntityBuild entityBuild, List<GModelElement> elements) {
        this.model = model;
        this.entityBuild = entityBuild;
        this.elements = elements;
        this.layoutEngine = new ClassLayout();
    }

    public Map<String, ClassLayout.Size> getDimensions() {
        return dimensions;
    }

    public void createEntities() {
        dimensions.clear();

        for (ClassEntity entity : model.entities) {
            double width, height;

            switch (entity.getType()) {
                case "CIRCLE" -> {
                    width = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
                    height = entityLength(entity);
                }

                case "DIAMOND" -> {
                    width = 30;
                    height = 30;
                }

                case "LOLLIPOP" -> {
                    width = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
                    height = 26;
                }

                case "ASSOCIATION_POINT" -> {
                    width = 8;
                    height = 8;
                }

                case "NOTE" -> {
                    width = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding) + 20;
                    height = calculateNoteHeight(entity.getName());
                }

                default -> {
                    double methodWidth = entityAttributesLength(entity.getMethods());
                    double fieldsWidth = entityAttributesLength(entity.getFields());
                    double attributesWidth = Math.max(methodWidth, fieldsWidth);

                    double textWidth = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
                    if (!entity.getStereotypeName().isEmpty()) {
                        double stereotypeWidth = WidthCalculator.calculateWidth(entity.getStereotypeName(), horizontalPadding);
                        textWidth = Math.max(textWidth, stereotypeWidth);
                    }

                    width = Math.max(textWidth, attributesWidth);
                    if (entity.isGeneric()) {
                        width += WidthCalculator.calculateWidth(entity.getGeneric(), horizontalPadding);
                    }

                    height = entityLength(entity);
                }
            }

            dimensions.put(entity.getId(), new ClassLayout.Size(width, height));
        }

        // Check if layout is needed
        boolean needsLayout = model.entities.stream()
                .anyMatch(e -> e.getX() == 0 && e.getY() == 0);

        if (needsLayout) {
            layoutEngine.layoutEntities(model, dimensions);
        }

        // Calculate package dimensions if packages exist
        if (!model.packages.isEmpty()) {
            calculatePackageDimensions();
            calculatePackageAnchorPositions();
            separateOverlappingPackages();
            createPackages();
        }

        for (ClassEntity entity : model.entities) {
            switch (entity.getType()) {
                case "CIRCLE" -> {
                    createCircleEntity(entity);
                    continue;
                }
                case "DIAMOND" -> {
                    createDiamondEntity(entity);
                    continue;
                }
                case "ASSOCIATION_POINT" -> {
                    createAssociationPoint(entity);
                    continue;
                }
                case "LOLLIPOP" -> {
                    createLollipop(entity);
                    continue;
                }
                case "NOTE" -> {
                    createNoteEntity(entity);
                    continue;
                }
            }

            ClassLayout.Size size = dimensions.get(entity.getId());
            double entityWidth = size.width;
            double entityHeight = size.height;

            List<String> methodNames = new ArrayList<>();
            for (EntityMethod method : entity.getMethods()) {
                methodNames.add(method.getMethodName());
            }

            List<String> fieldNames = new ArrayList<>();
            for (EntityMethod fields : entity.getFields()) {
                fieldNames.add(fields.getMethodName());
            }

            List<String> bodyLines = new ArrayList<>();
            for (EntityMethod item : entity.getRawBody()) {
                bodyLines.add(item.getMethodName());
            }

            elements.add(entityBuild.buildEntity(entity, entityWidth, entityHeight, methodNames, fieldNames, bodyLines));

            createTipNotes(entity, entityWidth, entityHeight);
        }

        entityBuild.buildPageDetails(elements, model, dimensions, model.entities);
    }

    private void calculatePackageDimensions() {
        // Create maps for entity dimensions
        Map<String, Double> entityWidths = new HashMap<>();
        Map<String, Double> entityHeights = new HashMap<>();

        for (Map.Entry<String, ClassLayout.Size> entry : dimensions.entrySet()) {
            entityWidths.put(entry.getKey(), entry.getValue().width);
            entityHeights.put(entry.getKey(), entry.getValue().height);
        }

        // Calculate dimensions for all packages
        for (Package pkg : model.packages) {
            if (pkg.isTopLevel()) {
                pkg.calculateDimensions(entityWidths, entityHeights);
            }
        }
    }

    private void createTipNotes(ClassEntity entity, double entityWidth, double entityHeight) {
        double tipX = entity.getX() + entityWidth + 20;
        double currentTipY = entity.getY();

        int tipSpacing = 15;

        for (int i = 0; i < entity.getFields().size(); i++) {
            EntityMethod field = entity.getFields().get(i);
            if (field.hasTip()) {
                String tipId = entity.getId() + "-field-" + i + "-tip";
                String memberName = field.getMethodName();

                double finalCurrentTipY = currentTipY;
                ClassEntity tipEntity = model.notes.stream()
                        .filter(n -> n.getId().equals(tipId))
                        .findFirst()
                        .orElseGet(() -> {
                            ClassEntity newTip = new ClassEntity(
                                    (int) tipX,
                                    (int) finalCurrentTipY,
                                    tipId,
                                    field.getTip(),
                                    "NOTE"
                            );
                            newTip.setBackground(field.getTipBackground());
                            model.notes.add(newTip);

                            int startLine = model.getLineFinder().findNoteLine(newTip.getName(), newTip);
                            int endLine   = model.getLineFinder().findNoteEndLine(startLine, newTip);
                            addMapperInfo(newTip, startLine, endLine, model.getLineMapper());

                            return newTip;
                        });

                double tipWidth = calculateTipWidth(tipEntity.getName());
                double tipHeight = calculateNoteHeight(tipEntity.getName());

                elements.add(entityBuild.buildNoteEntity(tipEntity, tipWidth, tipHeight));

                // Store tip info for edge creation
                tipInfoList.add(new TipInfo(tipId, entity.getId(), memberName,
                        tipEntity.getX(), tipEntity.getY(), tipWidth, tipHeight));

                // Move currentTipY down for next tip
                currentTipY += tipHeight + tipSpacing;
            }
        }

        for (int i = 0; i < entity.getMethods().size(); i++) {
            EntityMethod method = entity.getMethods().get(i);
            if (method.hasTip()) {
                String tipId = entity.getId() + "-method-" + i + "-tip";
                String memberName = method.getMethodName();


                double finalCurrentTipY = currentTipY;
                ClassEntity tipEntity = model.notes.stream()
                        .filter(n -> n.getId().equals(tipId))
                        .findFirst()
                        .orElseGet(() -> {
                            ClassEntity newTip = new ClassEntity(
                                    (int) tipX,
                                    (int) finalCurrentTipY,
                                    tipId,
                                    method.getTip(),
                                    "NOTE"
                            );
                            newTip.setBackground(method.getTipBackground());
                            model.notes.add(newTip);

                            int startLine = model.getLineFinder().findNoteLine(newTip.getName(), newTip);
                            int endLine   = model.getLineFinder().findNoteEndLine(startLine, newTip);
                            addMapperInfo(newTip, startLine, endLine, model.getLineMapper());

                            return newTip;
                        });

                double tipWidth = calculateTipWidth(tipEntity.getName());
                double tipHeight = calculateNoteHeight(tipEntity.getName());

                elements.add(entityBuild.buildNoteEntity(tipEntity, tipWidth, tipHeight));

                // Store tip info for edge creation
                tipInfoList.add(new TipInfo(tipId, entity.getId(), memberName,
                        tipEntity.getX(), tipEntity.getY(), tipWidth, tipHeight));

                // Move currentTipY down for next tip
                currentTipY += tipHeight + tipSpacing;
            }
        }
    }

    private void createCircleEntity(ClassEntity entity) {
        double entityWidth = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding);
        double entityHeight = entityLength(entity);

        elements.add(entityBuild.buildCircleEntity(entity, entityWidth, entityHeight));
    }

    private void createDiamondEntity(ClassEntity entity) {
        double entityWidth = 30;

        elements.add(entityBuild.buildDiamondEntity(entity, entityWidth));
    }

    private void createLollipop(ClassEntity entity) {
        double entityWidth = 15;
        double entityHeight = 15;

        elements.add(entityBuild.buildLollipopEntity(entity, entityWidth, entityHeight));
    }

    private void createAssociationPoint(ClassEntity entity) {
        elements.add(entityBuild.buildAssociationPoint(entity));
    }

    private void createNoteEntity(ClassEntity entity) {
        double entityWidth = WidthCalculator.calculateWidth(entity.getName(), horizontalPadding) + 20;
        double entityHeight = calculateNoteHeight(entity.getName());

        model.notes.add(entity);
        elements.add(entityBuild.buildNoteEntity(entity, entityWidth, entityHeight));
    }

    private double entityAttributesLength(List<EntityMethod> attribute) {
        double length = 0;

        for (EntityMethod method : attribute) {
            length = Math.max(WidthCalculator.calculateWidth(method.getMethodName(), horizontalPadding / 2), length);
        }

        return length;
    }

    private double entityLength(ClassEntity entity) {
        int lineHeight = 14;
        int verticalPadding = 5;
        int emptyHeight = 10;
        int headerHeight = 30;

        int fieldsHeight = !entity.getFields().isEmpty()
                ? entity.getFields().size() * lineHeight + verticalPadding * 2
                : emptyHeight;

        int methodsHeight = !entity.getMethods().isEmpty()
                ? entity.getMethods().size() * lineHeight + verticalPadding * 2
                : emptyHeight;

        int stereotypeHeight = !entity.getStereotypeName().isEmpty() ? lineHeight : 0;

        return stereotypeHeight + headerHeight + fieldsHeight + methodsHeight;
    }

    private double calculateNoteHeight(String text) {
        int lineHeight = 14;
        int padding = 15;
        int lines = text.split("<br>").length;
        return lines * lineHeight + padding * 2;
    }

    private double calculateTipWidth(String tipText) {
        String[] lines = tipText.split("<br>");
        int maxLineLength = 0;

        for (String line : lines) {
            maxLineLength = Math.max(maxLineLength, line.length());
        }

        int charWidth = 7;
        int padding = 10;

        return Math.max(maxLineLength * charWidth + padding * 2, 100);
    }

    private void calculatePackageAnchorPositions() {
        Map<String, Package> anchorToPackage = new HashMap<>();
        for (Package pkg : model.packages) {
            anchorToPackage.put(pkg.getAnchorId(), pkg);
        }

        // For each link between two package anchors, tell each side where the other is
        for (ClassLink link : model.links) {
            String id1 = link.getEntity1().getId();
            String id2 = link.getEntity2().getId();

            Package pkg1 = anchorToPackage.get(id1);
            Package pkg2 = anchorToPackage.get(id2);

            if (pkg1 == null || pkg2 == null) continue;

            double cx1 = pkg1.getX() + pkg1.getWidth() / 2;
            double cy1 = pkg1.getY() + pkg1.getHeight() / 2;
            double cx2 = pkg2.getX() + pkg2.getWidth() / 2;
            double cy2 = pkg2.getY() + pkg2.getHeight() / 2;

            pkg1.addAnchorTarget(cx2, cy2);
            pkg2.addAnchorTarget(cx1, cy1);
        }

        for (Package pkg : model.packages) {
            pkg.finalizeAnchor();
        }
    }

    private void separateOverlappingPackages() {
        // Sort top-level packages left-to-right, then push any that overlap to the right
        List<Package> topLevel = model.packages.stream()
                .filter(Package::isTopLevel)
                .sorted(Comparator.comparingDouble(Package::getX))
                .toList();

        int gap = 40;

        for (int i = 1; i < topLevel.size(); i++) {
            Package prev = topLevel.get(i - 1);
            Package curr = topLevel.get(i);

            double prevRight = prev.getX() + prev.getWidth();

            if (curr.getX() < prevRight + gap) {
                double shift = prevRight + gap - curr.getX();
                curr.shiftX(shift);
            }
        }
    }

    private void createPackages() {
        List<Package> sortedPackages = new ArrayList<>(model.packages);
        sortedPackages.sort(Comparator.comparingInt(Package::getDepth).reversed());

        for (Package pkg : sortedPackages) {
            GCompartment packageElement = entityBuild.buildPackage(
                    pkg,
                    pkg.getWidth(),
                    pkg.getHeight()
            );

            elements.add(packageElement);
        }
    }
}