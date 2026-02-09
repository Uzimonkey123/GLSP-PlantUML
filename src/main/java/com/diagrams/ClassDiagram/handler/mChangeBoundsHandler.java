package com.diagrams.ClassDiagram.handler;

import com.google.inject.Inject;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.AbstractCommand;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.features.core.model.UpdateModelAction;
import org.eclipse.glsp.server.operations.OperationHandler;
import org.eclipse.glsp.server.operations.ChangeBoundsOperation;
import org.eclipse.glsp.server.types.ElementAndBounds;
import org.eclipse.glsp.graph.GPoint;
import com.diagrams.ClassDiagram.model.NodePosition;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.state.ClassModelState;

import java.util.*;

public class mChangeBoundsHandler implements OperationHandler<ChangeBoundsOperation> {

    @Inject
    protected ClassModelState modelState;

    @Inject
    protected ActionDispatcher actionDispatcher;


    @Override
    public Optional<Command> createCommand(ChangeBoundsOperation operation) {
        return Optional.of(new ChangeBoundsCommand(operation, modelState, actionDispatcher));
    }

    @Override
    public Class<ChangeBoundsOperation> getHandledOperationType() {
        return ChangeBoundsOperation.class;
    }

    private static class ChangeBoundsCommand extends AbstractCommand {
        private final ChangeBoundsOperation operation;
        private final ClassModelState modelState;
        private final ActionDispatcher actionDispatcher;
        private final Map<String, NodePosition> oldPositions = new HashMap<>();

        public ChangeBoundsCommand(ChangeBoundsOperation operation, ClassModelState modelState, ActionDispatcher actionDispatcher) {
            this.operation = operation;
            this.modelState = modelState;
            this.actionDispatcher = actionDispatcher;
        }

        @Override
        public boolean canExecute() {
            return operation != null && modelState != null;
        }

        @Override
        public void execute() {
            for (ElementAndBounds elementAndBounds : operation.getNewBounds()) {
                String elementId = elementAndBounds.getElementId();
                GPoint newPosition = elementAndBounds.getNewPosition();

                if (newPosition != null) {
                    ClassEntity entity = modelState.getModel().getClassEntityById(elementId);

                    if (entity != null) {
                        double oldX = entity.getX();
                        double oldY = entity.getY();
                        NodePosition oldPosition = new NodePosition(
                                oldX,
                                oldY
                        );
                        oldPositions.put(elementId, oldPosition);


                        // Set new position
                        NodePosition position = new NodePosition(
                                newPosition.getX(),
                                newPosition.getY()
                        );
                        entity.setX(newPosition.getX());
                        entity.setY(newPosition.getY());

                        System.out.println("Updated position for " + entity.getName() +
                                " to (" + position.getX() + ", " + position.getY() + ")");
                    }
                }
            }

            actionDispatcher.dispatch(new UpdateModelAction());
        }

        @Override
        public boolean canUndo() {
            return !oldPositions.isEmpty();
        }

        @Override
        public void undo() {
            for (Map.Entry<String, NodePosition> entry : oldPositions.entrySet()) {
                ClassEntity entity = modelState.getModel().getClassEntityById(entry.getKey());

                if (entity != null && entry.getValue() != null) {
                    entity.setX(entry.getValue().getX());
                    entity.setY(entry.getValue().getY());
                }
            }

            actionDispatcher.dispatch(new UpdateModelAction());
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
            List<ClassEntity> affected = new ArrayList<>();
            for (String elementId : oldPositions.keySet()) {
                ClassEntity entity = modelState.getModel().getClassEntityById(elementId);

                if (entity != null) {
                    affected.add(entity);
                }
            }

            return affected;
        }

        @Override
        public String getLabel() {
            return "Change Bounds";
        }

        @Override
        public String getDescription() {
            return "Change position/size of elements";
        }
    }
}