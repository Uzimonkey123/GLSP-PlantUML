package com.diagrams.SequenceDiagram.handler;

import com.GLSPPlantUML.handlers.CustomLabelEdit;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelIndex;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SequenceLabelEdit Tests")
class SequenceLabelEditTest {
    private CustomLabelEdit handler;
    private SequenceModel model;

    @Mock private SequenceModelState mockModelState;
    @Mock private GModelIndex mockIndex;
    @Mock private GLabel mockLabel;
    @Mock private ApplyLabelEditOperation mockOperation;

    @BeforeEach
    void setup() throws Exception {
        handler = new CustomLabelEdit();

        model = new SequenceModel();
        model.participants = new ArrayList<>();
        model.messages = new ArrayList<>();
        model.groups = new ArrayList<>();
        model.englobers = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.anchors = new ArrayList<>();
        model.messageSpaces = new HashMap<>();

        Field modelStateField = findFieldInHierarchy(handler.getClass(), "modelState");
        modelStateField.setAccessible(true);
        modelStateField.set(handler, mockModelState);

        when(mockModelState.getModel()).thenReturn(model);
        when(mockModelState.getIndex()).thenReturn(mockIndex);
    }

    private void setupLabel(String labelId, String currentText) {
        when(mockLabel.getId()).thenReturn(labelId);
        when(mockLabel.getText()).thenReturn(currentText);
        when(mockIndex.getByClass(labelId, GLabel.class)).thenReturn(Optional.of(mockLabel));
        when(mockOperation.getLabelId()).thenReturn(labelId);
    }

    @Nested
    @DisplayName("Participant Labels")
    class ParticipantLabelTests {

        @Test
        @DisplayName("updates participant name")
        void updatesParticipantName() {
            SequenceNode participant = createParticipant("par-0", "Alice", "PARTICIPANT");
            model.participants.add(participant);

            setupLabel("par-0-label", "Alice");
            when(mockOperation.getText()).thenReturn("Bob");

            handler.createCommand(mockOperation);

            assertEquals("Bob", participant.getName());
            assertTrue(participant.isModified());
        }

        @Test
        @DisplayName("updates correct participant when multiple exist")
        void updatesCorrectParticipant() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            SequenceNode charlie = createParticipant("par-2", "Charlie", "PARTICIPANT");
            model.participants.add(alice);
            model.participants.add(bob);
            model.participants.add(charlie);

            setupLabel("par-1-label", "Bob");
            when(mockOperation.getText()).thenReturn("Robert");

            handler.createCommand(mockOperation);

