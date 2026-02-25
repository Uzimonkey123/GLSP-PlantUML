package com.diagrams.ClassDiagram.model;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;

import java.util.ArrayList;
import java.util.List;

public class ClassModel {
    public List<ClassEntity> entities = new ArrayList<>();
    public List<ClassLink> links = new ArrayList<>();
    public List<Package> packages = new ArrayList<>();
    public List<ClassLabel> labels = new ArrayList<>();
    public List<ClassEntity> notes = new ArrayList<>();

    public String footer;
    public String header;
    public String title;

    public int titleLineStart  = -1;
    public int titleLineEnd    = -1;
    public int headerLineStart = -1;
    public int headerLineEnd   = -1;
    public int footerLineStart = -1;
    public int footerLineEnd   = -1;

    public boolean titleModified  = false;
    public boolean headerModified = false;
    public boolean footerModified = false;

    private ClassLineMapper lineMapper;
    private ClassLineFinder lineFinder;

    public ClassModel() {}

    public void setLineFinder(ClassLineFinder lineFinder) {
        this.lineFinder = lineFinder;
    }

    public ClassLineFinder getLineFinder() {
        return lineFinder;
    }

    public void setMapper(ClassLineMapper lineMapper) {
        this.lineMapper = lineMapper;
    }

    public ClassLineMapper getLineMapper() {
        return lineMapper;
    }

    public ClassEntity getClassEntity(String name) {
        return entities.stream()
                .filter(entity -> entity.getName() != null && entity.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public ClassEntity getClassEntityById(String id) {
        return entities.stream()
                .filter(entity -> entity.getId() != null && entity.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassLabel getClassLabelById(String id) {
        return labels.stream()
                .filter(label -> label.getLabelId() != null && label.getLabelId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ClassEntity getClassNoteById(String id) {
        return notes.stream()
                .filter(note -> note.getId() != null && note.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}