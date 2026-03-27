package com.GLSPPlantUML.validators;

import com.GLSPPlantUML.utils.ErrorRecord;

public interface ValidationRule {
    String getDiagramType();
    ErrorRecord validate(String fileText);
}