/*
 * File: ErrorRecord.java
 * Author: Norman Babiak
 * Description: Record with the information for the error, having line, characters and the message itself
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.utils;

public record ErrorRecord(
    boolean hasError,
    String errorMsg,
    int lineNumber,
    int columnStart,
    int columnEnd
) {}