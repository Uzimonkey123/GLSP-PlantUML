/*
 * File: ValidationRule.java
 * Author: Norman Babiak
 * Description: Interface to implement in case of additional implemented rules
 * Date: 5.4.2026
 */

package com.GLSPPlantUML.validators;

import com.GLSPPlantUML.utils.ErrorRecord;

public interface ValidationRule {
    String getDiagramType();
    ErrorRecord validate(String fileText);
}