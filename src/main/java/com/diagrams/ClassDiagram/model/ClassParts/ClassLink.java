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

    private String sourceMember = null;
    private String targetMember = null;

    private String color = "#000000";
    private double thickness = 1.0;

    private String sourceQualifier = null;
    private String targetQualifier = null;
    private boolean noteLink = false;

    public ClassLink(String linkId, ClassEntity entity1, ClassEntity entity2,
                     String type, String message, int length,
                     String decorator1, String decorator2, String quantifier1, String quantifier2) {
        this.linkId = linkId;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.type = type;
        cleanMessage(message);
        this.length = length;
        this.decorator1 = decorator1;
        this.decorator2 = decorator2;
        this.quantifier1 = quantifier1;
        this.quantifier2 = quantifier2;
    }

    private void cleanMessage(String message) {
        if (message.equals("NULL")) {
            this.message = "";
            return;
        }


        if (message.startsWith("[") && message.endsWith("]")) {
            this.message = message.substring(1, message.length() - 1);

        } else {
            this.message = message;
        }
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
        int startIndex = this.type.indexOf("-") + 1;
        int endIndex = this.type.indexOf("(");

        if (startIndex > 0 && endIndex > startIndex) {
            return this.type.substring(startIndex, endIndex);
        }

        return this.type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        cleanMessage(message);
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    public String getSourceMember() {
        return sourceMember;
    }

    public void setSourceMember(String sourceMember) {
        this.sourceMember = sourceMember;
    }

    public String getTargetMember() {
        return targetMember;
    }

    public void setTargetMember(String targetMember) {
        this.targetMember = targetMember;
    }

    public String getSourceQualifier() {
        return sourceQualifier;
    }

    public void setSourceQualifier(String sourceQualifier) {
        this.sourceQualifier = sourceQualifier;
    }

    public String getTargetQualifier() {
        return targetQualifier;
    }

    public void setTargetQualifier(String targetQualifier) {
        this.targetQualifier = targetQualifier;
    }

    public boolean isNoteLink() {
        return noteLink;
    }

    public void setNoteLink(boolean setNoteLink) {
        this.noteLink = setNoteLink;
    }

    @Override
    public String toString() {
        return String.format("{ClassLink: %s, Entity1: %s, Entity2: %s, Type: %s, Message: %s, Dec1: %s, Dec2: %s}",
                linkId, entity1.getId(), entity2.getId(), type, message, decorator1, decorator2);
    }
}
