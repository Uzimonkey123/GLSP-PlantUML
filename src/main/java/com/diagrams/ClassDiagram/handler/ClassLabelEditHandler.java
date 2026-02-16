package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.eclipse.glsp.server.operations.GModelOperationHandler;

import java.util.Objects;
import java.util.Optional;

public class ClassLabelEditHandler extends GModelOperationHandler<ApplyLabelEditOperation> {
    private GLabel label;
    private ClassModel model;

    @Override
    public Optional<Command> createCommand(final ApplyLabelEditOperation operation) {

        label = findLabel(operation).orElseThrow(
                () -> new IllegalArgumentException("Element with provided ID cannot be found or is not a GLabel"));

        System.err.println("[ApplyLabelEdit] New label text: " + operation.getText());
        System.err.println("[ApplyLabelEdit] Label ID: " + label.getId());

        if (modelState instanceof ClassModelState sequenceState) {
            model = sequenceState.getModel();


            updateLinkLabels(operation);

        }

        return Objects.equals(label.getText(), operation.getText())
                ? doNothing()
                : commandOf(() -> label.setText(operation.getText()));
    }

    protected Optional<GLabel> findLabel(final ApplyLabelEditOperation operation) {
        return modelState.getIndex().getByClass(operation.getLabelId(), GLabel.class);
    }

    private void updateLinkLabels(final ApplyLabelEditOperation operation) {
        if(label.getId().startsWith("link-")) {
            for (ClassLabel classLabel : model.labels) {
                if (classLabel.getLabelId().equals(label.getId())) {
                    classLabel.setLabel(operation.getText());
                }
            }
        }
    }
}
