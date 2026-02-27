package com.diagrams.ClassDiagram.handler;

import com.google.inject.Inject;
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

            for (String id : operation.getElementIds()) {
                findAndRemove(root, id);
                System.out.println("Deleted element: " + id);
            }

            actionDispatcher.dispatch(new UpdateModelAction());
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
            return false; // TODO: store removed elements for undo support
        }

        @Override
        public void undo() {
            // TODO: re-insert removed elements
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