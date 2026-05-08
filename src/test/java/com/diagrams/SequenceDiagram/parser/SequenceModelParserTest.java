/*
 * File: SequenceModelParserTest.java
 * Author: Norman Babiak
 * Description: Tests for sequence model parser
 * Date: 29.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.SequenceDiagram.parser;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("SequenceModelParser Tests")
class SequenceModelParserTest extends SequenceDiagramTestBase {
    private SequenceModelParser parser;

    @BeforeEach
    void setup() throws Exception {
        parser = new SequenceModelParser();
        Field field = SequenceModelParser.class.getDeclaredField("modelState");
        field.setAccessible(true);
        field.set(parser, mock(SequenceModelState.class));
    }

    private SequenceModel parse(String resourcePath) throws IOException {
        return parser.parse(createTempPumlFile(loadResource(resourcePath)).toFile());
    }

    private SequenceModel parseSource(String source) throws IOException {
        return parser.parse(createTempPumlFile(source).toFile());
    }

    @Test
    @DisplayName("parses participants with line info")
    void participants() throws IOException {
        SequenceModel model = parse("participants/simple-participant.puml");

        assertTrue(model.participants.size() >= 2);
        assertTrue(model.participants.stream().allMatch(SequenceNode::hasLine));
    }

    @Test
    @DisplayName("parses aliases and ordering")
    void aliasesAndOrdering() throws IOException {
        SequenceModel aliased = parse("participants/with-aliases.puml");
        SequenceModel ordered = parse("participants/participant-ordering.puml");

        assertTrue(aliased.participants.stream().anyMatch(participant -> participant.getName().contains(" ")));
        assertTrue(ordered.participants.stream().anyMatch(participant -> participant.getOrder() != 0));
    }

    @Test
    @DisplayName("parses basic and styled messages")
    void messages() throws IOException {
        SequenceModel basic = parse("messages/basic-messages.puml");
        SequenceModel allTypes = parse("messages/all-arrow-types.puml");

        assertTrue(basic.messages.size() >= 2);
        assertTrue(allTypes.messages.size() >= 10);
        assertTrue(allTypes.messages.stream().anyMatch(SequenceMessage::isDotted));
    }

    @Test
    @DisplayName("parses self-messages")
    void selfMessages() throws IOException {
        SequenceModel model = parseSource(puml("participant A", "A -> A : think", "A -> A : process"));

        assertEquals(1, model.participants.size());
        assertEquals(2, model.messages.stream().filter(message -> message.getType().equals("edge"))
                .filter(message -> message.getFrom() != null && message.getFrom().equals(message.getTo())).count());
    }

    @Test
    @DisplayName("parses all fragment types and else branches")
    void fragments() throws IOException {
        SequenceModel alt = parse("fragments/alt-fragment.puml");
        SequenceModel loop = parse("fragments/loop-fragment.puml");
        SequenceModel ref = parse("fragments/ref-fragment.puml");

        assertTrue(alt.groups.stream().anyMatch(group -> group.getLabel().contains("alt")));
        assertTrue(loop.groups.stream().anyMatch(group -> group.getLabel().contains("loop")));
        assertTrue(ref.messages.stream().anyMatch(message -> message.getType().equals("edge:ref")));

        SequenceModel multiElse = parseSource(puml("participant A", "participant B",
                "alt c1", "A->B:1", "else c2", "A->B:2", "else c3", "A->B:3", "else c4", "A->B:4", "end"));

        assertTrue(multiElse.groups.getFirst().getSeparatorLabel().size() >= 3);
    }

    @Test
    @DisplayName("parses nested groups with correct levels")
    void nestedGroups() throws IOException {
        SequenceModel model = parse("complex/nested-fragments.puml");

        assertTrue(model.groups.size() >= 2);
        assertTrue(model.groups.stream().anyMatch(group -> group.getLevel() > 0));
    }

    @Test
    @DisplayName("parses 5 deeply nested groups")
    void deeplyNested() throws IOException {
        SequenceModel model = parseSource(puml("participant A", "participant B",
                "alt l1", "opt l2", "loop l3", "par l4", "critical l5",
                "A->B:deep", "end", "end", "end", "end", "end"));

        assertEquals(5, model.groups.size());
    }

    @Test
    @DisplayName("parses notes: basic, on messages, across participants")
    void notes() throws IOException {
        SequenceModel basic = parse("notes/basic-notes.puml");
        SequenceModel onMessages = parse("notes/notes-on-messages.puml");
        SequenceModel across = parse("notes/notes-across-participants.puml");

        assertFalse(basic.notes.isEmpty());
        assertTrue(onMessages.messages.stream().anyMatch(message -> !message.getNotes().isEmpty()));
        assertTrue(across.notes.stream()
                .anyMatch(note -> note.getPosition().equals("OVER") || note.getPosition().equals("OVER_SEVERAL")));
    }

    @Test
    @DisplayName("parses activations, create, destroy")
    void activations() throws IOException {
        SequenceModel activated = parse("activation/basic-activation.puml");
        SequenceModel createDestroy = parse("activation/create-destroy.puml");

        assertTrue(activated.participants.stream().anyMatch(participant -> !participant.getLifeEvents().isEmpty()));
        assertTrue(createDestroy.participants.stream().anyMatch(SequenceNode::isCreatedNode) ||
                createDestroy.participants.stream().anyMatch(participant -> participant.getDestroyIndex() >= 0));
    }

    @Test
    @DisplayName("parses boxes with colors")
    void englobers() throws IOException {
        SequenceModel basic = parse("boxes/basic-box.puml");
        SequenceModel colored = parse("boxes/colored-boxes.puml");

        assertFalse(basic.englobers.isEmpty());
        assertTrue(colored.englobers.stream().anyMatch(englober -> !englober.getColor().equals("#CCCCCC")));
    }

    @Test
    @DisplayName("parses title, header, footer, mainframe")
    void pageDetails() throws IOException {
        SequenceModel model = parse("complex/full-diagram.puml");

        assertFalse(model.title.isEmpty());
        assertNotNull(model.header);
        assertNotNull(model.footer);
    }

    @Test
    @DisplayName("implicit participants created from messages")
    void implicitParticipants() throws IOException {
        SequenceModel model = parseSource(puml("Alice -> Bob : hi"));

        assertEquals(2, model.participants.size());
    }

    @Test
    @DisplayName("handles empty diagram, CRLF, and blank lines")
    void edgeCases() throws IOException {
        SequenceModel empty = parseSource(puml(""));
        SequenceModel crlf = parseSource("@startuml\r\nparticipant A\r\nparticipant B\r\n@enduml");

        assertTrue(empty.participants.isEmpty() && empty.messages.isEmpty());
        assertEquals(2, crlf.participants.size());
    }

    @Test
    @DisplayName("full-diagram: all element types present with line tracking")
    void fullDiagram() throws IOException {
        SequenceModel model = parse("complex/full-diagram.puml");

        assertAll(
                () -> assertTrue(model.participants.size() >= 3),
                () -> assertTrue(model.messages.size() >= 5),
                () -> assertFalse(model.groups.isEmpty()),
                () -> assertFalse(model.title.isEmpty()),
                () -> assertTrue(model.participants.stream().filter(SequenceNode::hasLine).count() >= 2)
        );
    }

    @Test
    @DisplayName("parser resets state and sets line mapper between parses")
    void parserState() throws IOException {
        SequenceModel first = parse("participants/simple-participant.puml");
        SequenceModel second = parse("edge-cases/single-participant.puml");

        assertNotSame(first, second);
        assertNotNull(first.getLineMapper());
        assertNotNull(second.getLineMapper());
    }
}
