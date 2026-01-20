package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.NodePosition;

import java.util.ArrayList;
import java.util.List;

public class ClassEntity extends NodePosition {
    private final String id;
    private String name = "";
    private String originalName;
    private final String type;
    private String visibility = "";
    private final List<EntityMethod> methods = new ArrayList<>();
    private final List<EntityMethod> fields = new ArrayList<>();
    private final List<EntityMethod> rawBody = new ArrayList<>();

    public ClassEntity(int x, int y, String id, String name, String type,
                        List<EntityMethod> methods, List<EntityMethod> fields, List<EntityMethod> rawBody) {
        super(x, y);
        this.id = id;
        this.name = name;
        this.type = type;
        this.methods.addAll(methods);
        this.fields.addAll(fields);
        this.rawBody.addAll(rawBody);
    }

    public ClassEntity(int x, int y, String id, String name, String type) {
        super(x, y);
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public ClassEntity(int x, int y, String id, String type) {
        super(x, y);
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!this.name.equals(name)) {
            //setModified();
        }

        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<EntityMethod> getMethods() {
        return methods;
    }

    public List<EntityMethod> getFields() {
        return fields;
    }

    public List<EntityMethod> getRawBody() {
        return rawBody;
    }

    @Override
    public String toString() {
        return String.format("ClassEntity{id='%s', name='%s', type=%s, visibility='%s', pos=(%f,%f), fields=%d, methods=%d}",
                id, name, type, visibility, getX(), getY(), fields.size(), methods.size());
    }
}
