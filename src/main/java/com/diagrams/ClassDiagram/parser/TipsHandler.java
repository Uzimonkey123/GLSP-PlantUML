/*
 * File: TipsHandler.java
 * Author: Norman Babiak
 * Description: Attaches PlantUML tips to matching members
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.creole.Display;

import java.util.List;
import java.util.Map;

public class TipsHandler {

    /**
     * Applies tooltip content from a TIPS entity to matching member of the target entity. Searches methods, fields,
     * and rawBody since PlantUML doesn't distinguish where the tip attaches.
     */
    public void applyTipsToEntity(Entity tipsEntity, ClassEntity targetEntity) {
        Map<String, Display> tips = tipsEntity.getTips();
        if (targetEntity == null) {
            return;
        }

        for (Map.Entry<String, Display> entry : tips.entrySet()) {
            String memberName = entry.getKey();
            String tipContent = String.join("<br>", entry.getValue());

            attachTipToMember(targetEntity.getMethods(), memberName, tipContent, tipsEntity);
            attachTipToMember(targetEntity.getFields(), memberName, tipContent, tipsEntity);
            attachTipToMember(targetEntity.getRawBody(), memberName, tipContent, tipsEntity);
        }
    }

    /**
     * Finds the first member whose signature matches the tip key and sets its tooltip content and background color
     */
    private void attachTipToMember(List<EntityMethod> members, String memberName, String tipContent, Entity tipsEntity) {
        for (EntityMethod member : members) {
            if (matchesMemberSignature(member.getMethodName(), memberName)) {
                member.setTip(tipContent);

                if (tipsEntity.getColors().getColor(ColorType.BACK) != null) {
                    member.setTipBackground(tipsEntity.getColors().getColor(ColorType.BACK).asString());
                }

                return;
            }
        }
    }

    /**
     * Matches a member signature against a tip key. If the tip key has no parentheses but the member does,
     * compares by method name only
     */
    private boolean matchesMemberSignature(String memberSignature, String tipKey) {
        String cleanedMember = cleanSignature(memberSignature);
        String cleanedTip = cleanSignature(tipKey);

        if (!tipKey.contains("(") && cleanedMember.contains("(")) {
            String methodNameOnly = cleanedMember.substring(0, cleanedMember.indexOf("("));
            return methodNameOnly.equals(cleanedTip);
        }

        return cleanedMember.equals(cleanedTip);
    }

    /**
     * Strips visibility modifiers and PlantUML modifiers, then normalizes the signature
     * to "methodName(type1 type2)" format
     */
    private String cleanSignature(String signature) {
        String cleaned = signature.trim()
                .replaceFirst("^[+\\-#~]\\s*", "")
                .replaceAll("\\{[^}]+}\\s*", "");

        if (!cleaned.contains("(")) {
            String[] parts = cleaned.split("\\s+");
            return parts[parts.length - 1];
        }

        int parenStart = cleaned.indexOf("(");
        int parenEnd = cleaned.lastIndexOf(")");

        if (parenStart == -1 || parenEnd == -1) {
            return cleaned;
        }

        // Extract method name
        String beforeParen = cleaned.substring(0, parenStart).trim();
        String params = cleaned.substring(parenStart + 1, parenEnd).trim();

        String[] beforeParts = beforeParen.split("\\s+");
        String methodName = beforeParts[beforeParts.length - 1];

        if (params.isEmpty()) {
            return methodName + "()";
        }

        // Keep only first token of each param (the type, dropping the name)
        StringBuilder normalized = new StringBuilder();
        String[] paramList = params.split(",");
        for (int i = 0; i < paramList.length; i++) {
            String param = paramList[i].trim();
            String[] parts = param.split("\\s+");

            if (parts.length > 0) {
                normalized.append(parts[0]);

                if (i < paramList.length - 1) {
                    normalized.append(" ");
                }
            }
        }

        return methodName + "(" + normalized + ")";
    }
}