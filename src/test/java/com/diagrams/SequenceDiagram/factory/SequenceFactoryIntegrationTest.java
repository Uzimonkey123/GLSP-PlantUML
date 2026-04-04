package com.diagrams.SequenceDiagram.factory;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceParts.*;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.utils.NodeGap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sequence Factory Integration Tests")
class SequenceFactoryIntegrationTest {
    private SequenceModel model;
    private NodeBuild nodeBuild;
    private SequenceFactoryContext ctx;

    @BeforeEach
    void setup() {
        model = new SequenceModel();
        model.participants = new ArrayList<>();
        model.messages = new ArrayList<>();
        model.groups = new ArrayList<>();
        model.englobers = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.anchors = new ArrayList<>();
        model.messageSpaces = new HashMap<>();

        model.header = "";
        model.footer = "";
        model.title = "";
        model.mainframe = "";
        model.isMainframe = false;
        model.showFoot = true;

        nodeBuild = new NodeBuild();
        ctx = new SequenceFactoryContext(model);
    }

    @Nested
    @DisplayName("Node Factory Tests")
    class NodeFactoryTests {

        @Test
        @DisplayName("creates nodes for participants")
        void createsNodesForParticipants() {
            model.participants.add(createParticipant("par-0", "Alice", "PARTICIPANT"));
            model.participants.add(createParticipant("par-1", "Bob", "PARTICIPANT"));

            ctx.getMessagesYPos().add(70.0);
            ctx.setGapCalculator(new NodeGap(model));

            SequenceNodeFactory factory = new SequenceNodeFactory(ctx, nodeBuild, 100.0);
            factory.createNodes();

            assertFalse(ctx.getElements().isEmpty());
            assertFalse(ctx.getCentre().isEmpty());
            assertFalse(ctx.getHalfWidth().isEmpty());
        }

        @Test
        @DisplayName("calculates center positions for all participants")
        void calculatesCenterPositions() {
            model.participants.add(createParticipant("par-0", "Alice", "PARTICIPANT"));
            model.participants.add(createParticipant("par-1", "Bob", "PARTICIPANT"));
            model.participants.add(createParticipant("par-2", "Charlie", "PARTICIPANT"));

            ctx.getMessagesYPos().add(70.0);
            ctx.setGapCalculator(new NodeGap(model));

            SequenceNodeFactory factory = new SequenceNodeFactory(ctx, nodeBuild, 100.0);
            factory.createNodes();

            assertEquals(5, ctx.getCentre().size()); // 3 base + 2 invisible for exo
            assertTrue(ctx.getCentre().containsKey("par-0"));
            assertTrue(ctx.getCentre().containsKey("par-1"));
            assertTrue(ctx.getCentre().containsKey("par-2"));
        }

        @Test
        @DisplayName("creates invisible nodes for exo messages")
        void createsInvisibleNodes() {
            model.participants.add(createParticipant("par-0", "Alice", "PARTICIPANT"));

            ctx.getMessagesYPos().add(70.0);
            ctx.setGapCalculator(new NodeGap(model));

            SequenceNodeFactory factory = new SequenceNodeFactory(ctx, nodeBuild, 100.0);
            factory.createNodes();

            assertTrue(ctx.getCentre().containsKey("["));
            assertTrue(ctx.getCentre().containsKey("]"));
        }
    }

    @Nested
    @DisplayName("Message Factory Tests")
    class MessageFactoryTests {

        @BeforeEach
        void setupMessages() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            model.participants.add(alice);
            model.participants.add(bob);

            ctx.getCentre().put("par-0", 100.0);
            ctx.getCentre().put("par-1", 300.0);
            ctx.getCentre().put("[", 0.0);
            ctx.getCentre().put("]", 400.0);
            ctx.getHalfWidth().put("par-0", 40.0);
            ctx.getHalfWidth().put("par-1", 40.0);
            ctx.setCursor(400.0);
            ctx.setGapCalculator(new NodeGap(model));

            ctx.getMessagesYPos().add(70.0);
            ctx.getMessagesYPos().add(105.0);
        }

        @Test
        @DisplayName("creates divider, delay, and reference elements")
        void createsSpecialMessageElements() {
            NodeGap gapCalculator = new NodeGap(model);

            SequenceMessage divider = createDivider("msg-0", "Initialization");
            model.messages.add(divider);
            SequenceMessageFactory factory = new SequenceMessageFactory(ctx);
            factory.createEdges();
            assertFalse(ctx.getElements().isEmpty());

            ctx.getElements().clear();
            model.messages.clear();
            SequenceMessage delay = createDelay("msg-1", "5 minutes later");
            model.messages.add(delay);
            factory = new SequenceMessageFactory(ctx);
            factory.createEdges();
            assertFalse(ctx.getElements().isEmpty());

            ctx.getElements().clear();
            model.messages.clear();
            SequenceMessage ref = createReference("msg-2",
                    model.participants.get(0), model.participants.get(1), "See other diagram");
            model.messages.add(ref);
            factory = new SequenceMessageFactory(ctx);
            factory.createEdges();
            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("handles empty message list")
        void handlesEmptyMessageList() {
            SequenceMessageFactory factory = new SequenceMessageFactory(ctx);

            assertDoesNotThrow(factory::createEdges);
            assertTrue(ctx.getElements().isEmpty());
        }
    }

