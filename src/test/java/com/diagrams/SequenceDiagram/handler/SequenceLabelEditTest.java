/*
 * File: SequenceLabelEditHandlerTest.java
 * Author: Norman Babiak
 * Description: Tests for changing labels in the diagram
 * Date: 29.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.SequenceDiagram.handler;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelIndex;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SequenceLabelEdit Tests")
class SequenceLabelEditTest {
    private SequenceLabelEditHandler handler;
    private SequenceModel model;
    @Mock private SequenceModelState mockState;
    @Mock private GModelIndex mockIndex;
    @Mock private GLabel mockLabel;
    @Mock private ApplyLabelEditOperation mockOp;

    @BeforeEach
    void setup() throws Exception {
        handler = new SequenceLabelEditHandler();
        model = new SequenceModel();
        model.participants = new ArrayList<>();
        model.messages = new ArrayList<>();
        model.groups = new ArrayList<>();
        model.englobers = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.anchors = new ArrayList<>();
        model.messageSpaces = new HashMap<>();
        Field field = findField(handler.getClass(), "modelState");
        field.setAccessible(true);
        field.set(handler, mockState);
        when(mockState.getModel()).thenReturn(model);
        when(mockState.getIndex()).thenReturn(mockIndex);
    }

    private void label(String id, String text) {
        when(mockLabel.getId()).thenReturn(id);
        when(mockLabel.getText()).thenReturn(text);
        when(mockIndex.getByClass(id, GLabel.class)).thenReturn(Optional.of(mockLabel));
        when(mockOp.getLabelId()).thenReturn(id);
    }

    private SequenceNode participant(String id, String name) {
        return new SequenceNode(id, name, "PARTICIPANT", 0, null, false);
    }

    @Test
    @DisplayName("updates participant name, only target is modified")
    void participantName() {
        SequenceNode alice = participant("par-0", "Alice");
        SequenceNode bob = participant("par-1", "Bob");
        model.participants.add(alice);
        model.participants.add(bob);
        label("par-1-label", "Bob");
        when(mockOp.getText()).thenReturn("Robert");

        handler.createCommand(mockOp);

        assertEquals("Alice", alice.getName());
        assertEquals("Robert", bob.getName());
        assertFalse(alice.isModified());
        assertTrue(bob.isModified());
    }

    @Test
    @DisplayName("updates title, header, footer, mainframe")
    void pageDetails() {
        model.title = "T";
        model.header = "H";
        model.footer = "F";
        model.mainframe = "M";
        model.isMainframe = true;

        label("title", "T");
        when(mockOp.getText()).thenReturn("T2");
        handler.createCommand(mockOp);

        label("header", "H");
        when(mockOp.getText()).thenReturn("H2");
        handler.createCommand(mockOp);

        label("footer", "F");
        when(mockOp.getText()).thenReturn("F2");
        handler.createCommand(mockOp);

        label("mainframe", "M");
        when(mockOp.getText()).thenReturn("M2");
        handler.createCommand(mockOp);

        assertEquals("T2", model.title);
        assertEquals("H2", model.header);
        assertEquals("F2", model.footer);
        assertEquals("M2", model.mainframe);
    }

    @Test
    @DisplayName("updates englober label")
    void engloberLabel() {
        SequenceEnglober englober = new SequenceEnglober("eng-0", "Frontend", null, "#LightBlue", 0);
        model.englobers.add(englober);
        label("englober-label-eng-0", "Frontend");
        when(mockOp.getText()).thenReturn("UI");

        handler.createCommand(mockOp);

        assertEquals("UI", englober.getLabel());
        assertTrue(englober.isModified());
    }

    @Test
    @DisplayName("updates message, divider, and delay labels")
    void messageLabels() {
        SequenceNode nodeA = participant("par-0", "A");
        SequenceNode nodeB = participant("par-1", "B");
        SequenceMessage message = new SequenceMessage("msg-0", nodeA, nodeB, "Hello", null, "edge");
        SequenceMessage divider = new SequenceMessage("msg-1", null, null, "Init", null, "edge:divider");
        model.messages.add(message);
        model.messages.add(divider);

        label("label-0", "Hello");
        when(mockOp.getText()).thenReturn("Greetings");
        handler.createCommand(mockOp);

        assertEquals("Greetings", message.getMessage());
        assertTrue(message.isModified());

        label("label-1", "Init");
        when(mockOp.getText()).thenReturn("Setup");
        handler.createCommand(mockOp);

        assertEquals("Setup", divider.getMessage());
    }

    @Test
    @DisplayName("updates group label, comment, and separator label")
    void groupLabels() {
        SequenceGroup group = new SequenceGroup(0, "alt", "cond1", 0);
        group.addSeparator(5);
        group.addSeparatorLabel("else cond");
        model.groups.add(group);

        label("group-comment-0", "cond1");
        when(mockOp.getText()).thenReturn("x > 0");
        handler.createCommand(mockOp);

        assertEquals("x > 0", group.getComment());

        label("group-separator-0-0", "else cond");
        when(mockOp.getText()).thenReturn("default");
        handler.createCommand(mockOp);

        assertEquals("default", group.getSeparatorLabel().getFirst());
    }

    @Test
    @DisplayName("updates note label, selects correct one among multiple")
    void noteLabels() {
        SequenceNote note0 = new SequenceNote("note-0", "First", "LEFT", "#FFFFE0", "NORMAL");
        SequenceNote note1 = new SequenceNote("note-1", "Second", "RIGHT", "#FFFFE0", "NORMAL");
        model.notes.add(note0);
        model.notes.add(note1);
        label("note-1", "Second");
        when(mockOp.getText()).thenReturn("Updated");

        handler.createCommand(mockOp);

        assertEquals("First", note0.getLabel());
        assertEquals("Updated", note1.getLabel());
    }

    @Test
    @DisplayName("updates anchor label")
    void anchorLabel() {
        SequenceNode nodeA = participant("par-0", "A");
        SequenceNode nodeB = participant("par-1", "B");
        SequenceAnchor anchor = new SequenceAnchor(nodeA, nodeB, "anch-0", "Duration");
        model.anchors.add(anchor);
        label("anch-anch-0", "Duration");
        when(mockOp.getText()).thenReturn("Response Time");

        handler.createCommand(mockOp);

        assertEquals("Response Time", anchor.getLabel());
        assertTrue(anchor.isModified());
    }

    @Test
    @DisplayName("throws when label not found, handles unknown label gracefully")
    void edgeCases() {
        when(mockOp.getLabelId()).thenReturn("x");
        when(mockIndex.getByClass("x", GLabel.class)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> handler.createCommand(mockOp));

        label("unknown-label-123", "text");
        when(mockOp.getText()).thenReturn("new");

        assertDoesNotThrow(() -> handler.createCommand(mockOp));
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);

            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException(name);
    }
}
