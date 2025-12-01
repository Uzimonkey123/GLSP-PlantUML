package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.Visibility;

public class EntityMethod {
    private String visibilityChar;
    private final String methodName;

    public EntityMethod(String methodName) {
        String tempName;
        tempName = methodName;
        this.visibilityChar = "";

        if (tempName.charAt(0) != tempName.charAt(1)) {
            this.visibilityChar = Visibility.fromChar(tempName.charAt(0));
        }

        if (methodName.charAt(0) == '\\' || !this.visibilityChar.isEmpty()) {
            tempName = methodName.substring(1).trim();
        }

        this.methodName = tempName;
    }

    public String getVisibilityChar() {
        return visibilityChar;
    }

    public String getMethodName() {
        return methodName;
    }
}
