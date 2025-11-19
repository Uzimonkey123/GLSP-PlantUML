package com.diagrams.ClassDiagram.model.ClassParts;

public class ClassLink {
    private String linkId;
    private ClassEntity entity1;
    private ClassEntity entity2;
    private String type;
    private String message;

    public ClassLink(String linkId, ClassEntity entity1, ClassEntity entity2, String type, String message) {
        this.linkId = linkId;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.type = type;
        this.message = message;
    }

    public String getLinkId() {
        return linkId;
    }

    public ClassEntity getEntity1() {
        return entity1;
    }

    public ClassEntity getEntity2() {
        return entity2;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("{ClassLink: %s, Entity1: %s, Entity2: %s, Type: %s, Message: %s}",
                linkId, entity1, entity2, type, message);
    }
}
