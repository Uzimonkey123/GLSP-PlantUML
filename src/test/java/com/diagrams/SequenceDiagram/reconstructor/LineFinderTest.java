package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineFinder Tests")
class LineFinderTest extends SequenceDiagramTestBase {

    private void createFinderFromSource(String source) {
        model = new SequenceModel();
        lineMapper = new LineMapper(source);
        model.setMapper(lineMapper);
        elementToLineMap = new HashMap<>();
        lineFinder = new LineFinder(lineMapper, elementToLineMap);
    }

    @Nested
    @DisplayName("Participant Line Finding")
    class ParticipantLineTests {

        @Test
        @DisplayName("finds participants that tracks in map")
        void findParticipantsAndTrack() {
            createFinder("participants/simple-participant.puml");

            Object elemA = new Object();
            Object elemB = new Object();

            int lineA = lineFinder.findParticipantLine("Alice", elemA);
            int lineB = lineFinder.findParticipantLine("Bob", elemB);
            int lineC = lineFinder.findParticipantLine("Charlie", new Object());

            assertAll(
                    () -> assertTrue(lineA >= 0),
                    () -> assertTrue(lineB > lineA),
                    () -> assertTrue(lineC > lineB),
                    () -> assertEquals(lineA, elementToLineMap.get(elemA)),
                    () -> assertEquals(lineB, elementToLineMap.get(elemB))
            );
        }

        @Test
        @DisplayName("finds participant with alias")
        void findAliasAndNotFound() {
            createFinder("participants/with-aliases.puml");

            int aliasLine = lineFinder.findParticipantLine("User Interface Layer", new Object());
            int notFound = lineFinder.findParticipantLine("NonExistent", new Object());

            assertAll(
                    () -> assertTrue(aliasLine >= 0),
                    () -> assertEquals(-1, notFound)
            );
        }
    }

    @Nested
    @DisplayName("Message Line Finding")
    class MessageLineTests {

        @Test
        @DisplayName("finds messages sequentially by label")
        void findMessagesSequentially() {
            createFinderFromSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : Hello World",
                    "A -> B: think",
                    "B --> A : Goodbye"
            ));

            int helloLine = lineFinder.findMessageLine("Hello", new Object());
            int thinkLine = lineFinder.findMessageLine("think", new Object());
            int goodbyeLine = lineFinder.findMessageLine("Goodbye", new Object());

