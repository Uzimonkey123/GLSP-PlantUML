package com.diagrams.SequenceDiagram.validator;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
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

        if (modelState instanceof SequenceModelState sequenceModelState) {
            SequenceModel model = sequenceModelState.getModel();

            // Check for the same node name if exists, if yes, return error to client and do not allow change
            boolean hasName = model.participants.stream()
                    .anyMatch(p -> p.getName().equals(label));

            if (hasName) {
                return ValidationStatus.error("Node name already exists");
            }
        }

        // All good
        return ValidationStatus.ok();
    }
}