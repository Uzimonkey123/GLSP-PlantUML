/*
 * File: SequenceFactoryIntegrationTest.java
 * Author: Norman Babiak
 * Description: Integration tests for sequence factory
 * Date: 29.4.2026
 */

package com.diagrams.SequenceDiagram.factory;

import com.diagrams.SequenceDiagram.builders.NodeBuild;
import com.diagrams.SequenceDiagram.factory.SequenceParts.*;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.utils.NodeGap;
import org.eclipse.glsp.graph.GNode;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sequence Factory Tests")
class SequenceFactoryIntegrationTest {
    private SequenceModel model;
    private NodeBuild nodeBuild;
    private SequenceFactoryContext context;

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
        context = new SequenceFactoryContext(model);
    }

    private SequenceNode participant(String id, String name) {
        return new SequenceNode(id, name, "PARTICIPANT", 0, null, false);
    }

    private SequenceMessage message(String id, SequenceNode from, SequenceNode to, String text, String type) {
        return new SequenceMessage(id, from, to, text, null, type);
    }

    private void setupTwoParticipants() {
        model.participants.add(participant("par-0", "Alice"));
        model.participants.add(participant("par-1", "Bob"));
        context.getCentre().put("par-0", 100.0);
        context.getCentre().put("par-1", 300.0);
        context.getCentre().put("[", 0.0);
        context.getCentre().put("]", 400.0);
        context.getHalfWidth().put("par-0", 40.0);
        context.getHalfWidth().put("par-1", 40.0);
        context.setCursor(400.0);
        context.setGapCalculator(new NodeGap(model));
        context.getMessagesYPos().add(70.0);
        context.getMessagesYPos().add(105.0);
    }

    @Test
    @DisplayName("creates nodes with center positions and invisible exo nodes")
    void nodeFactory() {
        model.participants.add(participant("par-0", "Alice"));
        model.participants.add(participant("par-1", "Bob"));
        context.getMessagesYPos().add(70.0);
        context.setGapCalculator(new NodeGap(model));

        new SequenceNodeFactory(context, nodeBuild, 100.0).createNodes();

        assertEquals(4, context.getCentre().size());
        assertTrue(context.getCentre().containsKey("["));
        assertTrue(context.getCentre().containsKey("]"));
        assertTrue(context.getCentre().get("par-0") > 0);
        assertTrue(context.getCentre().get("par-1") > context.getCentre().get("par-0"));
        assertTrue(context.getElements().stream().anyMatch(element -> element.getId().equals("par-0")));
        assertTrue(context.getElements().stream().anyMatch(element -> element.getId().equals("par-1")));
    }

    @Test
    @DisplayName("creates divider, delay, and reference elements")
    void specialMessages() {
        setupTwoParticipants();

        model.messages.add(message("m0", null, null, "Init", "edge:divider"));
        new SequenceMessageFactory(context).createEdges();
        assertFalse(context.getElements().isEmpty());

        context.getElements().clear();
        model.messages.clear();
        model.messages.add(message("m1", null, null, "Wait", "edge:delay"));
        new SequenceMessageFactory(context).createEdges();
        assertFalse(context.getElements().isEmpty());

        context.getElements().clear();
        model.messages.clear();
        model.messages.add(message("m2", model.participants.get(0), model.participants.get(1), "See other", "edge:ref"));
        new SequenceMessageFactory(context).createEdges();
        assertFalse(context.getElements().isEmpty());
    }

    @Test
    @DisplayName("handles empty message list")
    void emptyMessages() {
        setupTwoParticipants();

        assertDoesNotThrow(() -> new SequenceMessageFactory(context).createEdges());
        assertTrue(context.getElements().isEmpty());
    }

    @Test
    @DisplayName("creates nested groups and separator labels")
    void nestedGroupsAndSeparators() {
        setupTwoParticipants();
        model.messages.add(message("m0", model.participants.get(0), model.participants.get(1), "req", "edge"));
        model.messages.add(message("m1", model.participants.get(1), model.participants.get(0), "res", "edge"));
        SequenceGroup outer = new SequenceGroup(0, "alt", "outer", 0);
        outer.setEndIndex(2);
        outer.addSeparator(1);
        outer.addSeparatorLabel("else");
        SequenceGroup inner = new SequenceGroup(0, "opt", "inner", 1);
        inner.setEndIndex(1);
        model.groups.add(outer);
        model.groups.add(inner);

        new SequenceGroupFactory(context).createGroups();

        assertTrue(context.getElements().size() >= 2, "Should create elements for both groups");
    }

    @Test
    @DisplayName("creates englober box spanning both participants")
    void engloberBox() {
        model.participants.add(participant("par-0", "Alice"));
        model.participants.add(participant("par-1", "Bob"));
        model.participants.get(0).addEngloberId("eng-0");
        model.participants.get(1).addEngloberId("eng-0");
        context.getCentre().put("par-0", 100.0);
        context.getCentre().put("par-1", 300.0);
        context.getHalfWidth().put("par-0", 40.0);
        context.getHalfWidth().put("par-1", 40.0);
        model.englobers.add(new SequenceEnglober("eng-0", "Frontend", null, "#LightBlue", 0));

        new SequenceEngloberFactory(context, 150.0).createEnglobers();

        assertTrue(context.getElements().stream().anyMatch(element -> element.getId().equals("eng-0")));
        GNode engloberNode = context.getElements().stream()
                .filter(element -> element.getId().equals("eng-0"))
                .filter(element -> element instanceof GNode).map(element -> (GNode) element)
                .findFirst().orElse(null);
        assertNotNull(engloberNode);
        assertTrue(engloberNode.getSize().getWidth() > 100, "Englober should span both participants");
    }

    @Test
    @DisplayName("creates life event bar and destroy cross")
    void lifeEvents() {
        model.participants.add(participant("par-0", "Alice"));
        context.getCentre().put("par-0", 100.0);
        context.getLifeEventYPos().add(70.0);
        context.getLifeEventYPos().add(105.0);
        context.getMessagesYPos().add(70.0);
        context.getMessagesYPos().add(105.0);
        model.participants.getFirst().addLifeEvent(new SequenceLifeEvent(0, 1, null));

        new SequenceLifeEventFactory(context).createSequenceLifeEvents();

        assertFalse(context.getElements().isEmpty(), "Should create activation bar");

        context.getElements().clear();
        model.participants.getFirst().getLifeEvents().clear();
        model.participants.getFirst().setDestroyIndex(1);

        new SequenceLifeEventFactory(context).createSequenceLifeEvents();

        assertFalse(context.getElements().isEmpty(), "Should create destroy element");
    }

    @Test
    @DisplayName("creates note with correct id")
    void noteFactory() {
        setupTwoParticipants();
        SequenceNoteFactory noteFactory = new SequenceNoteFactory(context);
        SequenceMessage noteMessage = new SequenceMessage("m0", model.participants.getFirst(), null, "edge:note");
        SequenceNote note = new SequenceNote("n0", "Important", "RIGHT", "#FFFFE0", "NORMAL");
        noteMessage.addNotes(note);
        model.messages.add(noteMessage);

        noteFactory.createNote(noteMessage, 0);

        assertFalse(context.getElements().isEmpty());
        assertTrue(context.getElements().stream().anyMatch(element -> element.getId().equals("n0")));
    }

    @Test
    @DisplayName("creates note spanning multiple participants")
    void noteOverSeveral() {
        setupTwoParticipants();
        SequenceMessage noteMessage = new SequenceMessage("m0", model.participants.get(0), model.participants.get(1), "edge:note");
        noteMessage.addNotes(new SequenceNote("n0", "Shared", "OVER_SEVERAL", "#FFFFE0", "NORMAL"));
        model.messages.add(noteMessage);

        new SequenceNoteFactory(context).createNote(noteMessage, 0);

        assertFalse(context.getElements().isEmpty());
        assertFalse(context.getElements().isEmpty(), "Should create note element for OVER_SEVERAL");
    }

    @Test
    @DisplayName("handles nested activations with stacked bars")
    void nestedActivations() {
        model.participants.add(participant("par-0", "Alice"));
        context.getCentre().put("par-0", 100.0);
        context.getLifeEventYPos().add(70.0);
        context.getLifeEventYPos().add(105.0);
        context.getMessagesYPos().add(70.0);
        context.getMessagesYPos().add(105.0);
        SequenceLifeEvent outerEvent = new SequenceLifeEvent(0, 1, null);
        outerEvent.setLevel(0);
        SequenceLifeEvent innerEvent = new SequenceLifeEvent(0, 1, null);
        innerEvent.setLevel(1);
        model.participants.getFirst().addLifeEvent(outerEvent);
        model.participants.getFirst().addLifeEvent(innerEvent);

        new SequenceLifeEventFactory(context).createSequenceLifeEvents();

        assertTrue(context.getElements().size() >= 2, "Nested activations should produce at least 2 elements");
    }
}
