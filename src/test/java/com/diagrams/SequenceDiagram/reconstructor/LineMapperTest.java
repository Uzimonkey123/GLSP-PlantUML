package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper.LineInfo;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper.LineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineMapper Tests")
class LineMapperTest extends SequenceDiagramTestBase {

    private long countLineType(LineType type) {
        return lineMapper.getLineInfos().stream()
                .filter(info -> info.type == type)
                .count();
    }

    private boolean hasLineType(LineType type) {
        return countLineType(type) > 0;
    }

    private boolean hasLineMatching(String contains) {
        return lineMapper.getLineInfos().stream()
                .anyMatch(info -> info.type == LineType.GROUP_START && info.trimmedText.contains(contains));
    }

    private List<LineInfo> getLinesOfType(LineType type) {
        return lineMapper.getLineInfos().stream()
                .filter(info -> info.type == type)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("Participant Detection")
    class ParticipantDetectionTests {

        @Test
        @DisplayName("detects participants with aliases, colors, and stereotypes")
        void detectParticipantsWithVariations() {
            createMapper("participants/simple-participant.puml");
            assertEquals(3, countLineType(LineType.PARTICIPANT));

            createMapper("participants/with-aliases.puml");
            assertTrue(getLinesOfType(LineType.PARTICIPANT).stream()
                    .anyMatch(info -> info.trimmedText.contains(" as ")));

            createMapper("participants/participant-styling.puml");
            assertTrue(getLinesOfType(LineType.PARTICIPANT).stream()
                    .anyMatch(info -> info.trimmedText.contains("#")));

            createMapper("participants/stereotypes.puml");
            assertTrue(getLinesOfType(LineType.PARTICIPANT).stream()
                    .anyMatch(info -> info.trimmedText.contains("<<")));
        }
    }

    @Nested
    @DisplayName("Message Detection")
    class MessageDetectionTests {

        @Test
        @DisplayName("detects messages with arrow types, colors, and returns")
        void detectMessagesWithVariations() {
            createMapper("messages/basic-messages.puml");
            assertTrue(countLineType(LineType.MESSAGE) >= 3);

            createMapper("messages/all-arrow-types.puml");
            assertTrue(countLineType(LineType.MESSAGE) >= 10);

            createMapper("messages/message-styling.puml");
            assertTrue(getLinesOfType(LineType.MESSAGE).stream()
                    .anyMatch(info -> info.trimmedText.contains("[#")));

            createMapper("messages/return-messages.puml");
            assertTrue(hasLineType(LineType.RETURN));
        }
    }

    @Nested
    @DisplayName("Group Detection")
    class FragmentDetectionTests {

        @Test
        @DisplayName("detects all group types")
        void detectAllFragmentTypes() {
            createMapper("fragments/opt-fragment.puml");
            assertTrue(hasLineMatching("opt"));

            createMapper("fragments/loop-fragment.puml");
            assertTrue(hasLineMatching("loop"));

            createMapper("fragments/par-fragment.puml");
            assertTrue(hasLineMatching("par"));

            createMapper("fragments/break-fragment.puml");
            assertTrue(hasLineMatching("break"));

            createMapper("fragments/critical-fragment.puml");
            assertTrue(hasLineMatching("critical"));

            createMapper("fragments/group-fragment.puml");
            assertTrue(hasLineMatching("group"));

            createMapper("fragments/ref-fragment.puml");
            assertTrue(hasLineType(LineType.REFERENCE));
        }

        @Test
        @DisplayName("detects nested groups with matching start/end counts")
        void detectNestedFragments() {
            createMapper("complex/nested-fragments.puml");

            long startCount = countLineType(LineType.GROUP_START);
            long endCount = countLineType(LineType.GROUP_END);

            assertTrue(startCount >= 3);
            assertEquals(startCount, endCount);
        }
    }

    @Nested
    @DisplayName("Note Detection")
    class NoteDetectionTests {

        @Test
        @DisplayName("detects notes with various positions")
        void detectNotesWithVariations() {
            createMapper("notes/basic-notes.puml");
            assertTrue(countLineType(LineType.NOTE) >= 2);

            createMapper("notes/notes-across-participants.puml");
            assertTrue(getLinesOfType(LineType.NOTE).stream()
                    .anyMatch(info -> info.trimmedText.contains("over")));
        }
    }

    @Nested
    @DisplayName("Activation Detection")
    class ActivationDetectionTests {

        @Test
        @DisplayName("detects activate, deactivate, create, destroy, and colors")
        void detectActivationVariations() {
            createMapper("activation/basic-activation.puml");
            assertTrue(hasLineType(LineType.ACTIVATE));
            assertTrue(hasLineType(LineType.DEACTIVATE));

            createMapper("activation/create-destroy.puml");
            assertTrue(hasLineType(LineType.CREATE));
            assertTrue(hasLineType(LineType.DESTROY));

            createMapper("activation/colored-activation.puml");
            assertTrue(getLinesOfType(LineType.ACTIVATE).stream()
                    .anyMatch(info -> info.trimmedText.contains("#")));
        }
    }

    @Nested
    @DisplayName("Englober Detection")
    class BoxDetectionTests {

        @Test
        @DisplayName("detects boxes with colors")
        void detectEnglobers() {
            createMapper("boxes/basic-box.puml");
            assertTrue(hasLineType(LineType.ENGLOBER));
            assertTrue(hasLineType(LineType.END_ENGLOBER));

            createMapper("boxes/colored-boxes.puml");
            assertTrue(getLinesOfType(LineType.ENGLOBER).stream()
                    .anyMatch(info -> info.trimmedText.contains("#")));
        }
    }

    @Nested
    @DisplayName("Comment and Page Details Detection")
    class CommentPageDetailsTests {

        @Test
        @DisplayName("detects comments and page details")
        void detectCommentsAndPageDetails() {
            createMapper("edge-cases/with-comments.puml");
            List<LineInfo> comments = getLinesOfType(LineType.COMMENT);
            assertTrue(comments.stream().anyMatch(info -> info.trimmedText.startsWith("'")));
            assertTrue(comments.stream().anyMatch(info -> info.trimmedText.startsWith("/'")));

            createMapper("edge-cases/mainframe-header-footer.puml");
            assertAll(
                    () -> assertTrue(hasLineType(LineType.TITLE)),
                    () -> assertTrue(hasLineType(LineType.HEADER)),
                    () -> assertTrue(hasLineType(LineType.FOOTER)),
                    () -> assertTrue(hasLineType(LineType.MAINFRAME))
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles empty and simple diagram")
        void handleEdgeCaseDiagrams() {
            createMapper("edge-cases/empty-diagram.puml");
            assertEquals(0, countLineType(LineType.PARTICIPANT));
            assertEquals(0, countLineType(LineType.MESSAGE));

            createMapper("edge-cases/single-participant.puml");
            assertEquals(1, countLineType(LineType.PARTICIPANT));
        }

        @Test
        @DisplayName("getLineInfo returns correct values")
        void getLineInfoBehavior() {
            createMapper("participants/simple-participant.puml");

            LineInfo info = lineMapper.getLineInfo(0);
            assertNotNull(info);
            assertEquals(LineType.START_UML, info.type);

            assertNull(lineMapper.getLineInfo(-1));
            assertNull(lineMapper.getLineInfo(1000));
        }
    }

    @Nested
    @DisplayName("Autonumber and Anchor Detection")
    class AutonumberAnchorTests {

        @Test
        @DisplayName("detects autonumber and anchor syntax")
        void detectAutonumberAndAnchors() {
            createMapper("messages/autonumber.puml");
            assertTrue(lineMapper.getLineInfos().stream()
                    .anyMatch(info -> info.trimmedText.startsWith("autonumber")));

            String source = puml(
                    "participant Alice",
                    "participant Bob",
                    "{start} Alice -> Bob : start doing things",
                    "Bob -> Alice : something",
                    "{end} Alice -> Bob : finish",
                    "{start} <-> {end} : some time"
            );

            lineMapper = new LineMapper(source);
            assertTrue(hasLineType(LineType.ANCHOR));
        }
    }

    @Nested
    @DisplayName("Complex Diagrams")
    class ComplexDiagramTests {

        @Test
        @DisplayName("handles full-diagram with all line types")
        void handleFullDiagram() {
            createMapper("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(hasLineType(LineType.START_UML)),
                    () -> assertTrue(hasLineType(LineType.TITLE)),
                    () -> assertTrue(countLineType(LineType.PARTICIPANT) >= 3),
                    () -> assertTrue(countLineType(LineType.MESSAGE) >= 5),
                    () -> assertTrue(hasLineType(LineType.GROUP_START)),
                    () -> assertTrue(hasLineType(LineType.ACTIVATE)),
                    () -> assertTrue(hasLineType(LineType.NOTE))
            );
        }
    }
}