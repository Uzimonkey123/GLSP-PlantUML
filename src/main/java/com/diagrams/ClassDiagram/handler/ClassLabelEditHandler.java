package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.eclipse.glsp.server.operations.GModelOperationHandler;

import java.util.List;
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

            if (label.getId().startsWith("ent-")) {
                for (ClassEntity entity : model.entities) {
                    if (label.getId().startsWith(entity.getId() + "-")) {
                        updateEntityLabel(entity, label.getId(), operation.getText());
                        break;
                    }
                }
            }

            updateLinkLabels(operation);
            updateQualifierLabels(operation);
            updateNoteLabels(operation);
            updatePackageLabels(operation);
            updatePageDetails(operation);
        }

        return Objects.equals(label.getText(), operation.getText())
                ? doNothing()
                : commandOf(() -> label.setText(operation.getText()));
    }

    protected Optional<GLabel> findLabel(final ApplyLabelEditOperation operation) {
        return modelState.getIndex().getByClass(operation.getLabelId(), GLabel.class);
    }

    private void updateEntityLabel(ClassEntity entity, String labelId, String newText) {
        String suffix = labelId.substring(entity.getId().length());

        if (suffix.equals("-label-name")) {
            entity.setName(newText);
            entity.setModified();
            return;
        }

        if (suffix.equals("generic")) {
            entity.setGeneric(newText);
            entity.setModified();
            return;
        }

        if (suffix.startsWith("-field-")) {
            updateListItem(entity.getFields(), suffix, "-field-", newText);
            entity.setModified();
            return;
        }

        if (suffix.startsWith("-method-")) {
            updateListItem(entity.getMethods(), suffix, "-method-", newText);
            entity.setModified();
            return;
        }

        if (suffix.startsWith("-body-")) {
            updateListItem(entity.getRawBody(), suffix, "-body-", newText);
            entity.setModified();
        }
    }

    private void updateListItem(List<?> list, String suffix, String prefix, String newText) {
        try {
            int index = Integer.parseInt(suffix.substring(prefix.length()));
            if (index >= 0 && index < list.size()) {
                ((EntityMethod) list.get(index)).setMethodName(newText);
            }

        } catch (NumberFormatException e) {
            System.err.println("[ApplyLabelEdit] No index: " + suffix);
        }
    }

    private void updateNoteLabels(final ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("note-") || label.getId().startsWith("ent-")) {
            for (ClassEntity note : model.notes) {
                if (label.getId().startsWith(note.getId() + "-")) {
                    note.setName(operation.getText());
                    note.setModified();
                    return;
                }
            }
        }
    }

    private void updateLinkLabels(final ApplyLabelEditOperation operation) {
        if(label.getId().startsWith("link-")) {
            for (ClassLabel classLabel : model.labels) {
                if (classLabel.getLabelId().equals(label.getId())) {
                    classLabel.setLabel(operation.getText());
                    classLabel.setModified();
                    return;
                }
            }
        }
    }

    private void updateQualifierLabels(final ApplyLabelEditOperation operation) {
        String id = label.getId();

        if (id.startsWith("link-qual-src-")) {
            String linkId = id.substring("link-qual-src-".length());
            findLink(linkId).ifPresent(link -> {
                link.setSourceQualifier(operation.getText());
                link.setModified();
            });
            return;
        }

        if (id.startsWith("link-qual-tgt-")) {
            String linkId = id.substring("link-qual-tgt-".length());
            findLink(linkId).ifPresent(link -> {
                link.setTargetQualifier(operation.getText());
                link.setModified();
            });
        }
    }

    private Optional<ClassLink> findLink(String linkId) {
        return model.links.stream()
                .filter(l -> l.getLinkId().equals(linkId))
                .findFirst();
    }

    private void updatePackageLabels(final ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("pkg-")) {
            for (Package pkg : model.packages) {
                if (label.getId().startsWith(pkg.getId() + "-")) {
                    pkg.setName(operation.getText());
                    pkg.setModified();
                    return;
                }
            }
        }
    }

    private void updatePageDetails(final ApplyLabelEditOperation operation) {
        if (label.getId().startsWith("header")) {
            model.header = operation.getText();
            model.headerModified = true;
            return;
        }

        if (label.getId().startsWith("footer")) {
            model.footer = operation.getText();
            model.footerModified = true;
            return;
        }

        if (label.getId().startsWith("title")) {
            model.title = operation.getText();
            model.titleModified = true;
        }
    }
}
