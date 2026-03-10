package com.diagrams.ClassDiagram.model.ClassParts;

import com.diagrams.ClassDiagram.model.Visibility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityMethod {
    private String visibilityChar;
    private String methodName;
    private String tip;
    private String tipBackground = "#FFFFCC";
    private final String originalName;
    private boolean isField = false;

    public EntityMethod(String methodName) {
        parse(methodName);
        this.originalName = methodName;
    }

    private void parse(String raw) {
        this.visibilityChar = "";
        String tempName = raw;

        if (isSeparatorLine(raw)) {
            this.methodName = raw;

            return;
        }

        if (!raw.isEmpty()) {
            String vis = Visibility.fromChar(raw.charAt(0));
            if (!vis.isEmpty()) {
                this.visibilityChar = vis;
                tempName = raw.substring(1).trim();
                this.methodName = tempName;
                removeBracketed();

                return;
            }

            if (raw.charAt(0) == '\\') {
                this.methodName = raw.substring(1).trim();
                removeBracketed();

                return;
            }
        }

        Pattern pattern = Pattern.compile("^((?:\\{(?:static|abstract|classifier)}\\s*)+)([+\\-#~])\\s*(.*)$");
        Matcher matcher = pattern.matcher(raw);

        if (matcher.matches()) {
            String modifiers = matcher.group(1);
            String visChar = matcher.group(2);
            String rest = matcher.group(3);

            this.visibilityChar = Visibility.fromChar(visChar.charAt(0));
            tempName = modifiers + rest;
        }

        this.methodName = tempName;
        removeBracketed();
    }

    private boolean isSeparatorLine(String raw) {
        return raw.startsWith("--") ||
                raw.startsWith("..") ||
                raw.startsWith("==") ||
                raw.startsWith("__");
    }

    private void removeBracketed() {
        this.methodName = this.methodName
                .replaceAll("\\{(?!static}|classifier}|abstract})[^}]*}", "")
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

    public void setTipBackground(String tipBackground) {
        this.tipBackground = tipBackground;
    }

    public String getTipBackground() {
        return this.tipBackground;
    }

    public String getVisibilityChar() {
        return visibilityChar;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        parse(methodName);
    }

    public String getOriginalName() {
        return originalName;
    }

    public boolean isField() {
        return isField;
    }

    public void setField(boolean isField) {
        this.isField = isField;
    }
}
