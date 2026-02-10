package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.NodePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Package extends NodePosition {
    private final String id;
    private String name;
    private final String type;
    private final List<ClassEntity> entities = new ArrayList<>();
    private final List<Package> childPackages = new ArrayList<>();
    private Package parentPackage = null;
    private String background = null;
    private double width = 0;
    private double height = 0;
    private final int padding = 20;

    public Package(String id, String name, String type) {
        super(0, 0);
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public List<ClassEntity> getEntities() {
        return entities;
    }

    public void addEntity(ClassEntity entity) {
        entities.add(entity);
    }

    public List<Package> getChildPackages() {
        return childPackages;
    }

    public void addChildPackage(Package pkg) {
        childPackages.add(pkg);
        pkg.setParentPackage(this);
    }

    public Package getParentPackage() {
        return parentPackage;
    }

    public void setParentPackage(Package parentPackage) {
        this.parentPackage = parentPackage;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public int getPadding() {
        return padding;
    }

    public int getHeaderHeight() {
        int baseHeaderHeight = 30;
        int extraLineHeight = 16;

        return baseHeaderHeight + (getNameLines() - 1) * extraLineHeight;
    }

    public int getNameLines() {
        return name.split("\\\\n|\n").length;
    }

    public double estimateLabelWidth() {
        String[] lines = name.split("\\\\n|\n");
        int maxLen = 0;
        for (String line : lines) {
            maxLen = Math.max(maxLen, line.length());
        }
        return maxLen * 8.0 + (padding * 2);
    }

    public List<ClassEntity> getAllEntities() {
        List<ClassEntity> all = new ArrayList<>(entities);

        for (Package child : childPackages) {
            all.addAll(child.getAllEntities());
        }

        return all;
    }

    public void calculateDimensions(Map<String, Double> entityWidths, Map<String, Double> entityHeights) {
        for (Package child : childPackages) {
            child.calculateDimensions(entityWidths, entityHeights);
        }

        int headerHeight = getHeaderHeight();

        if (entities.isEmpty() && childPackages.isEmpty()) {
            width = Math.max(150, estimateLabelWidth());
            height = 100 + headerHeight;

            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (ClassEntity entity : entities) {
            double entityWidth = entityWidths.getOrDefault(entity.getId(), 200.0);
            double entityHeight = entityHeights.getOrDefault(entity.getId(), 150.0);

            minX = Math.min(minX, entity.getX());
            minY = Math.min(minY, entity.getY());
            maxX = Math.max(maxX, entity.getX() + entityWidth);
            maxY = Math.max(maxY, entity.getY() + entityHeight);
        }

        for (Package child : childPackages) {
            minX = Math.min(minX, child.getX());
            minY = Math.min(minY, child.getY());
            maxX = Math.max(maxX, child.getX() + child.getWidth());
            maxY = Math.max(maxY, child.getY() + child.getHeight());
        }

        // Set position to top-left corner with padding
        if (minX != Double.MAX_VALUE) {
            setX(minX - padding);
            setY(minY - padding - headerHeight);
        }

        double contentWidth = (maxX - minX) + (2 * padding);
        width  = Math.max(contentWidth, estimateLabelWidth());
        height = (maxY - minY) + (2 * padding) + headerHeight;
    }

    public void shiftX(double dx) {
        setX(getX() + dx);

        for (ClassEntity entity : entities) {
            entity.setX((int) (entity.getX() + dx));
        }

        for (Package child : childPackages) {
            child.shiftX(dx);
        }
    }

    public boolean isTopLevel() {
        return parentPackage == null;
    }

    public int getDepth() {
        int depth = 0;
        Package current = this.parentPackage;

        while (current != null) {
            depth++;
            current = current.getParentPackage();
        }

        return depth;
    }

    @Override
    public String toString() {
        return String.format("Package{id='%s', name='%s', entities=%d, childPackages=%d, pos=(%f,%f), size=(%f,%f)}",
                id, name, entities.size(), childPackages.size(), getX(), getY(), width, height);
    }
}