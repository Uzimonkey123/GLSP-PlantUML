/*
 * File: ValidationRequest.java
 * Author: Norman Babiak
 * Description: Request sent to the client
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.utils;

public record ValidationRequest(
        String context,
        String diagramType
) {}

