package com.GLSPPlantUML.validators;

import com.google.inject.Inject;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.server.features.directediting.LabelEditValidator;
import org.eclipse.glsp.server.features.directediting.ValidationStatus;
import org.eclipse.glsp.server.model.GModelState;

public class SequenceLabelValidator implements LabelEditValidator {

    @Inject
    protected GModelState modelState;

    @Override
    public ValidationStatus validate(final String label, final GModelElement element) {
        if (label == null || label.trim().isEmpty()) {
            return ValidationStatus.error("Label must not be empty");
        }

        // All good
        return ValidationStatus.ok();
    }
}