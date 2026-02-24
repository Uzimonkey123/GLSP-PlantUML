package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.reconstructor.SourceElement;

public class ClassLink extends SourceElement {
    private final String linkId;
    private final ClassEntity entity1;
    private final ClassEntity entity2;
    private final String type;
    private ClassLabel message;
    private int length;
    private final String decorator1;
    private final String decorator2;
    private ClassLabel quantifier1;
    private ClassLabel quantifier2;

    private String sourceMember = null;
    private String targetMember = null;

    private String color = "#000000";
    private double thickness = 1.0;

    private ClassLabel sourceQualifier = null;
    private ClassLabel targetQualifier = null;

    private boolean noteLink = false;

    private ClassEntity noteOnLink = null;
    private String notePosition = null;

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
        this.quantifier1 = new ClassLabel(0, 0, "link-quant1-" + linkId, quantifier1);
        this.quantifier2 = new ClassLabel(0, 0, "link-quant2-" + linkId, quantifier2);
    }

    private void cleanMessage(String message) {
        if (this.message == null) {
            this.message = new ClassLabel(0, 0, "link-label-" + this.linkId, "");
        }

        if (message.equals("NULL")) {
            this.message.setLabel("");
            return;
        }


        if (message.startsWith("[") && message.endsWith("]")) {
            this.message.setLabel( message.substring(1, message.length() - 1));

        } else {
            this.message.setLabel(message);
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

    public ClassLabel getMessage() {
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

    public ClassLabel getQuantifier1() {
        return quantifier1;
    }

    public ClassLabel getQuantifier2() {
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

    public ClassLabel getSourceQualifierLabel() {
        return sourceQualifier;
    }

    public ClassLabel getTargetQualifierLabel() {
        return targetQualifier;
    }

    public String getSourceQualifier() {
        return sourceQualifier != null ? sourceQualifier.getLabel() : null;
    }

    public String getTargetQualifier() {
        return targetQualifier != null ? targetQualifier.getLabel() : null;
    }

    public void setSourceQualifier(String text) {
        if (text == null || text.isEmpty()) {
            this.sourceQualifier = null;

            return;
        }

        if (this.sourceQualifier == null) {
            this.sourceQualifier = new ClassLabel(0, 0, "link-qual-src-" + linkId, text);

        } else {
            this.sourceQualifier.setLabel(text);
            setModified();
        }
    }

    public void setTargetQualifier(String text) {
        if (text == null || text.isEmpty()) {
            this.targetQualifier = null;

            return;
        }

        if (this.targetQualifier == null) {
            this.targetQualifier = new ClassLabel(0, 0, "link-qual-tgt-" + linkId, text);

        } else {
            this.targetQualifier.setLabel(text);
            setModified();
        }
    }

    public boolean isNoteLink() {
        return noteLink;
    }

    public void setNoteLink(boolean setNoteLink) {
        this.noteLink = setNoteLink;
    }

    public ClassEntity getNoteOnLink() {
        return noteOnLink;
    }

    public void setNoteOnLink(ClassEntity noteOnLink) {
        this.noteOnLink = noteOnLink;
    }

    public boolean hasNoteOnLink() {
        return noteOnLink != null;
    }

    public String getNotePosition() {
        return notePosition;
    }

    public void setNotePosition(String notePosition) {
        this.notePosition = notePosition;
    }

    @Override
    public String toString() {
        return String.format("{ClassLink: %s, Entity1: %s, Entity2: %s, Type: %s, Message: %s, Dec1: %s, Dec2: %s}",
                linkId, entity1.getId(), entity2.getId(), type, message, decorator1, decorator2);
    }
}