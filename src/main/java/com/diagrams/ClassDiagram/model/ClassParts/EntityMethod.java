package com.diagrams.ClassDiagram.model.ClassParts;

public class EntityMethod {
    private String visibilityChar;
    private String methodName;

    public EntityMethod(String methodName) {
        this.methodName = methodName;
    }

    public String getVisibilityChar() {
        return visibilityChar;
    }

    public String getMethodName() {
        return methodName;
    }
}