            assertEquals("Alice", alice.getName());
            assertEquals("Robert", bob.getName());
            assertEquals("Charlie", charlie.getName());
            assertFalse(alice.isModified());
            assertTrue(bob.isModified());
            assertFalse(charlie.isModified());
        }
    }

    @Nested
    @DisplayName("Page Details Labels")
    class PageDetailsTests {

        @Test
        @DisplayName("updates title, header, footer, and mainframe")
        void updatesPageDetails() {
            // Title
            model.title = "Old Title";
            setupLabel("title", "Old Title");
            when(mockOperation.getText()).thenReturn("New Title");
            handler.createCommand(mockOperation);
            assertEquals("New Title", model.title);
            assertTrue(model.titleModified);

            model.header = "Old Header";
            setupLabel("header", "Old Header");
            when(mockOperation.getText()).thenReturn("New Header");
            handler.createCommand(mockOperation);
            assertEquals("New Header", model.header);
            assertTrue(model.headerModified);

            model.footer = "Old Footer";
            setupLabel("footer", "Old Footer");
            when(mockOperation.getText()).thenReturn("New Footer");
            handler.createCommand(mockOperation);
            assertEquals("New Footer", model.footer);
            assertTrue(model.footerModified);

            model.mainframe = "Old Frame";
            model.isMainframe = true;
            setupLabel("mainframe", "Old Frame");
            when(mockOperation.getText()).thenReturn("New Frame");
            handler.createCommand(mockOperation);
            assertEquals("New Frame", model.mainframe);
            assertTrue(model.mainframeModified);

            model.title = "Title";
            setupLabel("title-label", "Title");
            when(mockOperation.getText()).thenReturn("Updated Title");
            handler.createCommand(mockOperation);
            assertEquals("Updated Title", model.title);
        }
    }

    @Nested
    @DisplayName("Englober Labels")
    class EngloberLabelTests {

        @Test
        @DisplayName("updates englober label and selects correct one when multiple exist")
        void updatesEngloberLabels() {
            SequenceEnglober frontend = createEnglober("englober-0", "Frontend", "#LightBlue");
            SequenceEnglober backend = createEnglober("englober-1", "Backend", "#LightGreen");
            model.englobers.add(frontend);
            model.englobers.add(backend);

            setupLabel("englober-label-englober-0", "Frontend");
            when(mockOperation.getText()).thenReturn("UI Layer");
            handler.createCommand(mockOperation);
            assertEquals("UI Layer", frontend.getLabel());
            assertTrue(frontend.isModified());

            setupLabel("englober-label-englober-1", "Backend");
            when(mockOperation.getText()).thenReturn("Services");
            handler.createCommand(mockOperation);
            assertEquals("UI Layer", frontend.getLabel());
            assertEquals("Services", backend.getLabel());
        }
    }

    @Nested
    @DisplayName("Anchor Labels")
    class AnchorLabelTests {

        @Test
        @DisplayName("updates anchor label and selects correct one when multiple exist")
        void updatesAnchorLabels() {
            SequenceNode from = createParticipant("par-0", "A", "PARTICIPANT");
            SequenceNode to = createParticipant("par-1", "B", "PARTICIPANT");
            SequenceAnchor anchor1 = new SequenceAnchor(from, to, "anchor-0", "First");
            SequenceAnchor anchor2 = new SequenceAnchor(from, to, "anchor-1", "Second");
            model.anchors.add(anchor1);
            model.anchors.add(anchor2);

            // Update first anchor
            setupLabel("anch-anchor-0", "First");
            when(mockOperation.getText()).thenReturn("Duration");
            handler.createCommand(mockOperation);
            assertEquals("Duration", anchor1.getLabel());
            assertTrue(anchor1.isModified());

            setupLabel("anch-anchor-1", "Second");
            when(mockOperation.getText()).thenReturn("Response Time");
            handler.createCommand(mockOperation);
            assertEquals("Duration", anchor1.getLabel());
            assertEquals("Response Time", anchor2.getLabel());
        }
    }

    @Nested
    @DisplayName("Group Labels")
    class GroupLabelTests {

        @Test
        @DisplayName("updates group label for named group")
        void updatesGroupLabel() {
            SequenceGroup group = createGroup(0, "My Group", "Description", true);
            model.groups.add(group);

            setupLabel("group-label-0", "My Group");
            when(mockOperation.getText()).thenReturn("Updated Group");

            handler.createCommand(mockOperation);

            assertEquals("Updated Group", group.getLabel());
            assertTrue(group.isModified());
        }

        @Test
        @DisplayName("updates group comment")
        void updatesGroupComment() {
            SequenceGroup group = createGroup(0, "alt", "condition1", false);
            model.groups.add(group);

            setupLabel("group-comment-0", "condition1");
            when(mockOperation.getText()).thenReturn("x > 0");

            handler.createCommand(mockOperation);

            assertEquals("x > 0", group.getComment());
            assertTrue(group.isModified());
        }

        @Test
        @DisplayName("updates group separator label")
        void updatesGroupSeparatorLabel() {
            SequenceGroup group = createGroup(0, "alt", "condition1", false);
            group.addSeparator(5);
            group.addSeparatorLabel("else condition");
            model.groups.add(group);

            setupLabel("group-separator-0-0", "else condition");
            when(mockOperation.getText()).thenReturn("default");

            handler.createCommand(mockOperation);

            assertEquals("default", group.getSeparatorLabel().get(0));
            assertTrue(group.isModified());
        }
    }

    @Nested
    @DisplayName("Message Labels")
    class MessageLabelTests {

        @Test
        @DisplayName("updates message, divider, and delay labels")
        void updatesMessageLabels() {
            SequenceNode from = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode to = createParticipant("par-1", "Bob", "PARTICIPANT");

            SequenceMessage message = createMessage("msg-0", from, to, "Hello");
            model.messages.add(message);
            setupLabel("label-0", "Hello");
            when(mockOperation.getText()).thenReturn("Greetings");
            handler.createCommand(mockOperation);
            assertEquals("Greetings", message.getMessage());
            assertTrue(message.isModified());

            SequenceMessage divider = createDividerMessage("msg-1", "Initialization");
            model.messages.add(divider);
            setupLabel("label-1", "Initialization");
            when(mockOperation.getText()).thenReturn("Setup Phase");
            handler.createCommand(mockOperation);
            assertEquals("Setup Phase", divider.getMessage());

            SequenceMessage delay = createDelayMessage("msg-2", "5 minutes later");
            model.messages.add(delay);
            setupLabel("label-2", "5 minutes later");
            when(mockOperation.getText()).thenReturn("10 minutes later");
            handler.createCommand(mockOperation);
            assertEquals("10 minutes later", delay.getMessage());
        }
    }

    @Nested
    @DisplayName("Note Labels")
    class NoteLabelTests {

        @Test
        @DisplayName("updates note label")
        void updatesNoteLabel() {
            SequenceNote note = createNote("note-0", "Old text", "RIGHT");
            model.notes.add(note);

            setupLabel("note-0", "Old text");
            when(mockOperation.getText()).thenReturn("New text");

            handler.createCommand(mockOperation);

            assertEquals("New text", note.getLabel());
            assertTrue(note.isModified());
        }

        @Test
        @DisplayName("updates correct note when multiple exist")
        void updatesCorrectNote() {
            SequenceNote note0 = createNote("note-0", "First note", "LEFT");
            SequenceNote note1 = createNote("note-1", "Second note", "RIGHT");
            SequenceNote note2 = createNote("note-2", "Third note", "OVER");
            model.notes.add(note0);
            model.notes.add(note1);
            model.notes.add(note2);

            setupLabel("note-1", "Second note");
            when(mockOperation.getText()).thenReturn("Updated note");

            handler.createCommand(mockOperation);

            assertEquals("First note", note0.getLabel());
            assertEquals("Updated note", note1.getLabel());
            assertEquals("Third note", note2.getLabel());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("throws when label not found")
        void throwsWhenLabelNotFound() {
            when(mockOperation.getLabelId()).thenReturn("nonexistent");
            when(mockIndex.getByClass("nonexistent", GLabel.class)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> handler.createCommand(mockOperation));
        }

        @Test
        @DisplayName("handles label ID with no matching element gracefully")
        void handlesNoMatchingElement() {
            setupLabel("unknown-label-123", "text");
            when(mockOperation.getText()).thenReturn("new text");

            assertDoesNotThrow(() -> handler.createCommand(mockOperation));
        }

        @Test
        @DisplayName("handles empty participant list")
        void handlesEmptyParticipantList() {
            setupLabel("par-0-label", "Alice");
            when(mockOperation.getText()).thenReturn("Bob");

            assertDoesNotThrow(() -> handler.createCommand(mockOperation));
        }

        @Test
        @DisplayName("handles index extraction from various labels")
        void handlesIndexExtraction() {
            SequenceNote note = createNote("note-5", "Text", "RIGHT");
            model.notes.add(note);

            setupLabel("note-5", "Text");
            when(mockOperation.getText()).thenReturn("Updated");

            handler.createCommand(mockOperation);

            assertEquals("Updated", note.getLabel());
        }

        @Test
        @DisplayName("handles concurrent modification")
        void handlesConcurrentModification() {
            SequenceNode p1 = createParticipant("par-0", "A", "PARTICIPANT");
            SequenceNode p2 = createParticipant("par-1", "B", "PARTICIPANT");
            model.participants.add(p1);
            model.participants.add(p2);

            setupLabel("par-0-label", "A");
            when(mockOperation.getText()).thenReturn("A1");
            handler.createCommand(mockOperation);

            setupLabel("par-1-label", "B");
            when(mockOperation.getText()).thenReturn("B1");
            handler.createCommand(mockOperation);

            assertEquals("A1", p1.getName());
            assertEquals("B1", p2.getName());
        }
    }

    @Nested
    @DisplayName("Combined Scenarios")
    class CombinedScenarioTests {

        @Test
        @DisplayName("handles complex diagram with all element types")
        void handlesComplexDiagram() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            model.participants.add(alice);
            model.participants.add(bob);

            SequenceEnglober box = createEnglober("englober-0", "System", "#LightBlue");
            model.englobers.add(box);

            SequenceMessage msg = createMessage("msg-0", alice, bob, "request");
            model.messages.add(msg);

            SequenceGroup group = createGroup(0, "alt", "success", false);
            model.groups.add(group);

            SequenceNote note = createNote("note-0", "Important", "RIGHT");
            model.notes.add(note);

            SequenceAnchor anchor = new SequenceAnchor(alice, bob, "anchor-0", "Duration");
            model.anchors.add(anchor);

            model.title = "Test";

            setupLabel("par-0-label", "Alice");
            when(mockOperation.getText()).thenReturn("Alice Updated");
            handler.createCommand(mockOperation);
            assertEquals("Alice Updated", alice.getName());

            setupLabel("englober-label-englober-0", "System");
            when(mockOperation.getText()).thenReturn("System Updated");
            handler.createCommand(mockOperation);
            assertEquals("System Updated", box.getLabel());

            setupLabel("label-0", "request");
            when(mockOperation.getText()).thenReturn("request Updated");
            handler.createCommand(mockOperation);
            assertEquals("request Updated", msg.getMessage());

            setupLabel("group-comment-0", "success");
            when(mockOperation.getText()).thenReturn("success Updated");
            handler.createCommand(mockOperation);
            assertEquals("success Updated", group.getComment());

            setupLabel("note-0", "Important");
            when(mockOperation.getText()).thenReturn("Important Updated");
            handler.createCommand(mockOperation);
            assertEquals("Important Updated", note.getLabel());

            setupLabel("anch-anchor-0", "Duration");
            when(mockOperation.getText()).thenReturn("Duration Updated");
            handler.createCommand(mockOperation);
            assertEquals("Duration Updated", anchor.getLabel());

            setupLabel("title", "Test");
            when(mockOperation.getText()).thenReturn("Test Updated");
            handler.createCommand(mockOperation);
            assertEquals("Test Updated", model.title);
        }
    }

    private SequenceNode createParticipant(String id, String name, String type) {
        return new SequenceNode(id, name, type, 0, null, false);
    }

    private SequenceEnglober createEnglober(String id, String label, String color) {
        return new SequenceEnglober(id, label, null, color, 0);
    }

    private SequenceGroup createGroup(int startIndex, String label, String comment, boolean isGroup) {
        return new SequenceGroup(startIndex, label, comment, 0);
    }

    private SequenceMessage createMessage(String id, SequenceNode from, SequenceNode to, String text) {
        return new SequenceMessage(id, from, to, text, null, "edge");
    }

    private SequenceMessage createDividerMessage(String id, String text) {
        return new SequenceMessage(id, null, null, text, null, "edge:divider");
    }

    private SequenceMessage createDelayMessage(String id, String text) {
        return new SequenceMessage(id, null, null, text, null, "edge:delay");
    }

    private SequenceNote createNote(String id, String label, String position) {
        return new SequenceNote(id, label, position, "#FFFFE0", "NORMAL");
    }

    private Field findFieldInHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);

            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class " + clazz.getName());
    }
}