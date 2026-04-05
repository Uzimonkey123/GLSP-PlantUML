/*
 * File: ErrorMessage.java
 * Author: Norman Babiak
 * Description: Class for saving the error message and build the final GModel to send the client in case of error
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.utils;

import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

public class ErrorMessage {
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public GModelElement buildError() {
        return new GLabelBuilder("label:html")
                .position(0, 0)
                .text(message)
                .build();
    }
}
