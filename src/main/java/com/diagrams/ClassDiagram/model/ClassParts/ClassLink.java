package com.diagrams.ClassDiagram.model.ClassParts;

public class ClassLink {
    private final String linkId;
    private final ClassEntity entity1;
    private final ClassEntity entity2;
    private final String type;
    private String message;
    private int length;
    private final String decorator1;
    private final String decorator2;
    private final String quantifier1;
    private final String quantifier2;

    public ClassLink(String linkId, ClassEntity entity1, ClassEntity entity2,
                     String type, String message, int length,
                     String decorator1, String decorator2, String quantifier1, String quantifier2) {
        this.linkId = linkId;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.type = type;
        this.message = message;
        this.length = length;
        this.decorator1 = decorator1;
        this.decorator2 = decorator2;
        this.quantifier1 = quantifier1;
        this.quantifier2 = quantifier2;
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getDecorator1() {
        return decorator1;
    }

    public String getDecorator2() {
        return decorator2;
    }

    public String getQuantifier1() {
        if (quantifier1 == null) return "";

        return quantifier1;
    }

    public String getQuantifier2() {
        if (quantifier2 == null) return "";

        return quantifier2;
    }

    @Override
    public String toString() {
        return String.format("{ClassLink: %s, Entity1: %s, Entity2: %s, Type: %s, Message: %s, Dec1: %s, Dec2: %s}",
                linkId, entity1.getId(), entity2.getId(), type, message, decorator1, decorator2);
    }
}
