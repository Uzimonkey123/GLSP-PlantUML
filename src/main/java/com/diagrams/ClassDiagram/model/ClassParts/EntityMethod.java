package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.Visibility;

public class EntityMethod {
    private String visibilityChar;
    private String methodName;
    private String tip;

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
        removeBracketed();
    }

    private void removeBracketed() {
        this.methodName = this.methodName
                .replaceAll("\\{(?!static\\}|classifier\\}|abstract\\})[^}]*\\}", "")
                .trim();
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public String getTip() {
        return this.tip;
    }

    public boolean hasTip() {
        return tip != null && !tip.isEmpty();
    }

    public String getVisibilityChar() {
        return visibilityChar;
    }

    public String getMethodName() {
        return methodName;
    }
}
