/*
 * File: ClassDeleteHandler.java
 * Author: Norman Babiak
 * Description: Handler for deleting elements from the diagram
 * Date: 4.5.2026
 */

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
import java.util.regex.Matcher;

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

        /**
         * Main entrypoint that delegates and finds the given element that was deleted between notes, entities and links
         */
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
                    deleteNote(classModel, note);
                    findAndRemove(root, id);

                    continue;
                }

                findAndRemove(root, id);
            }

            actionDispatcher.dispatch(new UpdateModelAction());
        }

        /**
         * Deletes an entity and cascades to all connected links.
         * If a connected link leads to an association point the entire association structure is removed.
         */
        private void deleteEntity(ClassModel model, ClassEntity entity) {
            if (entity.hasLine()) {
                model.markLinesForDeletion(entity.getSourceLineStart(), entity.getSourceLineEnd());
            }

            Set<ClassEntity> associationPointsToDelete = new HashSet<>();

            Iterator<ClassLink> linkIter = model.links.iterator();
            while (linkIter.hasNext()) {
                ClassLink link = linkIter.next();

                if (link.getEntity1() == entity || link.getEntity2() == entity) {
                    if (link.hasLine()) {
                        model.markLinesForDeletion(link.getSourceLineStart(), link.getSourceLineEnd());
                    }

                    // Check if the other end is association point, if yes, whole association class structure removed
                    ClassEntity other = (link.getEntity1() == entity) ? link.getEntity2() : link.getEntity1();
                    if ("ASSOCIATION_POINT".equals(other.getType())) {
                        if (!link.getType().contains("DASHED")) {
                            associationPointsToDelete.add(other);

                        } else {
                            markAssociationClassLineForDeletion(model, entity);
                        }
                    }

                    linkIter.remove();
                }
            }

            for (ClassEntity assocPoint : associationPointsToDelete) {
                deleteAssociationPoint(model, assocPoint);
            }

            model.entities.remove(entity);
            deleteNotesReferencingEntity(model, entity);
        }

        /**
         * Marks the association class line that needs to be deleted from source code (entity1, entity2) .. AssocClass
         */
        private void markAssociationClassLineForDeletion(ClassModel model, ClassEntity assocClass) {
            ClassLineMapper lineMapper = model.getLineMapper();
            if (lineMapper == null) return;

            String assocClassName = assocClass.getName();
            String assocClassAlias = assocClass.getAlias();

            for (ClassLineMapper.LineInfo info : lineMapper.getLineInfos()) {
                if (info.type != ClassLineMapper.LineType.RELATIONSHIP) continue;

                String line = info.originalText;

                if (line.contains("..") && line.contains("(") && line.contains(")")) {
                    boolean referencesAssocClass = line.contains(".. " + assocClassName)
                            || line.contains(".." + assocClassName)
                            || line.contains(".. \"" + assocClassName + "\"");

                    if (assocClassAlias != null && !assocClassAlias.isEmpty()) {
                        referencesAssocClass = referencesAssocClass
                                || line.contains(".. " + assocClassAlias)
                                || line.contains(".." + assocClassAlias);
                    }

                    if (referencesAssocClass) {
                        model.markLinesForDeletion(info.lineNumber, info.lineNumber);

                        return;
                    }
                }
            }
        }

        /**
         * Removes an association point and all links connected to it.
         */
        private void deleteAssociationPoint(ClassModel model, ClassEntity assocPoint) {
            if (assocPoint.hasLine()) {
                model.markLinesForDeletion(assocPoint.getSourceLineStart(), assocPoint.getSourceLineEnd());
            }

            Iterator<ClassLink> linkIter = model.links.iterator();
            while (linkIter.hasNext()) {
                ClassLink link = linkIter.next();

                if (link.getEntity1() == assocPoint || link.getEntity2() == assocPoint) {
                    if (link.hasLine()) {
                        model.markLinesForDeletion(link.getSourceLineStart(), link.getSourceLineEnd());
                    }

                    linkIter.remove();
                }
            }

            model.entities.remove(assocPoint);
        }

        /**
         * Removes notes that reference the deleted entity via "note of EntityName" syntax.
         */
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

        /**
         * Deletes link from the model, and as well note if one is connected on it
         */
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

        /**
         * Deletes a note and cleans up references: tip notes clear the member's tooltip, note-on-link notes detach
         * from the link, adn any connecting links are marked for deletion.
         */
        private void deleteNote(ClassModel model, ClassEntity note) {
             if (note.hasLine()) {
                model.markLinesForDeletion(note.getSourceLineStart(), note.getSourceLineEnd());
            }

            if (note.getId().endsWith("-tip")) {
                clearTipFromMember(model, note.getId());
            }

            if (note.getId().startsWith("note-link")) {
                clearNotesFromLink(model, note);
            }

            for (ClassLink link : model.links) {
                if (link.getEntity1() == note || link.getEntity2() == note) {
                    if (link.hasLine()) {
                        model.markLinesForDeletion(link.getSourceLineStart(), link.getSourceLineEnd());
                    }
                }
            }

            model.notes.remove(note);
            model.entities.remove(note);
        }

        /**
         * If just note is deleted from the link, find the link and remove the fact that it was once there
         */
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

        /**
         * Parses the tip ID to find the parent entity and member index, then clears the tooltip from that member.
         */
        private void clearTipFromMember(ClassModel model, String tipId) {
            Matcher m = java.util.regex.Pattern
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

        /**
         * Finds the element by its ID and removes it
         */
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