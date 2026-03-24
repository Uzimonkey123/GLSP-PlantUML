package com.diagrams.SequenceDiagram.parser;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("SequenceModelParser Edge Case Tests")
class SequenceModelParserComplexTest extends SequenceDiagramTestBase {
    private SequenceModelParser parser;

    @BeforeEach
    void setUpParser() throws Exception {
        parser = new SequenceModelParser();
        SequenceModelState modelState = mock(SequenceModelState.class);
        Field field = SequenceModelParser.class.getDeclaredField("modelState");
        field.setAccessible(true);
        field.set(parser, modelState);
    }

    private SequenceModel parseSource(String source) throws IOException {
        Path file = createTempPumlFile(source);

        return parser.parse(file.toFile());
    }

    @Nested
    @DisplayName("Whitespace and Formatting")
    class WhitespaceTests {

        @Test
        @DisplayName("handles tabs and mixed indentation")
        void handleMixedIndentation() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant Alice",
                    "\tAlice -> Bob : message1",
                    "  \t  Bob -> Alice : message2"
            ));

            assertFalse(result.messages.isEmpty());
        }

        @Test
        @DisplayName("handles Windows CRLF line endings")
        void handleCRLF() throws IOException {
            String source = "@startuml\r\nparticipant A\r\nparticipant B\r\nA -> B : msg\r\n@enduml";
            SequenceModel result = parseSource(source);

            assertEquals(2, result.participants.size());
            assertFalse(result.messages.isEmpty());
        }

        @Test
        @DisplayName("handles mixed line endings")
        void handleMixedLineEndings() throws IOException {
            String source = "@startuml\nparticipant A\r\nparticipant B\rA -> B : msg\n@enduml";
            SequenceModel result = parseSource(source);

            assertNotNull(result);
            assertTrue(result.participants.size() >= 2);
        }

        @Test
        @DisplayName("handles extra blank lines")
        void handleExtraBlankLines() throws IOException {
            SequenceModel result = parseSource(puml(
                    "",
                    "participant Alice",
                    "",
                    "",
                    "participant Bob",
                    "",
                    "Alice -> Bob : message",
                    ""
            ));

            assertEquals(2, result.participants.size());
            assertFalse(result.messages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Empty and Minimal Content")
    class EmptyContentTests {

        @Test
        @DisplayName("handles completely empty diagram")
        void handleEmptyDiagram() throws IOException {
            SequenceModel result = parseSource(puml(""));

            assertTrue(result.participants.isEmpty());
            assertTrue(result.messages.isEmpty());
        }

        @Test
        @DisplayName("handles diagram with only comments")
        void handleOnlyComments() throws IOException {
            SequenceModel result = parseSource(puml(
                    "' This is a comment",
                    "/' Multi-line",
                    "   comment '/",
                    "' Another comment"
            ));

            assertTrue(result.participants.isEmpty());
        }

        @Test
        @DisplayName("handles empty box")
        void handleEmptyBox() throws IOException {
            SequenceModel result = parseSource(puml(
                    "box \"Empty Box\"",
                    "end box"
            ));

            assertEquals(0, result.englobers.size());
        }

        @Test
        @DisplayName("handles empty alt fragment")
        void handleEmptyAltFragment() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "alt condition",
                    "end"
            ));

            assertFalse(result.groups.isEmpty());
        }
    }

    @Nested
    @DisplayName("Self-Referencing Messages")
    class SelfReferenceTests {

        @Test
        @DisplayName("handles single self-message")
        void handleSingleSelfMessage() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant Alice",
                    "Alice -> Alice : think"
            ));

            assertEquals(1, result.participants.size());
            assertFalse(result.messages.isEmpty());

            SequenceMessage msg = result.messages.stream()
                    .filter(m -> m.getType().equals("edge"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(msg);
            assertEquals(msg.getFrom(), msg.getTo());
        }

        @Test
        @DisplayName("handles multiple self-messages")
        void handleMultipleSelfMessages() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant Server",
                    "Server -> Server : validate",
                    "Server -> Server : process",
                    "Server -> Server : respond"
            ));

            assertEquals(1, result.participants.size());

            long selfMsgCount = result.messages.stream()
                    .filter(m -> m.getType().equals("edge"))
                    .filter(m -> m.getFrom() != null && m.getFrom().equals(m.getTo()))
                    .count();
            assertEquals(3, selfMsgCount);
        }
    }

    @Nested
    @DisplayName("Message Arrow Variations")
    class MessageArrowTests {

        @Test
        @DisplayName("handles all basic arrow types")
        void handleAllBasicArrowTypes() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : solid",
                    "A --> B : dotted",
                    "A ->> B : async solid",
                    "A -->> B : async dotted",
                    "A ->x B : lost",
                    "A -\\\\ B : half bottom",
                    "A -/ B : half top"
            ));

            assertTrue(result.messages.size() >= 7);
        }
    }

    @Nested
    @DisplayName("Fragment Edge Cases")
    class FragmentTests {

        @Test
        @DisplayName("handles deeply nested groups")
        void handleDeeplyNestedFragments() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "alt level1",
                    "  opt level2",
                    "    loop level3",
                    "      par level4",
                    "        critical level5",
                    "          A -> B : deep",
                    "        end",
                    "      end",
                    "    end",
                    "  end",
                    "end"
            ));

            assertEquals(5, result.groups.size());
        }

        @Test
        @DisplayName("handles alt with multiple else branches")
        void handleMultipleElseBranches() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "alt case1",
                    "  A -> B : first",
                    "else case2",
                    "  A -> B : second",
                    "else case3",
                    "  A -> B : third",
                    "else default",
                    "  A -> B : default",
                    "end"
            ));

            assertFalse(result.groups.isEmpty());
            SequenceGroup altGroup = result.groups.getFirst();
            assertTrue(altGroup.getSeparatorLabel().size() >= 3);
        }
    }

    @Nested
    @DisplayName("Activation Edge Cases")
    class ActivationTests {

        @Test
        @DisplayName("handles deeply nested activations")
        void handleDeeplyNestedActivations() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : msg1",
                    "activate B",
                    "B -> B : process1",
                    "activate B",
                    "B -> B : process2",
                    "activate B",
                    "B -> B : process3",
                    "deactivate B",
                    "deactivate B",
                    "deactivate B"
            ));

            SequenceNode nodeB = result.participants.stream()
                    .filter(p -> p.getName().equals("B"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(nodeB);
            assertTrue(nodeB.getLifeEvents().size() >= 3);
        }

        @Test
        @DisplayName("handles inline activation with ++")
        void handleInlineActivation() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B++ : activate",
                    "B -> A-- : deactivate"
            ));

            assertFalse(result.messages.isEmpty());
        }

        @Test
        @DisplayName("handles unclosed activations")
        void handleUnclosedActivations() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : msg",
                    "activate B",
                    "B -> A : response"
            ));

            assertNotNull(result);
            assertFalse(result.messages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Note Edge Cases")
    class NoteTests {

        @Test
        @DisplayName("handles multiline note")
        void handleMultilineNote() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "note left of A",
                    "  Line 1",
                    "  Line 2",
                    "  Line 3",
                    "end note"
            ));

            assertFalse(result.notes.isEmpty());
            assertTrue(result.notes.stream()
                    .anyMatch(n -> n.getLabel().contains("Line")));
        }

        @Test
        @DisplayName("handles note across all participants")
        void handleNoteAcross() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "participant C",
                    "note across : spans all"
            ));

            assertFalse(result.notes.isEmpty());
        }

        @Test
        @DisplayName("handles parallel notes with /")
        void handleParallelNotes() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "note over A : first",
                    "/ note over B : parallel"
            ));

            boolean hasParallelNote = result.notes.stream()
                    .anyMatch(SequenceNote::isParalell);
            assertTrue(hasParallelNote);
        }
    }

    @Nested
    @DisplayName("Box Edge Cases")
    class BoxTests {

        @Test
        @DisplayName("handles box with quoted label")
        void handleBoxWithQuotedLabel() throws IOException {
            SequenceModel result = parseSource(puml(
                    "box \"My Application Layer\"",
                    "  participant Service",
                    "end box"
            ));

            assertEquals(1, result.englobers.size());
            assertTrue(result.englobers.getFirst().getLabel().contains("Application"));
        }

        @Test
        @DisplayName("handles multiple boxes")
        void handleMultipleBoxes() throws IOException {
            SequenceModel result = parseSource(puml(
                    "box \"Frontend\" #LightBlue",
                    "  participant UI",
                    "end box",
                    "box \"Backend\" #LightGreen",
                    "  participant API",
                    "  participant DB",
                    "end box"
            ));

            assertEquals(2, result.englobers.size());
        }

        @Test
        @DisplayName("handles box without label")
        void handleBoxWithoutLabel() throws IOException {
            SequenceModel result = parseSource(puml(
                    "box",
                    "  participant A",
                    "end box"
            ));

            assertEquals(1, result.englobers.size());
        }
    }

    @Nested
    @DisplayName("Delay Edge Cases")
    class DividerDelayTests {

        @Test
        @DisplayName("handles empty delay")
        void handleEmptyDelay() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : before",
                    "...",
                    "A -> B : after"
            ));

            boolean hasDelay = result.messages.stream()
                    .anyMatch(m -> m.getType().equals("edge:delay"));
            assertTrue(hasDelay);
        }

        @Test
        @DisplayName("handles spacing directive")
        void handleSpacingDirective() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : first",
                    "|||",
                    "A -> B : second",
                    "||45||",
                    "A -> B : third"
            ));

            assertFalse(result.messageSpaces.isEmpty());
        }
    }

    @Nested
    @DisplayName("Participant Declaration Variations")
    class ParticipantDeclarationTests {

        @Test
        @DisplayName("handles implicit participant creation")
        void handleImplicitParticipant() throws IOException {
            SequenceModel result = parseSource(puml(
                    "Alice -> Bob : message"
            ));

            assertEquals(2, result.participants.size());
        }

        @Test
        @DisplayName("handles participant with long quoted name")
        void handleLongQuotedName() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant \"This is a very long participant name with spaces\" as P",
                    "P -> P : work"
            ));

            assertEquals(1, result.participants.size());
            assertTrue(result.participants.getFirst().getName().contains("very long"));
        }
    }

    @Nested
    @DisplayName("Scale Tests")
    class ScaleTests {

        @Test
        @DisplayName("handles 20 participants")
        void handleManyParticipants() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\n");

            for (int i = 0; i < 20; i++) {
                sb.append("participant P").append(i).append("\n");
            }
            sb.append("@enduml");

            SequenceModel result = parseSource(sb.toString());
            assertEquals(20, result.participants.size());
        }

        @Test
        @DisplayName("handles 50 messages")
        void handleManyMessages() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\n");
            sb.append("participant A\nparticipant B\n");

            for (int i = 0; i < 50; i++) {
                sb.append("A -> B : message").append(i).append("\n");
            }
            sb.append("@enduml");

            SequenceModel result = parseSource(sb.toString());
            assertEquals(50, result.messages.size());
        }

        @Test
        @DisplayName("handles 10 nested groups")
        void handleManyNestedGroups() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\n");
            sb.append("participant A\nparticipant B\n");

            for (int i = 0; i < 10; i++) {
                sb.append("alt level").append(i).append("\n");
            }

            sb.append("A -> B : deep\n");
            sb.append("end\n".repeat(10));
            sb.append("@enduml");

            SequenceModel result = parseSource(sb.toString());
            assertEquals(10, result.groups.size());
        }
    }

    @Nested
    @DisplayName("Return Message Edge Cases")
    class ReturnMessageTests {

        @Test
        @DisplayName("handles return with no text")
        void handleEmptyReturn() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : request",
                    "activate B",
                    "return"
            ));

            assertFalse(result.messages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Autonumber Edge Cases")
    class AutonumberTests {

        @Test
        @DisplayName("handles autonumber with format")
        void handleAutonumberWithFormat() throws IOException {
            SequenceModel result = parseSource(puml(
                    "autonumber \"<b>[000]\"",
                    "participant A",
                    "participant B",
                    "A -> B : message"
            ));

            assertFalse(result.messages.isEmpty());
        }

        @Test
        @DisplayName("handles autonumber stop and resume")
        void handleAutonumberStopResume() throws IOException {
            SequenceModel result = parseSource(puml(
                    "autonumber",
                    "participant A",
                    "participant B",
                    "A -> B : numbered",
                    "autonumber stop",
                    "A -> B : unnumbered",
                    "autonumber resume",
                    "A -> B : numbered again"
            ));

            assertTrue(result.messages.size() >= 3);
        }
    }

    @Nested
    @DisplayName("Parallel Message Edge Cases")
    class ParallelMessageTests {

        @Test
        @DisplayName("handles parallel messages with &")
        void handleParallelMessages() throws IOException {
            SequenceModel result = parseSource(puml(
                    "participant A",
                    "participant B",
                    "participant C",
                    "A -> B : first",
                    "& A -> C : parallel"
            ));

            boolean hasParallel = result.messages.stream()
                    .anyMatch(SequenceMessage::isParallel);
            assertTrue(hasParallel);
        }
    }
}