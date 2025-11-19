package com.diagrams.ClassDiagram.model;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;

import java.util.ArrayList;
import java.util.List;

public class ClassModel {
    public List<ClassEntity> entities = new ArrayList<>();
    public List<ClassLink> links = new ArrayList<>();

    public ClassModel() {}

    public ClassEntity getClassEntity(String className) {
        return this.entities.stream()
                .filter(c -> c.getName().equals(className))
                .findFirst()
                .orElse(null);
    }
}
