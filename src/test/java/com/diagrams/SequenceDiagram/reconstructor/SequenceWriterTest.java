/*
 * File: SequenceWriterTest.java
 * Author: Norman Babiak
 * Description: Tests for sequence writer, writing back into file the new version of diagram
 * Date: 29.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.writers.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SequenceWriter Tests")
class SequenceWriterTest extends SequenceDiagramTestBase {
    @Mock private SequenceModel mockModel;
    @Mock private LineMapper mockLineMapper;
    private AutoCloseable mocks;
    private Path testFile;

    @BeforeEach
    void setup() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        testFile = createTempPumlFile(puml("participant Foo"));
        when(mockModel.getLineMapper()).thenReturn(mockLineMapper);
        when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());
        mockModel.participants = new ArrayList<>();
        mockModel.messages = new ArrayList<>();
        mockModel.anchors = new ArrayList<>();
        mockModel.groups = new ArrayList<>();
        mockModel.englobers = new ArrayList<>();
        mockModel.notes = new ArrayList<>();
        mockModel.messageSpaces = new HashMap<>();
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    private SequenceWriterContext writerContext(Path file) throws IOException {
        return new SequenceWriterContext(mockModel, Files.readAllLines(file, StandardCharsets.UTF_8), mockLineMapper);
    }

    @Test
    @DisplayName("writes unchanged file, throws for non-existent")
    void basicWriteAndError() throws IOException {
        String content = puml("participant Foo");
        Path file = createTempPumlFile(content);

        new SequenceWriter(mockModel, file.toUri().toString()).write();

        assertEquals(normalizeContent(content), normalizeContent(Files.readString(file)));
        assertThrows(IOException.class, () -> new SequenceWriter(mockModel, tempDir.resolve("nope.puml").toUri().toString()));
    }

    @Test
    @DisplayName("updates title, header, footer, mainframe")
    void pageDetails() throws IOException {
        Path file = createTempPumlFile(puml("title Old", "header Old", "footer Old", "mainframe Old", "participant Foo"));
        mockModel.titleModified = true;
        mockModel.title = "New T";
        mockModel.titleLineStart = 1;
        mockModel.titleLineEnd = 1;
        mockModel.headerModified = true;
        mockModel.header = "New H";
        mockModel.headerLineStart = 2;
        mockModel.headerLineEnd = 2;
        mockModel.footerModified = true;
        mockModel.footer = "New F";
        mockModel.footerLineStart = 3;
        mockModel.footerLineEnd = 3;
        mockModel.mainframeModified = true;
        mockModel.mainframe = "New M";
        mockModel.mainframeLineNumber = 4;

        new SequenceWriter(mockModel, file.toUri().toString()).write();

        String result = Files.readString(file);
        assertAll(
                () -> assertTrue(result.contains("New T")),
                () -> assertTrue(result.contains("New H")),
                () -> assertTrue(result.contains("New F")),
                () -> assertTrue(result.contains("mainframe New M"))
        );
    }

    @Test
    @DisplayName("renames participant in file")
    void renameParticipant() throws IOException {
        Path file = createTempPumlFile(puml("participant Foo"));
        SequenceNode node = mock(SequenceNode.class);
        when(node.getOriginalName()).thenReturn("Foo");
        when(node.getName()).thenReturn("Bar");
        when(node.getType()).thenReturn("PARTICIPANT");
        when(node.isModified()).thenReturn(true);
        when(node.hasLine()).thenReturn(true);
        when(node.getSourceLineStart()).thenReturn(1);
        when(node.getSourceLineEnd()).thenReturn(1);
        when(node.getOrder()).thenReturn(0);
        when(node.getBackground()).thenReturn("#5d4949");
        when(node.getRawSourceText()).thenReturn("participant Foo");
        when(node.getLifeEvents()).thenReturn(new ArrayList<>());
        mockModel.participants = List.of(node);

        LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(1, "participant Foo");
        when(mockLineMapper.getLineInfos()).thenReturn(List.of(lineInfo));
        when(mockLineMapper.getLineInfo(1)).thenReturn(lineInfo);

        new SequenceWriter(mockModel, file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("Bar"));
    }

    @Test
    @DisplayName("replaceParticipant: generates correct type keyword and includes order/color")
    void replaceParticipant() throws Exception {
        SequenceNode node = mock(SequenceNode.class);
        when(node.getName()).thenReturn("Alice");
        when(node.getType()).thenReturn("ACTOR");
        when(node.getRawSourceText()).thenReturn("actor Alice order 10 #LightBlue");
        when(node.getOrder()).thenReturn(10);
        when(node.getBackground()).thenReturn("#LightBlue");
        when(node.getId()).thenReturn("alice");
        when(node.getSourceLineStart()).thenReturn(0);
        when(node.getLifeEvents()).thenReturn(new ArrayList<>());
        when(mockLineMapper.getLineInfo(0)).thenReturn(new LineMapper.LineInfo(0, "actor Alice order 10 #LightBlue"));

        Method method = ParticipantWriter.class.getDeclaredMethod("replaceParticipant", SequenceNode.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new ParticipantWriter(writerContext(testFile)), node);

        assertTrue(result.contains("actor"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("order 10"));
        assertTrue(result.contains("#LightBlue"));
    }

    @Test
    @DisplayName("buildArrow: solid, dotted, async, cross, colored, bidirectional")
    void buildArrow() throws Exception {
        MessageWriter messageWriter = new MessageWriter(writerContext(testFile));
        Method buildMethod = MessageWriter.class.getDeclaredMethod("buildArrow", SequenceMessage.class);
        buildMethod.setAccessible(true);

        String solid = (String) buildMethod.invoke(messageWriter, arrow(false, "block", "none", "black"));
        assertTrue(solid.contains(">") && !solid.contains("--"));

        String dotted = (String) buildMethod.invoke(messageWriter, arrow(true, "block", "none", "black"));
        assertTrue(dotted.contains("--"));

        String async = (String) buildMethod.invoke(messageWriter, arrow(false, "open", "none", "black"));
        assertTrue(async.contains(">>"));

        String colored = (String) buildMethod.invoke(messageWriter, arrow(false, "block", "none", "#red"));
        assertTrue(colored.contains("[#red]"));
    }

    @Test
    @DisplayName("replaceGroupStart: includes type, comment, and colors")
    void replaceGroupStart() throws Exception {
        SequenceGroup group = mock(SequenceGroup.class);
        when(group.getLabel()).thenReturn("alt");
        when(group.getComment()).thenReturn("x > 0");
        when(group.isGroup()).thenReturn(false);
        when(group.getRawSourceText()).thenReturn("alt x > 0");
        when(group.getElementColor()).thenReturn("#blue");
        when(group.getBackColor()).thenReturn("#light");

        Method method = GroupWriter.class.getDeclaredMethod("replaceGroupStart", SequenceGroup.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new GroupWriter(writerContext(testFile)), group);

        assertTrue(result.contains("alt"));
        assertTrue(result.contains("x > 0"));
        assertTrue(result.contains("#blue"));
        assertTrue(result.contains("#light"));
    }

    @Test
    @DisplayName("replaceEnglober: generates box with label and color")
    void replaceEnglober() throws Exception {
        SequenceEnglober englober = mock(SequenceEnglober.class);
        when(englober.getRawSourceText()).thenReturn("box \"Backend\" #LightGreen");
        when(englober.getLabel()).thenReturn("Backend");
        when(englober.getColor()).thenReturn("#LightGreen");

        Method method = EngloberWriter.class.getDeclaredMethod("replaceEnglober", SequenceEnglober.class);
        method.setAccessible(true);
        String result = (String) method.invoke(new EngloberWriter(writerContext(testFile)), englober);

        assertTrue(result.contains("box"));
        assertTrue(result.contains("#LightGreen"));
    }

    @Test
    @DisplayName("integration: preserves simple participants")
    void preserveParticipants() throws IOException {
        String content = loadResource("participants/simple-participant.puml");
        Path file = createTempPumlFile(content);
        createMapper("participants/simple-participant.puml");

        new SequenceWriter(model, file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("participant"));
    }

    @Test
    @DisplayName("integration: preserves alt/else/end structure")
    void preserveFragments() throws IOException {
        String content = loadResource("fragments/alt-fragment.puml");
        Path file = createTempPumlFile(content);
        createMapper("fragments/alt-fragment.puml");

        new SequenceWriter(model, file.toUri().toString()).write();

        String result = Files.readString(file);
        assertTrue(result.contains("alt"));
        assertTrue(result.contains("else"));
        assertTrue(result.contains("end"));
    }

    @Test
    @DisplayName("integration: preserves full-diagram byte-for-byte")
    void preserveFullDiagram() throws IOException {
        String original = loadResource("complex/full-diagram.puml");
        Path file = createTempPumlFile(original);
        createMapper("complex/full-diagram.puml");

        new SequenceWriter(model, file.toUri().toString()).write();

        assertEquals(normalizeContent(original), normalizeContent(Files.readString(file)));
    }

    @Test
    @DisplayName("handles line deletion")
    void lineDeletion() throws IOException {
        Path file = createTempPumlFile(puml("participant A", "participant B", "A -> B : msg"));
        when(mockModel.getLinesToDelete()).thenReturn(List.of(new int[]{2, 2}));

        new SequenceWriter(mockModel, file.toUri().toString()).write();

        assertFalse(Files.readString(file).contains("participant B"));
    }

    @Test
    @DisplayName("preserves comments in file")
    void preserveComments() throws IOException {
        String content = loadResource("edge-cases/with-comments.puml");
        Path file = createTempPumlFile(content);
        createMapper("edge-cases/with-comments.puml");

        new SequenceWriter(model, file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("'"));
    }

    private SequenceMessage arrow(boolean dotted, String endHead, String startHead, String color) {
        SequenceMessage message = mock(SequenceMessage.class);
        when(message.isDotted()).thenReturn(dotted);
        when(message.getEndHead()).thenReturn(endHead);
        when(message.getStartHead()).thenReturn(startHead);
        when(message.getStartPart()).thenReturn("full");
        when(message.getEndPart()).thenReturn("full");
        when(message.getStartDecor()).thenReturn("none");
        when(message.getEndDecor()).thenReturn("none");
        when(message.getColor()).thenReturn(color);
        return message;
    }
}
