package com.GLSPPlantUML.handlers;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
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

            boolean updated = false;
            for (SequenceNode participant : model.participants) {
                String nodeLabelID = participant.getId() + "-label";
                if (label.getId().equals(nodeLabelID)) {
                    updated = true;
                    participant.setName(operation.getText());
                }
            }

            if (!updated) {
                checkPageDetails(operation);
                checkEnglobers(operation);
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

    private void checkPageDetails(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("header")) {
            model.header = operation.getText();
            model.headerModified = true;
        }

        if (label.getId().startsWith("footer")) {
            model.footer = operation.getText();
            model.footerModified = true;
        }

        if (label.getId().startsWith("title")) {
            model.title = operation.getText();
            model.titleModified = true;
        }

        if (label.getId().startsWith("mainframe")) {
            model.mainframe = operation.getText();
            model.mainframeModified = true;
        }
    }

    private void checkEnglobers(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("englober-label")) {
            for (SequenceEnglober englober : model.englobers) {
                String engloberLabelID = "englober-label-" + englober.getId();
                if (label.getId().equals(engloberLabelID)) {
                    englober.setLabel(operation.getText());
                }
            }
        }
    }

    private void checkAnchors(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("anch")) {
            for (SequenceAnchor anchor : model.anchors) {
                String anchorLabelID = "anch-" + anchor.getAnchorId();
                if (label.getId().equals(anchorLabelID)) {
                    anchor.setLabel(operation.getText());
                }
            }
        }
    }

    private void checkGroups(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("group-label")) {
            int expectedGroupLevel = extractIndex(label.getId());
            for (SequenceGroup group : model.groups) {
                if (group.getStartIndex() == expectedGroupLevel && group.isGroup()) {
                    group.setLabel(operation.getText());
                }
            }
        }

        if (label.getId().startsWith("group-comment")) {
            int expectedGroupLevel = extractIndex(label.getId());
            for (SequenceGroup group : model.groups) {
                if (group.getStartIndex() == expectedGroupLevel) {
                    group.setComment(operation.getText());
                }
            }
        }

        if (label.getId().startsWith("group-separator-")) {
            String[] parts = label.getId().split("-");
            if (parts.length >= 4) {
                int groupStart = Integer.parseInt(parts[2]);
                int separatorIndex = Integer.parseInt(parts[3]);

                for (SequenceGroup group : model.groups) {
                    if (group.getStartIndex() == groupStart) {
                        group.setSeparatorLabel(separatorIndex, operation.getText());
                    }
                }
            }
        }
    }

    private void checkMessages(ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("note-")) {
            String expectedNoteId = "note-" + extractIndex(label.getId());
            for (SequenceNote note : model.notes) {
                if (note.getId().equals(expectedNoteId)) {
                    note.setLabel(operation.getText());
                }
            }
        }

        // Match message ID
        if (label.getId().startsWith("label-")) {
            String expectedMessageId = "msg-" + extractIndex(label.getId());
            for (SequenceMessage message : model.messages) {
                if (message.getMsgId().equals(expectedMessageId)) {
                    message.setMessage(operation.getText());
                }
            }
        }
    }
}