    @Nested
    @DisplayName("Group Factory Tests")
    class GroupFactoryTests {

        @BeforeEach
        void setupGroups() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            model.participants.add(alice);
            model.participants.add(bob);

            ctx.getCentre().put("par-0", 100.0);
            ctx.getCentre().put("par-1", 300.0);

            // Add messages to define group bounds
            model.messages.add(createMessage("msg-0", alice, bob, "request"));
            model.messages.add(createMessage("msg-1", bob, alice, "response"));

            ctx.getMessagesYPos().add(70.0);
            ctx.getMessagesYPos().add(105.0);
        }

        @Test
        @DisplayName("creates group outline")
        void createsGroupOutline() {
            SequenceGroup group = createGroup(0, 2, "alt", "condition", 0);
            model.groups.add(group);

            SequenceGroupFactory factory = new SequenceGroupFactory(ctx);
            factory.createGroups();

            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("creates nested groups in correct order")
        void createsNestedGroupsCorrectly() {
            SequenceGroup outer = createGroup(0, 2, "alt", "outer condition", 0);
            SequenceGroup inner = createGroup(0, 1, "opt", "inner condition", 1);

            model.groups.add(outer);
            model.groups.add(inner);

            SequenceGroupFactory factory = new SequenceGroupFactory(ctx);
            factory.createGroups();

            assertTrue(ctx.getElements().size() >= 2);
        }

        @Test
        @DisplayName("creates separator labels for else branches")
        void createsSeparatorLabels() {
            SequenceGroup group = createGroup(0, 2, "alt", "case1", 0);
            group.addSeparator(1);
            group.addSeparatorLabel("case2");
            model.groups.add(group);

            SequenceGroupFactory factory = new SequenceGroupFactory(ctx);
            factory.createGroups();

            assertFalse(ctx.getElements().isEmpty());
        }
    }

    @Nested
    @DisplayName("Englober Factory Tests")
    class EngloberFactoryTests {

        @BeforeEach
        void setupEnglobers() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            alice.addEngloberId("englober-0");
            bob.addEngloberId("englober-0");
            model.participants.add(alice);
            model.participants.add(bob);

            ctx.getCentre().put("par-0", 100.0);
            ctx.getCentre().put("par-1", 300.0);
            ctx.getHalfWidth().put("par-0", 40.0);
            ctx.getHalfWidth().put("par-1", 40.0);
        }

        @Test
        @DisplayName("creates englober box")
        void createsEngloberBox() {
            SequenceEnglober englober = createEnglober("englober-0", "Frontend", "#LightBlue");
            model.englobers.add(englober);

            SequenceEngloberFactory factory = new SequenceEngloberFactory(ctx, 150.0);
            factory.createEnglobers();

            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("handles multiple englobers")
        void handlesMultipleEnglobers() {
            SequenceNode charlie = createParticipant("par-2", "Charlie", "PARTICIPANT");
            charlie.addEngloberId("englober-1");
            model.participants.add(charlie);
            ctx.getCentre().put("par-2", 500.0);
            ctx.getHalfWidth().put("par-2", 40.0);

            SequenceEnglober frontend = createEnglober("englober-0", "Frontend", "#LightBlue");
            SequenceEnglober backend = createEnglober("englober-1", "Backend", "#LightGreen");
            model.englobers.add(frontend);
            model.englobers.add(backend);

            SequenceEngloberFactory factory = new SequenceEngloberFactory(ctx, 150.0);
            factory.createEnglobers();

            assertTrue(ctx.getElements().size() >= 2);
        }
    }

    @Nested
    @DisplayName("Life Event Factory Tests")
    class LifeEventFactoryTests {

        @BeforeEach
        void setupLifeEvents() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            model.participants.add(alice);

            ctx.getCentre().put("par-0", 100.0);

            ctx.getLifeEventYPos().add(70.0);
            ctx.getLifeEventYPos().add(105.0);
            ctx.getMessagesYPos().add(70.0);
            ctx.getMessagesYPos().add(105.0);
        }

