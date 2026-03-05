package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.google.inject.Inject;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.emf.common.command.AbstractCommand;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.features.core.model.UpdateModelAction;
import org.eclipse.glsp.server.operations.DeleteOperation;
import org.eclipse.glsp.server.operations.OperationHandler;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GModelRoot;

import java.util.*;

public class ClassDeleteHandler implements OperationHandler<DeleteOperation> {

    @Inject
    protected ClassModelState modelState;

    @Inject
    protected ActionDispatcher actionDispatcher;

    @Override
    public Optional<Command> createCommand(DeleteOperation operation) {
        return Optional.of(new DeleteCommand(operation, modelState, actionDispatcher));
    }

    @Override
    public Class<DeleteOperation> getHandledOperationType() {
        return DeleteOperation.class;
    }

    private static class DeleteCommand extends AbstractCommand {
        private final DeleteOperation operation;
        private final ClassModelState modelState;
        private final ActionDispatcher actionDispatcher;

        public DeleteCommand(DeleteOperation operation, ClassModelState modelState, ActionDispatcher actionDispatcher) {
            this.operation = operation;
            this.modelState = modelState;
            this.actionDispatcher = actionDispatcher;
        }

        @Override
        public boolean canExecute() {
            return operation != null && modelState != null
                    && operation.getElementIds() != null
                    && !operation.getElementIds().isEmpty();
        }

        @Override
        public void execute() {
            GModelRoot root = modelState.getRoot();
            ClassModel classModel = modelState.getModel();

            for (String id : operation.getElementIds()) {
                ClassEntity entity = classModel.getClassEntityById(id);
                if (entity != null) {
                    deleteEntity(classModel, entity);
                    findAndRemove(root, id);

                    continue;
                }

                ClassLink link = classModel.getLinkById(id);
                if (link != null) {
                    deleteLink(classModel, link);
                    findAndRemove(root, id);

                    continue;
                }

                ClassEntity note = classModel.getClassNoteById(id);
                if (note != null) {
                    deleteNote(classModel, note, id);
                    findAndRemove(root, id);

                    continue;
                }

                findAndRemove(root, id);
            }

            actionDispatcher.dispatch(new UpdateModelAction());
        }

        private void deleteEntity(ClassModel model, ClassEntity entity) {
            if (entity.hasLine()) {
                model.markLinesForDeletion(entity.getSourceLineStart(), entity.getSourceLineEnd());
            }

            Iterator<ClassLink> linkIter = model.links.iterator();
            while (linkIter.hasNext()) {
                ClassLink link = linkIter.next();

                if (link.getEntity1() == entity || link.getEntity2() == entity) {
                    if (link.hasLine()) {
                        model.markLinesForDeletion(link.getSourceLineStart(), link.getSourceLineEnd());
                    }

                    linkIter.remove();
                }
            }

            model.entities.remove(entity);
            deleteNotesReferencingEntity(model, entity);
        }

        private void deleteNotesReferencingEntity(ClassModel model, ClassEntity entity) {
            ClassLineMapper lineMapper = model.getLineMapper();
            if (lineMapper == null) return;

            String entityName = entity.getName();
            String entityAlias = entity.getAlias();

            Iterator<ClassEntity> noteIter = model.notes.iterator();
            while (noteIter.hasNext()) {
                ClassEntity note = noteIter.next();

                if (note.hasLine()) {
                    ClassLineMapper.LineInfo info = lineMapper.getLineInfo(note.getSourceLineStart());
                    if (info != null && info.type == ClassLineMapper.LineType.NOTE) {
                        String noteLine = info.originalText;

                        boolean referencesEntity = noteLine.contains(" of " + entityName) ||
                                                    noteLine.contains(" of \"" + entityName + "\"");

                        if (entityAlias != null && !entityAlias.isEmpty()) {
                            referencesEntity = referencesEntity ||
                                                noteLine.contains(" of " + entityAlias) ||
                                                noteLine.contains(" of \"" + entityAlias + "\"");
                        }

                        if (referencesEntity) {
                            model.markLinesForDeletion(note.getSourceLineStart(), note.getSourceLineEnd());
                            noteIter.remove();
                        }
                    }
                }
            }
        }

        private void deleteLink(ClassModel model, ClassLink link) {
            if (link.hasLine()) {
                model.markLinesForDeletion(link.getSourceLineStart(), link.getSourceLineEnd());
            }

            if (link.hasNoteOnLink()) {
                ClassEntity note = link.getNoteOnLink();

                if (note.hasLine()) {
                    model.markLinesForDeletion(note.getSourceLineStart(), note.getSourceLineEnd());
                }

                model.notes.remove(note);
                model.entities.remove(note);
            }

            model.links.remove(link);
        }

        private void deleteNote(ClassModel model, ClassEntity note, String id) {
            if (note.hasLine()) {
                model.markLinesForDeletion(note.getSourceLineStart(), note.getSourceLineEnd());
            }

            if (note.getId().endsWith("-tip")) {
                clearTipFromMember(model, note.getId());
            }

            if (note.getId().startsWith("note-link")) {
                clearNotesFromLink(model, note);
            }

            model.notes.remove(note);
            model.entities.remove(note);
        }

        private void clearNotesFromLink(ClassModel model, ClassEntity note) {
            for (ClassLink link : model.links) {
                if (link.hasNoteOnLink() && link.getNoteOnLink().getId().equals(note.getId())) {

                    if (link.getNoteOnLink().hasLine()) {
                        model.markLinesForDeletion(note.getSourceLineStart(), note.getSourceLineEnd());
                    }

                    model.notes.remove(note);
                    model.entities.remove(note);
                }
            }
        }

        private void clearTipFromMember(ClassModel model, String tipId) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.+)-(field|method)-(\\d+)-tip$")
                    .matcher(tipId);

            if (!m.matches()) return;

            String parentId = m.group(1);
            String memberType = m.group(2);
            int memberIndex = Integer.parseInt(m.group(3));

            ClassEntity parent = model.getClassEntityById(parentId);
            if (parent == null) return;

            List<EntityMethod> members = memberType.equals("field")
                    ? parent.getFields()
                    : parent.getMethods();

            if (memberIndex >= 0 && memberIndex < members.size()) {
                members.get(memberIndex).setTip(null);
            }

            for (EntityMethod bodyItem : parent.getRawBody()) {
                if (memberIndex >= 0 && memberIndex < members.size()) {
                    String memberName = members.get(memberIndex).getMethodName();
                    if (bodyItem.getMethodName().equals(memberName)) {
                        bodyItem.setTip(null);
                    }
                }
            }
        }

        private boolean findAndRemove(GModelElement parent, String id) {
            List<GModelElement> children = new ArrayList<>(parent.getChildren());
            for (GModelElement child : children) {
                if (id.equals(child.getId())) {
                    parent.getChildren().remove(child);
                    return true;
                }

                if (findAndRemove(child, id)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean canUndo() {
            return false;
        }

        @Override
        public void undo() {
            // TODO: store removed elements for undo support
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public Collection<?> getResult() {
            return Collections.emptyList();
        }

        @Override
        public Collection<?> getAffectedObjects() {
            return Collections.emptyList();
        }

        @Override
        public String getLabel() {
            return "Delete Elements";
        }

        @Override
        public String getDescription() {
            return "Delete selected elements from diagram";
        }
    }
}