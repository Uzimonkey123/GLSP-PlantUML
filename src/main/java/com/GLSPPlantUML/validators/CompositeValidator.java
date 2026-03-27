package com.GLSPPlantUML.validators;

import com.GLSPPlantUML.launcher.RuleLoader;
import com.GLSPPlantUML.utils.ErrorRecord;

import java.util.List;

public class CompositeValidator {
    private final ErrorValidator errorValidator;
    private final RuleLoader ruleLoader;

    public CompositeValidator(ErrorValidator errorValidator, RuleLoader ruleLoader) {
        this.errorValidator = errorValidator;
        this.ruleLoader = ruleLoader;
    }

    public ErrorRecord validate(String fileText, String diagramType) {
        ErrorRecord syntaxResult = errorValidator.checkErrors(fileText);
        if (syntaxResult.hasError()) {
            return syntaxResult;
        }

        List<ValidationRule> rules = ruleLoader.getRulesForDiagram(diagramType);
        for (ValidationRule rule : rules) {
            ErrorRecord result = rule.validate(fileText);
            if (result != null && result.hasError()) {
                return result;
            }
        }

        return new ErrorRecord(false, null, -1, -1, -1);
    }

    public ErrorRecord validate(String fileText) {
        return errorValidator.checkErrors(fileText);
    }
}