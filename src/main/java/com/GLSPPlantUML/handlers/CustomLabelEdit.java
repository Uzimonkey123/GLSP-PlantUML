package com.GLSPPlantUML.handlers;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.state.SequenceModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.eclipse.glsp.server.operations.GModelOperationHandler;

import java.util.Objects;
import java.util.Optional;

public class CustomLabelEdit extends GModelOperationHandler<ApplyLabelEditOperation> {

    @Override
    public Optional<Command> createCommand(final ApplyLabelEditOperation operation) {

        GLabel label = findLabel(operation).orElseThrow(
                () -> new IllegalArgumentException("Element with provided ID cannot be found or is not a GLabel"));

        System.err.println("[ApplyLabelEdit] New label text: " + operation.getText());
        System.err.println("[ApplyLabelEdit] Label ID: " + label.getId());

        if (modelState instanceof SequenceModelState sequenceState) {
            SequenceModel model = sequenceState.getModel();

            boolean updated = model.participants.stream()
                    .filter(p -> p.getName().equals(label.getText()))
                    .findFirst()
                    .map(p -> {
                        p.setName(operation.getText());
                        return true;
                    })
                    .orElse(false);

            // Match message ID
            if (!updated && label.getId().startsWith("label-")) {
                String expectedMessageId = "msg-" + extractIndex(label.getId());
                model.messages.stream()
                        .filter(m -> m.getMsgId().equals(expectedMessageId))
                        .findFirst()
                        .ifPresent(m -> m.setMessage(operation.getText()));
            }
        }

        return Objects.equals(label.getText(), operation.getText())
                ? doNothing()
                : commandOf(() -> label.setText(operation.getText()));
    }

    protected Optional<GLabel> findLabel(final ApplyLabelEditOperation operation) {
        return modelState.getIndex().getByClass(operation.getLabelId(), GLabel.class);
    }

    private int extractIndex(String labelId) {
        try {
            return Integer.parseInt(labelId.substring(labelId.lastIndexOf('-') + 1));
        } catch (Exception e) {
            return -1;
        }
    }
}
