/*
 * File: SequenceReconstructorTest.java
 * Author: Norman Babiak
 * Description: Tests for reconstructor classes, line mapper and line finder
 * Date: 29.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper.LineType;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Sequence Reconstructor Tests")
class SequenceReconstructorTest extends SequenceDiagramTestBase {

    private void finderFromSource(String source) {
        model = new SequenceModel();
        lineMapper = new LineMapper(source);
        model.setMapper(lineMapper);
        elementToLineMap = new HashMap<>();
        lineFinder = new LineFinder(lineMapper, elementToLineMap);
    }

    private long countType(LineType type) {
        return lineMapper.getLineInfos().stream().filter(info -> info.type == type).count();
    }

    @Test
    @DisplayName("mapper: detects participants, messages, groups, notes, activations")
    void mapperCoreTypes() {
        createMapper("participants/simple-participant.puml");
        assertEquals(3, countType(LineType.PARTICIPANT));

        createMapper("messages/basic-messages.puml");
        assertTrue(countType(LineType.MESSAGE) >= 3);

        createMapper("fragments/opt-fragment.puml");
        assertTrue(lineMapper.getLineInfos().stream()
                .anyMatch(info -> info.type == LineType.GROUP_START && info.trimmedText.contains("opt")));

        createMapper("notes/basic-notes.puml");
        assertTrue(countType(LineType.NOTE) >= 2);

        createMapper("activation/basic-activation.puml");
        assertTrue(countType(LineType.ACTIVATE) > 0 && countType(LineType.DEACTIVATE) > 0);
    }

    @Test
    @DisplayName("mapper: detects englobers, returns, references, anchors")
    void mapperExtendedTypes() {
        createMapper("boxes/basic-box.puml");
        assertTrue(countType(LineType.ENGLOBER) > 0 && countType(LineType.END_ENGLOBER) > 0);

        createMapper("messages/return-messages.puml");
        assertTrue(countType(LineType.RETURN) > 0);

        createMapper("fragments/ref-fragment.puml");
        assertTrue(countType(LineType.REFERENCE) > 0);

        lineMapper = new LineMapper(puml("participant A", "participant B",
                "{start} A -> B : go", "{end} A -> B : done", "{start} <-> {end} : time"));
        assertTrue(countType(LineType.ANCHOR) > 0);
    }

    @Test
    @DisplayName("mapper: nested groups have matching start/end counts")
    void mapperNestedGroups() {
        createMapper("complex/nested-fragments.puml");

        assertEquals(countType(LineType.GROUP_START), countType(LineType.GROUP_END));
        assertTrue(countType(LineType.GROUP_START) >= 3);
    }

    @Test
    @DisplayName("mapper: detects comments, page details, and handles edge cases")
    void mapperMisc() {
        createMapper("edge-cases/with-comments.puml");
        assertTrue(lineMapper.getLineInfos().stream()
                .anyMatch(info -> info.type == LineType.COMMENT && info.trimmedText.startsWith("'")));

        createMapper("edge-cases/mainframe-header-footer.puml");
        assertTrue(countType(LineType.TITLE) > 0 && countType(LineType.HEADER) > 0 && countType(LineType.FOOTER) > 0);

        createMapper("edge-cases/empty-diagram.puml");
        assertEquals(0, countType(LineType.PARTICIPANT));
        assertNotNull(lineMapper.getLineInfo(0));
        assertNull(lineMapper.getLineInfo(-1));
        assertNull(lineMapper.getLineInfo(1000));
    }

    @Test
    @DisplayName("mapper: full-diagram has all line types")
    void mapperFullDiagram() {
        createMapper("complex/full-diagram.puml");

        assertAll(
                () -> assertTrue(countType(LineType.PARTICIPANT) >= 3),
                () -> assertTrue(countType(LineType.MESSAGE) >= 5),
                () -> assertTrue(countType(LineType.GROUP_START) > 0),
                () -> assertTrue(countType(LineType.ACTIVATE) > 0),
                () -> assertTrue(countType(LineType.NOTE) > 0)
        );
    }

    @Test
    @DisplayName("finder: locates participants, tracks in map, returns -1 for missing")
    void finderParticipants() {
        createFinder("participants/simple-participant.puml");
        Object elementA = new Object();
        Object elementB = new Object();
        int lineA = lineFinder.findParticipantLine("Alice", elementA);
        int lineB = lineFinder.findParticipantLine("Bob", elementB);

        assertTrue(lineA >= 0 && lineB > lineA);
        assertEquals(lineA, elementToLineMap.get(elementA));
        assertEquals(-1, lineFinder.findParticipantLine("NonExistent", new Object()));
    }

    @Test
    @DisplayName("finder: locates messages sequentially")
    void finderMessages() {
        finderFromSource(puml("participant A", "participant B", "A -> B : Hello", "A -> B: think", "B --> A : Goodbye"));

        int helloLine = lineFinder.findMessageLine("Hello", new Object());
        int thinkLine = lineFinder.findMessageLine("think", new Object());
        int goodbyeLine = lineFinder.findMessageLine("Goodbye", new Object());

        assertTrue(helloLine >= 0 && thinkLine > helloLine && goodbyeLine > thinkLine);
        assertEquals(-1, lineFinder.findMessageLine("nonexistent_xyz", new Object()));
    }

    @Test
    @DisplayName("finder: locates alt start, else, and end in order")
    void finderGroups() {
        createFinder("fragments/alt-fragment.puml");

        int startLine = lineFinder.findGroupStartLine("alt", new Object());
        int elseLine = lineFinder.findGroupElseLine("else", new Object());
        int endLine = lineFinder.findGroupEndLine(new Object());

        assertTrue(startLine >= 0 && elseLine > startLine && endLine > elseLine);
    }

    @Test
    @DisplayName("finder: locates nested groups in correct order")
    void finderNestedGroups() {
        createFinder("complex/nested-fragments.puml");

        int outerStart = lineFinder.findGroupStartLine("loop", new Object());
        int innerStart = lineFinder.findGroupStartLine("alt", new Object());
        int innerEnd = lineFinder.findGroupEndLine(new Object());
        int outerEnd = lineFinder.findGroupEndLine(new Object());

        assertTrue(outerStart >= 0 && innerStart > outerStart && innerEnd > innerStart && outerEnd > innerEnd);
    }

    @Test
    @DisplayName("finder: locates activate/deactivate, create/destroy")
    void finderActivation() {
        createFinder("activation/basic-activation.puml");
        int activateLine = lineFinder.findActivateLine("Server", new Object());
        int deactivateLine = lineFinder.findDeactivateLine("Server", new Object());

        assertTrue(activateLine >= 0 && deactivateLine > activateLine);

        createFinder("activation/create-destroy.puml");
        int createLine = lineFinder.findCreateLine("Worker", new Object());
        int destroyLine = lineFinder.findDestroyLine("Worker", new Object());

        assertTrue(createLine >= 0 && destroyLine > createLine);
    }

    @Test
    @DisplayName("finder: locates notes, englobers, returns, anchors")
    void finderMisc() {
        createFinder("notes/basic-notes.puml");
        assertTrue(lineFinder.findNoteLine("left", new Object()) >= 0);

        createFinder("boxes/colored-boxes.puml");
        assertTrue(lineFinder.findEngloberLine("Client Layer", new Object()) >= 0);

        finderFromSource(puml("participant A", "participant B", "A->B:call", "return success"));
        assertTrue(lineFinder.findReturnLine("success", new Object()) >= 0);

        finderFromSource(puml("participant A", "participant B",
                "{start} A -> B : go", "B -> A : back", "{end} A -> B : done", "{start} <-> {end} : time"));
        assertTrue(lineFinder.findAnchorLine("start", new Object()) >= 0);
    }

    @Test
    @DisplayName("finder: position management and element tracking")
    void finderPosition() {
        finderFromSource(puml("participant A", "participant B", "A -> B : first", "A -> B : second"));

        assertEquals(0, lineFinder.getCurrentPosition());
        lineFinder.findParticipantLine("A", "element1");
        assertTrue(lineFinder.getCurrentPosition() > 0);

        lineFinder.setPosition(4);
        assertEquals(4, lineFinder.findMessageLine("", new Object()));

        lineFinder.resetSearch();
        assertEquals(0, lineFinder.getCurrentPosition());

        finderFromSource(puml("participant A", "A -> A : msg"));
        lineFinder.findParticipantLine("A", "participant");
        lineFinder.findMessageLine("msg", "message");

        assertEquals(2, elementToLineMap.size());
    }
}
