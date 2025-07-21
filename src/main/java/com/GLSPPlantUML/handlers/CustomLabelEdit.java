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
    private GLabel label;
    private SequenceModel model;

    @Override
    public Optional<Command> createCommand(final ApplyLabelEditOperation operation) {

        label = findLabel(operation).orElseThrow(
                () -> new IllegalArgumentException("Element with provided ID cannot be found or is not a GLabel"));

        System.err.println("[ApplyLabelEdit] New label text: " + operation.getText());
        System.err.println("[ApplyLabelEdit] Label ID: " + label.getId());

        if (modelState instanceof SequenceModelState sequenceState) {
            model = sequenceState.getModel();

            boolean updated = model.participants.stream()
                    .filter(p -> p.getName().equals(label.getText()))
                    .findFirst()
                    .map(p -> {
                        p.setName(operation.getText());
                        return true;
                    })
                    .orElse(false);

            if (!updated) {
                checkAnchors(operation);
                checkGroups(operation);
                checkMessages(operation);
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

    private void checkAnchors(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("anch")) {
            model.anchors.stream()
                    .filter(a -> {
                        String anchorLabelID = "anch-" + a.getAnchorId();
                        return anchorLabelID.equals(label.getId());
                    })
                    .findFirst()
                    .ifPresent(a -> a.setLabel(operation.getText()));
        }
    }

    private void checkGroups(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("group-label")) {
            int expectedGroupLevel = extractIndex(label.getId());
            model.groups.stream()
                    .filter(g -> g.getStartIndex() == expectedGroupLevel)
                    .findFirst()
                    .ifPresent(g -> g.setLabel(operation.getText()));
        }

        if (label.getId().startsWith("group-comment")) {
            int expectedGroupLevel = extractIndex(label.getId());
            model.groups.stream()
                    .filter(g -> g.getStartIndex() == expectedGroupLevel)
                    .findFirst()
                    .ifPresent(g -> g.setComment(operation.getText()));
        }
    }

    private void checkMessages(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("note-")) {
            String expectedNoteId = "note-" + extractIndex(label.getId());
            model.notes.stream()
                    .filter(n -> n.getId().equals(expectedNoteId))
                    .findFirst()
                    .ifPresent(n -> n.setLabel(operation.getText()));
        }

        // Match message ID
        if (label.getId().startsWith("label-")) {
            String expectedMessageId = "msg-" + extractIndex(label.getId());
            model.messages.stream()
                    .filter(m -> m.getMsgId().equals(expectedMessageId))
                    .findFirst()
                    .ifPresent(m -> m.setMessage(operation.getText()));
        }
    }
}