            assertAll(
                    () -> assertTrue(helloLine >= 0),
                    () -> assertTrue(thinkLine > helloLine),
                    () -> assertTrue(goodbyeLine > thinkLine),
                    () -> assertEquals(-1, lineFinder.findMessageLine("nonexistent_xyz", new Object()))
            );
        }
    }

    @Nested
    @DisplayName("Group Finding")
    class GroupFindingTests {

        @Test
        @DisplayName("finds alt  with else and end")
        void findAltWithElseAndEnd() {
            createFinder("fragments/alt-fragment.puml");

            int startLine = lineFinder.findGroupStartLine("alt", new Object());
            int elseLine = lineFinder.findGroupElseLine("else", new Object());
            int endLine = lineFinder.findGroupEndLine(new Object());

            assertAll(
                    () -> assertTrue(startLine >= 0),
                    () -> assertTrue(elseLine > startLine),
                    () -> assertTrue(endLine > elseLine)
            );
        }

        @Test
        @DisplayName("finds nested fragments in correct order")
        void findNestedFragments() {
            createFinder("complex/nested-fragments.puml");

            int outerStart = lineFinder.findGroupStartLine("loop", new Object());
            int innerStart = lineFinder.findGroupStartLine("alt", new Object());
            int innerEnd = lineFinder.findGroupEndLine(new Object());
            int outerEnd = lineFinder.findGroupEndLine(new Object());

            assertAll(
                    () -> assertTrue(outerStart >= 0),
                    () -> assertTrue(innerStart > outerStart),
                    () -> assertTrue(innerEnd > innerStart),
                    () -> assertTrue(outerEnd > innerEnd)
            );
        }
    }

    @Nested
    @DisplayName("Reference Finding")
    class ReferenceFindingTests {

        @Test
        @DisplayName("finds reference by keyword and participant")
        void findReference() {
            createFinder("fragments/ref-fragment.puml");

            assertTrue(lineFinder.findReferenceLine("ref", new Object()) >= 0);
            lineFinder.resetSearch();
            assertTrue(lineFinder.findReferenceLine("Client", new Object()) >= 0);
        }
    }

    @Nested
    @DisplayName("Activation Finding")
    class ActivationFindingTests {

        @Test
        @DisplayName("finds activate and deactivate lines")
        void findActivateDeactivate() {
            createFinder("activation/basic-activation.puml");

            int activate = lineFinder.findActivateLine("Server", new Object());
            int deactivate = lineFinder.findDeactivateLine("Server", new Object());

            assertAll(
                    () -> assertTrue(activate >= 0),
                    () -> assertTrue(deactivate > activate)
            );
        }

        @Test
        @DisplayName("finds create and destroy lines")
        void findCreateDestroy() {
            createFinder("activation/create-destroy.puml");

            int create = lineFinder.findCreateLine("Worker", new Object());
            int destroy = lineFinder.findDestroyLine("Worker", new Object());

            assertAll(
                    () -> assertTrue(create >= 0),
                    () -> assertTrue(destroy > create)
            );
        }
    }

    @Nested
    @DisplayName("Note Finding")
    class NoteFindingTests {

        @Test
        @DisplayName("finds note and note end")
        void findNoteAndEnd() {
            createFinder("notes/basic-notes.puml");

            int noteLine = lineFinder.findNoteLine("left", new Object());
            int noteEnd = lineFinder.findNoteEndLine(new Object());

            assertAll(
                    () -> assertTrue(noteLine >= 0),
                    () -> assertTrue(noteEnd >= 0)
            );
        }
    }

    @Nested
    @DisplayName("Englober Finding")
    class EngloberFindingTests {

        @Test
        @DisplayName("finds englobers and preserves position")
        void findBoxesAndPreservePosition() {
            createFinder("boxes/colored-boxes.puml");

            int line1 = lineFinder.findEngloberLine("Client Layer", new Object());
            int line2 = lineFinder.findEngloberLine("Application Layer", new Object());
            int line3 = lineFinder.findEngloberLine("Infrastructure", new Object());

            assertAll(
                    () -> assertTrue(line1 >= 0),
                    () -> assertTrue(line2 >= 0),
                    () -> assertTrue(line3 >= 0)
            );
        }
    }

    @Nested
    @DisplayName("Return and Anchor Finding")
    class ReturnAnchorTests {

        @Test
        @DisplayName("finds return lines sequentially")
        void findReturns() {
            createFinderFromSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : call",
                    "return success",
                    "A -> B : another",
                    "return failure"
            ));

            int successLine = lineFinder.findReturnLine("success", new Object());
            int failureLine = lineFinder.findReturnLine("failure", new Object());

            assertAll(
                    () -> assertTrue(successLine >= 0),
                    () -> assertTrue(failureLine > successLine)
            );
        }

        @Test
        @DisplayName("finds anchor by ID")
        void findAnchor() {
            createFinderFromSource(puml(
                    "participant Alice",
                    "participant Bob",
                    "{start} Alice -> Bob : start doing things",
                    "Bob -> Alice : something",
                    "{end} Alice -> Bob : finish",
                    "{start} <-> {end} : some time"
            ));

            assertTrue(lineFinder.findAnchorLine("start", new Object()) >= 0);
        }
    }

    @Nested
    @DisplayName("Position Management")
    class PositionTests {

        @Test
        @DisplayName("manages search position correctly")
        void positionManagement() {
            createFinderFromSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : first",
                    "A -> B : second"
            ));

            assertEquals(0, lineFinder.getCurrentPosition());

            lineFinder.findParticipantLine("A", new Object());
            assertTrue(lineFinder.getCurrentPosition() > 0);

            lineFinder.setPosition(4);
            int line = lineFinder.findMessageLine("", new Object());
            assertEquals(4, line);

            lineFinder.resetSearch();
            assertEquals(0, lineFinder.getCurrentPosition());
        }
    }

    @Nested
    @DisplayName("Element Tracking")
    class ElementTrackingTests {

        @Test
        @DisplayName("tracks elements")
        void trackElementsIgnoreNull() {
            createFinderFromSource(puml(
                    "participant A",
                    "participant B",
                    "A -> B : message"
            ));

            Object elemA = "participantA";
            Object elemMsg = "message1";

            lineFinder.findParticipantLine("A", elemA);
            lineFinder.findParticipantLine("B", null);
            lineFinder.findMessageLine("message", elemMsg);

            assertAll(
                    () -> assertEquals(2, elementToLineMap.size()),
                    () -> assertNotNull(elementToLineMap.get(elemA)),
                    () -> assertNotNull(elementToLineMap.get(elemMsg))
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles empty diagram")
        void handleEdgeCase() {
            createFinder("edge-cases/empty-diagram.puml");
            assertAll(
                    () -> assertEquals(-1, lineFinder.findParticipantLine("any", new Object())),
                    () -> assertEquals(-1, lineFinder.findMessageLine("any", new Object()))
            );
        }

        @Test
        @DisplayName("handles special characters and empty search")
        void handleSpecialAndEmpty() {
            createFinder("messages/basic-messages.puml");
            assertTrue(lineFinder.findMessageLine("", new Object()) >= 0);

            createFinder("edge-cases/special-characters.puml");
            assertTrue(lineFinder.findParticipantLine("User", new Object()) >= 0);
        }
    }
}