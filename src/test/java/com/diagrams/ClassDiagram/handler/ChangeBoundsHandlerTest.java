/*
 * File: ChangeBoundsHandlerTest.java
 * Author: Norman Babiak
 * Description: Tests for changing bounds of diagram
 * Date: 28.4.2026
 */

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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
        model.entities = new ArrayList<>(); model.labels = new ArrayList<>();
        model.notes = new ArrayList<>();

        Field field1 = ChangeBoundsHandler.class.getDeclaredField("modelState");
        field1.setAccessible(true);
        field1.set(handler, mockModelState);

        Field field2 = ChangeBoundsHandler.class.getDeclaredField("actionDispatcher");
        field2.setAccessible(true);
        field2.set(handler, mockActionDispatcher);

        when(mockModelState.getModel()).thenReturn(model);
    }

    private ChangeBoundsOperation op(String id, double x, double y) {
        GPoint point = GraphFactory.eINSTANCE.createGPoint();
        point.setX(x);
        point.setY(y);

        ElementAndBounds bound = new ElementAndBounds();
        bound.setElementId(id);
        bound.setNewPosition(point);

        ChangeBoundsOperation op = new ChangeBoundsOperation();
        op.setNewBounds(List.of(bound));
        return op;
    }

    private ClassEntity entity(String id, double x, double y) {
        ClassEntity entity = new ClassEntity((int)x, (int)y, id, "E", "CLASS", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }

    @Test
    @DisplayName("moves entity, label, and note to new position")
    void movesElements() {
        ClassEntity ent = entity("ent-0", 0, 0);
        model.entities.add(ent);

        ClassLabel lbl = new ClassLabel(0, 0, "lbl-0", "x");
        model.labels.add(lbl);

        ClassEntity note = entity("note-0", 0, 0);
        model.notes.add(note);

        op("ent-0", 100, 200).getNewBounds().forEach(b -> {});
        Command command1 = handler.createCommand(op("ent-0", 100, 200)).orElseThrow();
        command1.execute();

        Command command2 = handler.createCommand(op("lbl-0", 50, 60)).orElseThrow();
        command2.execute();

        Command command3 = handler.createCommand(op("note-0", 10, 20)).orElseThrow();
        command3.execute();

        assertEquals(100, ent.getX()); assertEquals(200, ent.getY());
        assertEquals(50, lbl.getX()); assertEquals(60, lbl.getY()); assertTrue(lbl.isModified());
        assertEquals(10, note.getX()); assertEquals(20, note.getY());
    }

    @Test
    @DisplayName("undo restores original position")
    void undo() {
        ClassEntity entity = entity("ent-0", 50, 75);
        model.entities.add(entity);

        Command command = handler.createCommand(op("ent-0", 200, 300)).orElseThrow();
        command.execute();
        assertEquals(200, entity.getX());

        command.undo(); assertEquals(50, entity.getX());
        assertEquals(75, entity.getY());
    }

    @Test
    @DisplayName("handles unknown element without error")
    void unknownElement() {
        Command command = handler.createCommand(op("unknown", 10, 10)).orElseThrow();
        assertDoesNotThrow(command::execute);
    }

    @Test
    @DisplayName("getAffectedObjects contains moved entity")
    void affectedObjects() {
        ClassEntity entity = entity("ent-0", 0, 0);
        model.entities.add(entity);

        Command command = handler.createCommand(op("ent-0", 1, 1)).orElseThrow();
        command.execute();

        assertTrue(command.getAffectedObjects().contains(entity));
    }
}
