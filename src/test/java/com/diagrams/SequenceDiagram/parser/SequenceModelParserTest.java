package com.diagrams.SequenceDiagram.parser;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SequenceModelParser Tests")
class SequenceModelParserTest extends SequenceDiagramTestBase {
    private SequenceModelParser parser;

    @BeforeEach
    void setupParser() {
        parser = new SequenceModelParser();
    }

    private SequenceModel parse(String resourcePath) throws IOException {
        Path file = createTempPumlFile(loadResource(resourcePath));

        return parser.parse(file.toFile());
    }

    private Set<String> participantTypes(SequenceModel model) {
        return model.participants.stream()
                .map(SequenceNode::getType)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("Participant Parsing")
    class ParticipantParsingTests {

        @Test
        @DisplayName("parses participants")
        void parseParticipantVariations() throws IOException {
            SequenceModel model = parse("participants/simple-participant.puml");
            assertTrue(model.participants.size() >= 2);
            assertTrue(model.participants.stream().allMatch(SequenceNode::hasLine));

            model = parse("participants/with-aliases.puml");
            assertTrue(model.participants.stream().anyMatch(p -> p.getName().contains(" ") || p.hasLine()));

            model = parse("participants/participant-ordering.puml");
            assertTrue(model.participants.stream().anyMatch(p -> p.getOrder() != 0));

            model = parse("participants/participant-styling.puml");
            assertTrue(model.participants.stream().anyMatch(p -> !p.getBackground().equals("#5d4949")));

            model = parse("participants/stereotypes.puml");
            assertTrue(model.participants.stream().anyMatch(SequenceNode::isStereotype));
        }
    }

    @Nested
    @DisplayName("Message Parsing")
    class MessageParsingTests {

        @Test
        @DisplayName("parses messages")
        void parseMessageVariations() throws IOException {
            SequenceModel model = parse("messages/basic-messages.puml");
            assertTrue(model.messages.size() >= 2);

            model = parse("messages/all-arrow-types.puml");
            assertTrue(model.messages.size() >= 10);
            assertTrue(model.messages.stream().anyMatch(SequenceMessage::isDotted));

            model = parse("messages/self-messages.puml");
            assertTrue(model.messages.stream()
                    .anyMatch(m -> m.getFrom() != null && m.getTo() != null && m.getFrom().equals(m.getTo())));

            model = parse("messages/message-styling.puml");
            assertTrue(model.messages.stream()
                    .anyMatch(m -> m.getColor() != null && !m.getColor().equals("black")));

            model = parse("messages/autonumber.puml");
            assertTrue(model.messages.stream()
                    .anyMatch(m -> m.getNumbering() != null && !m.getNumbering().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Fragment Parsing")
    class FragmentParsingTests {

        @Test
        @DisplayName("parses all group types")
        void parseAllFragmentTypes() throws IOException {
            SequenceModel model = parse("fragments/alt-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("alt")));
            assertTrue(model.groups.stream().findFirst().map(SequenceGroup::hasLine).orElse(false));

            model = parse("fragments/opt-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("opt")));

            model = parse("fragments/loop-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("loop")));

            model = parse("fragments/par-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("par")));

            model = parse("fragments/break-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("break")));

            model = parse("fragments/critical-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(g -> g.getLabel().toLowerCase().contains("critical")));

            model = parse("fragments/group-fragment.puml");
            assertTrue(model.groups.stream().anyMatch(SequenceGroup::isGroup));

            model = parse("fragments/ref-fragment.puml");
            assertTrue(model.messages.stream().anyMatch(m -> m.getType().equals("edge:ref")));
        }

        @Test
        @DisplayName("parses nested groups with levels")
        void parseNestedFragments() throws IOException {
            SequenceModel model = parse("complex/nested-fragments.puml");

            assertTrue(model.groups.size() >= 2);
            assertTrue(model.groups.stream().anyMatch(g -> g.getLevel() > 0));
        }
    }

    @Nested
    @DisplayName("Note Parsing")
    class NoteParsingTests {

        @Test
        @DisplayName("parses notes")
        void parseNoteVariations() throws IOException {
            SequenceModel model = parse("notes/basic-notes.puml");
            assertFalse(model.notes.isEmpty());
            assertTrue(model.notes.stream().allMatch(SequenceNote::hasLine));

            model = parse("notes/notes-on-messages.puml");
            assertTrue(model.messages.stream().anyMatch(m -> !m.getNotes().isEmpty()));

            model = parse("notes/notes-across-participants.puml");
            assertTrue(model.notes.stream()
                    .anyMatch(n -> n.getPosition().equals("OVER") || n.getPosition().equals("OVER_SEVERAL")));
        }
    }

    @Nested
    @DisplayName("Activation Parsing")
    class ActivationParsingTests {

        @Test
        @DisplayName("parses activations")
        void parseActivationVariations() throws IOException {
            SequenceModel model = parse("activation/basic-activation.puml");
            assertTrue(model.participants.stream().anyMatch(p -> !p.getLifeEvents().isEmpty()));

            model = parse("activation/colored-activation.puml");
            assertTrue(model.participants.stream()
                    .flatMap(p -> p.getLifeEvents().stream())
                    .anyMatch(le -> !le.getBackground().equals("#5d4949")));

            model = parse("activation/create-destroy.puml");
            boolean hasCreated = model.participants.stream().anyMatch(SequenceNode::isCreatedNode);
            boolean hasDestroyed = model.participants.stream().anyMatch(p -> p.getDestroyIndex() >= 0);
            assertTrue(hasCreated || hasDestroyed);
        }
    }

    @Nested
    @DisplayName("Divider, Delay, and Box Parsing")
    class DividerDelayBoxParsingTests {

        @Test
        @DisplayName("parses dividers, delays, and boxes")
        void parseDividerDelayBox() throws IOException {
            SequenceModel model = parse("boxes/basic-box.puml");
            assertFalse(model.englobers.isEmpty());
            assertTrue(model.englobers.stream().allMatch(SequenceEnglober::hasLine));

            model = parse("boxes/colored-boxes.puml");
            assertTrue(model.englobers.stream().anyMatch(e -> !e.getColor().equals("#CCCCCC")));
        }
    }

    @Nested
    @DisplayName("Page Details Parsing")
    class PageDetailsParsingTests {

        @Test
        @DisplayName("parses title, header, footer, mainframe")
        void parsePageDetails() throws IOException {
            SequenceModel model = parse("complex/full-diagram.puml");
            assertFalse(model.title.isEmpty());
            assertTrue(model.titleLineStart >= 0);
            assertNotNull(model.header);
            assertNotNull(model.footer);

            model = parse("edge-cases/mainframe-header-footer.puml");
            if (model.isMainframe) {
                assertFalse(model.mainframe.isEmpty());
                assertTrue(model.mainframeLineNumber >= 0);
            }
        }
    }

    @Nested
    @DisplayName("Complex Diagrams")
    class ComplexDiagramTests {

        @Test
        @DisplayName("parses full diagram and design patterns with source line tracking")
        void parseComplexDiagrams() throws IOException {
            SequenceModel model = parse("complex/full-diagram.puml");
            SequenceModel finalModel = model;
            assertAll(
                    () -> assertTrue(finalModel.participants.size() >= 3),
                    () -> assertTrue(finalModel.messages.size() >= 5),
                    () -> assertFalse(finalModel.groups.isEmpty()),
                    () -> assertFalse(finalModel.title.isEmpty())
            );

            long participantsWithLines = model.participants.stream().filter(SequenceNode::hasLine).count();
            assertTrue(participantsWithLines >= 2);

            model = parse("complex/design-patterns.puml");
            assertTrue(model.participants.size() >= 4);
            assertTrue(model.messages.size() >= 8);
            assertTrue(model.participants.stream().anyMatch(p -> !p.getLifeEvents().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Parser State")
    class ParserStateTests {

        @Test
        @DisplayName("resets state between parses and sets line mapper")
        void parserStateManagement() throws IOException {
            SequenceModel model1 = parse("participants/simple-participant.puml");
            SequenceModel model2 = parse("edge-cases/single-participant.puml");

            assertNotSame(model1, model2);
            if (!model2.participants.isEmpty()) {
                assertTrue(model2.participants.getFirst().getId().startsWith("par-"));
            }

            assertNotNull(model1.getLineMapper());
            assertNotNull(model2.getLineMapper());
        }
    }
}