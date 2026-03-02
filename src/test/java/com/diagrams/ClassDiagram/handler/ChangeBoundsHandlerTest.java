package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLabel;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GPoint;
import org.eclipse.glsp.graph.GraphFactory;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.operations.ChangeBoundsOperation;
import org.eclipse.glsp.server.types.ElementAndBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChangeBoundsHandler Tests")
class ChangeBoundsHandlerTest {
    private ChangeBoundsHandler handler;
    private ClassModel model;

    @Mock private ClassModelState mockModelState;
    @Mock private ActionDispatcher mockActionDispatcher;

    @BeforeEach
    void setUp() throws Exception {
        handler = new ChangeBoundsHandler();

        model = new ClassModel();
        model.entities = new ArrayList<>();
        model.labels = new ArrayList<>();
        model.notes = new ArrayList<>();

        injectField("modelState", mockModelState);
        injectField("actionDispatcher", mockActionDispatcher);

        when(mockModelState.getModel()).thenReturn(model);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = ChangeBoundsHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, value);
    }

    private ChangeBoundsOperation createOperation(String elementId, double x, double y) {
        GPoint point = GraphFactory.eINSTANCE.createGPoint();
        point.setX(x);
        point.setY(y);

        ElementAndBounds bounds = new ElementAndBounds();
        bounds.setElementId(elementId);
        bounds.setNewPosition(point);

        ChangeBoundsOperation operation = new ChangeBoundsOperation();
        operation.setNewBounds(List.of(bounds));

        return operation;
    }

    @Test
    @DisplayName("getHandledOperationType returns ChangeBoundsOperation")
    void handledOperationType() {
        assertEquals(ChangeBoundsOperation.class, handler.getHandledOperationType());
    }

    @Test
    @DisplayName("createCommand returns non-empty command")
    void createCommandReturnsCommand() {
        ChangeBoundsOperation operation = createOperation("ent-0", 100, 200);

        Optional<Command> command = handler.createCommand(operation);

        assertTrue(command.isPresent());
        assertTrue(command.get().canExecute());
    }

    @Test
    @DisplayName("execute updates entity position")
    void executeUpdatesEntityPosition() {
        ClassEntity entity = createEntity("ent-0", "User", 0, 0);
        model.entities.add(entity);

        ChangeBoundsOperation operation = createOperation("ent-0", 150, 250);
        Command command = handler.createCommand(operation).orElseThrow();

        command.execute();

        assertEquals(150, entity.getX());
        assertEquals(250, entity.getY());
    }

    @Test
    @DisplayName("execute updates label position")
    void executeUpdatesLabelPosition() {
        ClassLabel label = new ClassLabel(0, 0, "link-0-msg", "uses");
        model.labels.add(label);

        ChangeBoundsOperation operation = createOperation("link-0-msg", 80, 120);
        Command command = handler.createCommand(operation).orElseThrow();

        command.execute();

        assertEquals(80, label.getX());
        assertEquals(120, label.getY());
        assertTrue(label.isModified());
    }

    @Test
    @DisplayName("execute updates note position")
    void executeUpdatesNotePosition() {
        ClassEntity note = createEntity("note-0", "Note text", 0, 0);
        model.notes.add(note);

        ChangeBoundsOperation operation = createOperation("note-0", 200, 300);
        Command command = handler.createCommand(operation).orElseThrow();

        command.execute();

        assertEquals(200, note.getX());
        assertEquals(300, note.getY());
    }

    @Test
    @DisplayName("execute handles unknown element gracefully")
    void executeHandlesUnknownElement() {
        ChangeBoundsOperation operation = createOperation("unknown-id", 100, 100);
        Command command = handler.createCommand(operation).orElseThrow();

        assertDoesNotThrow(command::execute);
    }

    @Test
    @DisplayName("undo restores original position")
    void undoRestoresPosition() {
        ClassEntity entity = createEntity("ent-0", "User", 50, 75);
        model.entities.add(entity);

        ChangeBoundsOperation operation = createOperation("ent-0", 200, 300);
        Command command = handler.createCommand(operation).orElseThrow();

        command.execute();
        assertEquals(200, entity.getX());
        assertEquals(300, entity.getY());

        command.undo();
        assertEquals(50, entity.getX());
        assertEquals(75, entity.getY());
    }

    @Test
    @DisplayName("canUndo returns true after execute")
    void canUndoAfterExecute() {
        ClassEntity entity = createEntity("ent-0", "User", 0, 0);
        model.entities.add(entity);

        ChangeBoundsOperation operation = createOperation("ent-0", 100, 100);
        Command command = handler.createCommand(operation).orElseThrow();

        command.execute();

        assertTrue(command.canUndo());
    }

    @Test
    @DisplayName("getAffectedObjects returns moved entities")
    void getAffectedObjectsReturnsEntities() {
        ClassEntity entity = createEntity("ent-0", "User", 0, 0);
        model.entities.add(entity);

        ChangeBoundsOperation operation = createOperation("ent-0", 100, 100);
        Command command = handler.createCommand(operation).orElseThrow();
        command.execute();

        assertTrue(command.getAffectedObjects().contains(entity));
    }

    @Test
    @DisplayName("getLabel returns descriptive label")
    void getLabelReturnsDescription() {
        ChangeBoundsOperation operation = createOperation("ent-0", 0, 0);
        Command command = handler.createCommand(operation).orElseThrow();

        assertEquals("Change Bounds", command.getLabel());
    }

    private ClassEntity createEntity(String id, String name, double x, double y) {
        ClassEntity entity = new ClassEntity((int) x, (int) y, id, name, "CLASS",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }
}