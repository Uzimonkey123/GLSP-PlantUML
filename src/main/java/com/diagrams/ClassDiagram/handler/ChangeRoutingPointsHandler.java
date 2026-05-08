/*
 * File: ChangeRoutingPointsHandler.java
 * Author: Norman Babiak
 * Description: Placeholder handler for changing routing points after update from client side
 * Date: 30.3.2026
 */

package com.diagrams.ClassDiagram.handler;

import com.google.inject.Inject;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.AbstractCommand;
import org.eclipse.glsp.server.operations.OperationHandler;
import org.eclipse.glsp.server.operations.ChangeRoutingPointsOperation;
import com.diagrams.ClassDiagram.state.ClassModelState;

import java.util.*;

/**
 * Placeholder class, since routing points are not changed in the implementation of the tool
 */
public class ChangeRoutingPointsHandler implements OperationHandler<ChangeRoutingPointsOperation> {

    @Inject
    protected ClassModelState modelState;

    @Override
    public Optional<Command> createCommand(ChangeRoutingPointsOperation operation) {
        return Optional.of(new ChangeRoutingPointsCommand(operation, modelState));
    }

    @Override
    public Class<ChangeRoutingPointsOperation> getHandledOperationType() {
        return ChangeRoutingPointsOperation.class;
    }

    private static class ChangeRoutingPointsCommand extends AbstractCommand {
        private final ChangeRoutingPointsOperation operation;
        private final ClassModelState modelState;

        public ChangeRoutingPointsCommand(ChangeRoutingPointsOperation operation, ClassModelState modelState) {
            this.operation = operation;
            this.modelState = modelState;
        }

        @Override
        public boolean canExecute() {
            return operation != null && modelState != null;
        }

        @Override
        public void execute() {
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
            return "Change Routing Points";
        }

        @Override
        public String getDescription() {
            return "Change routing points of edges";
        }
    }
}