        @Test
        @DisplayName("creates life event bar")
        void createsLifeEventBar() {
            SequenceNode alice = model.participants.getFirst();
            SequenceLifeEvent lifeEvent = new SequenceLifeEvent(0, 1, null);
            alice.addLifeEvent(lifeEvent);

            SequenceLifeEventFactory factory = new SequenceLifeEventFactory(ctx);
            factory.createSequenceLifeEvents();

            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("creates destroy cross")
        void createsDestroyCross() {
            SequenceNode alice = model.participants.getFirst();
            alice.setDestroyIndex(1);

            SequenceLifeEventFactory factory = new SequenceLifeEventFactory(ctx);
            factory.createSequenceLifeEvents();

            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("handles nested activations")
        void handlesNestedActivations() {
            SequenceNode alice = model.participants.getFirst();
            SequenceLifeEvent outer = new SequenceLifeEvent(0, 1, null);
            outer.setLevel(0);
            SequenceLifeEvent inner = new SequenceLifeEvent(0, 1, null);
            inner.setLevel(1);
            alice.addLifeEvent(outer);
            alice.addLifeEvent(inner);

            SequenceLifeEventFactory factory = new SequenceLifeEventFactory(ctx);
            factory.createSequenceLifeEvents();

            assertTrue(ctx.getElements().size() >= 2);
        }
    }

    @Nested
    @DisplayName("Note Factory Tests")
    class NoteFactoryTests {

        @BeforeEach
        void setupNotes() {
            SequenceNode alice = createParticipant("par-0", "Alice", "PARTICIPANT");
            SequenceNode bob = createParticipant("par-1", "Bob", "PARTICIPANT");
            model.participants.add(alice);
            model.participants.add(bob);

            ctx.getCentre().put("par-0", 100.0);
            ctx.getCentre().put("par-1", 300.0);
            ctx.getHalfWidth().put("par-0", 40.0);
            ctx.getHalfWidth().put("par-1", 40.0);

            ctx.getMessagesYPos().add(70.0);
            ctx.getMessagesYPos().add(105.0);
        }

        @Test
        @DisplayName("creates note elements including over multiple participants")
        void createsNoteElements() {
            SequenceNoteFactory factory = new SequenceNoteFactory(ctx);

            SequenceNode alice = model.participants.getFirst();
            SequenceMessage noteMsg = createNoteMessage("msg-0", alice);
            SequenceNote note = createNote("note-0", "Important note", "RIGHT");
            noteMsg.addNotes(note);
            model.messages.add(noteMsg);
            factory.createNote(noteMsg, 0);
            assertFalse(ctx.getElements().isEmpty());

            ctx.getElements().clear();
            SequenceNode bob = model.participants.get(1);
            SequenceMessage overMsg = new SequenceMessage("msg-1", alice, bob, "edge:note");
            SequenceNote overNote = createNote("note-1", "Shared note", "OVER_SEVERAL");
            overMsg.addNotes(overNote);
            model.messages.add(overMsg);
            factory.createNote(overMsg, 1);
            assertFalse(ctx.getElements().isEmpty());
        }

        @Test
        @DisplayName("handles different note positions")
        void handlesDifferentNotePositions() {
            SequenceNode alice = model.participants.getFirst();

            SequenceMessage leftNoteMsg = createNoteMessage("msg-0", alice);
            SequenceNote leftNote = createNote("note-0", "Left", "LEFT");
            leftNoteMsg.addNotes(leftNote);

            SequenceMessage rightNoteMsg = createNoteMessage("msg-1", alice);
            SequenceNote rightNote = createNote("note-1", "Right", "RIGHT");
            rightNoteMsg.addNotes(rightNote);

            SequenceMessage overNoteMsg = createNoteMessage("msg-2", alice);
            SequenceNote overNote = createNote("note-2", "Over", "OVER");
            overNoteMsg.addNotes(overNote);

            ctx.getMessagesYPos().add(105.0);
            ctx.getMessagesYPos().add(140.0);

            SequenceNoteFactory factory = new SequenceNoteFactory(ctx);
            factory.createNote(leftNoteMsg, 0);
            factory.createNote(rightNoteMsg, 1);
            factory.createNote(overNoteMsg, 2);

            assertTrue(ctx.getElements().size() >= 3);
        }
    }

    private SequenceNode createParticipant(String id, String name, String type) {
        return new SequenceNode(id, name, type, 0, null, false);
    }

    private SequenceMessage createDivider(String id, String text) {
        return new SequenceMessage(id, null, null, text, null, "edge:divider");
    }

    private SequenceMessage createDelay(String id, String text) {
        return new SequenceMessage(id, null, null, text, null, "edge:delay");
    }

    private SequenceMessage createReference(String id, SequenceNode from, SequenceNode to, String text) {
        return new SequenceMessage(id, from, to, text, null, "edge:ref");
    }

    private SequenceMessage createNoteMessage(String id, SequenceNode participant) {
        return new SequenceMessage(id, participant, null, "edge:note");
    }

    private SequenceMessage createMessage(String id, SequenceNode from, SequenceNode to, String text) {
        return new SequenceMessage(id, from, to, text, null, "edge");
    }

    private SequenceGroup createGroup(int startIndex, int endIndex, String label, String comment, int level) {
        SequenceGroup group = new SequenceGroup(startIndex, label, comment, level);
        group.setEndIndex(endIndex);
        return group;
    }

    private SequenceEnglober createEnglober(String id, String label, String color) {
        return new SequenceEnglober(id, label, null, color, 0);
    }

    private SequenceNote createNote(String id, String label, String position) {
        return new SequenceNote(id, label, position, "#FFFFE0", "NORMAL");
    }
}