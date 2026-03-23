package com.diagrams.SequenceDiagram.reconstructor;

import com.diagrams.SequenceDiagram.SequenceDiagramTestBase;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Method;
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
    void setUpMocks() throws IOException {
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
    void tearDownMocks() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("initializes with mock model")
        void initializeWithMock() throws IOException {
            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            assertNotNull(writer);
        }

        @Test
        @DisplayName("throws IOException for non-existent file")
        void throwsForNonExistent() {
            Path nonExistent = tempDir.resolve("nonexistent.puml");
            assertThrows(IOException.class, () ->
                    new SequenceWriter(mockModel, nonExistent.toUri().toString()));
        }

        @Test
        @DisplayName("initializes with real model")
        void initializeWithRealModel() throws IOException {
            createMapper("participants/simple-participant.puml");
            Path file = createTempPumlFile(loadResource("participants/simple-participant.puml"));

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            assertNotNull(writer);
        }
    }

    @Nested
    @DisplayName("write() Integration")
    class WriteIntegrationTests {

        @Test
        @DisplayName("writes file with no modifications")
        void writeNoModifications() throws IOException {
            String content = puml("participant Foo");
            Path file = createTempPumlFile(content);

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertEquals(normalizeContent(content), normalizeContent(result));
        }

        @Test
        @DisplayName("updates page title")
        void updateTitle() throws IOException {
            String content = puml("title Old Title", "participant Foo");
            Path file = createTempPumlFile(content);

            mockModel.titleModified = true;
            mockModel.title = "New Title";
            mockModel.titleLineStart = 1;
            mockModel.titleLineEnd = 1;

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("title New Title"));
        }

        @Test
        @DisplayName("updates page header")
        void updateHeader() throws IOException {
            String content = puml("header Old Header", "participant Foo");
            Path file = createTempPumlFile(content);

            mockModel.headerModified = true;
            mockModel.header = "New Header";
            mockModel.headerLineStart = 1;
            mockModel.headerLineEnd = 1;

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("header New Header"));
        }

        @Test
        @DisplayName("updates page footer")
        void updateFooter() throws IOException {
            String content = puml("footer Old Footer", "participant Foo");
            Path file = createTempPumlFile(content);

            mockModel.footerModified = true;
            mockModel.footer = "New Footer";
            mockModel.footerLineStart = 1;
            mockModel.footerLineEnd = 1;

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("footer New Footer"));
        }

        @Test
        @DisplayName("updates mainframe")
        void updateMainframe() throws IOException {
            String content = puml("mainframe Old Frame", "participant Foo");
            Path file = createTempPumlFile(content);

            mockModel.mainframeModified = true;
            mockModel.mainframe = "New Frame";
            mockModel.mainframeLineNumber = 1;

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("mainframe New Frame"));
        }

        @Test
        @DisplayName("renames participant in file")
        void renameParticipant() throws IOException {
            String content = puml("participant Foo");
            Path file = createTempPumlFile(content);

            SequenceNode node = createMockNode("Foo", "Bar", "PARTICIPANT", 1);
            when(node.getRawSourceText()).thenReturn("participant Foo");
            mockModel.participants = List.of(node);

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(1, "participant Foo");
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(lineInfo));
            when(mockLineMapper.getLineInfo(1)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("Bar"));
        }
    }

    @Nested
    @DisplayName("replaceParticipant")
    class ReplaceParticipantTests {

        @Test
        @DisplayName("generates simple participant")
        void simpleParticipant() throws Exception {
            SequenceNode node = createSimpleMockNode("Alice", "PARTICIPANT");
            when(node.getRawSourceText()).thenReturn("participant Alice");

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "participant Alice");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("participant"));
            assertTrue(result.contains("Alice"));
        }

        @Test
        @DisplayName("generates actor declaration")
        void actorDeclaration() throws Exception {
            SequenceNode node = createSimpleMockNode("User", "ACTOR");
            when(node.getRawSourceText()).thenReturn("actor User");

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "actor User");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("actor"));
            assertTrue(result.contains("User"));
        }

        @Test
        @DisplayName("generates database declaration")
        void databaseDeclaration() throws Exception {
            SequenceNode node = createSimpleMockNode("DB", "DATABASE");
            when(node.getRawSourceText()).thenReturn("database DB");

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "database DB");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("database"));
        }

        @Test
        @DisplayName("handles participant with alias")
        void participantWithAlias() throws Exception {
            SequenceNode node = createSimpleMockNode("User Interface", "PARTICIPANT");
            when(node.getRawSourceText()).thenReturn("participant \"User Interface\" as UI");

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "participant \"User Interface\" as UI");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("as UI") || result.contains("User Interface"));
        }

        @Test
        @DisplayName("includes order")
        void participantWithOrder() throws Exception {
            SequenceNode node = createSimpleMockNode("Alice", "PARTICIPANT");
            when(node.getRawSourceText()).thenReturn("participant Alice order 10");
            when(node.getOrder()).thenReturn(10);

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "participant Alice order 10");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("order 10"));
        }

        @Test
        @DisplayName("includes background color")
        void participantWithColor() throws Exception {
            SequenceNode node = createSimpleMockNode("Alice", "PARTICIPANT");
            when(node.getRawSourceText()).thenReturn("participant Alice #LightBlue");
            when(node.getBackground()).thenReturn("#LightBlue");

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "participant Alice #LightBlue");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceParticipant(writer, node);

            assertTrue(result.contains("#LightBlue"));
        }

        private String invokeReplaceParticipant(SequenceWriter writer, SequenceNode node) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceParticipant", SequenceNode.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, node);
        }
    }

    @Nested
    @DisplayName("replaceMessage")
    class ReplaceMessageTests {

        @Test
        @DisplayName("generates delay message")
        void delayMessage() throws Exception {
            SequenceMessage message = createMockMessage("edge:delay", "5 minutes later");
            when(message.getRawSourceText()).thenReturn("...5 minutes later...");
            when(message.getSourceLineStart()).thenReturn(0);

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "...5 minutes later...");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceMessage(writer, message);

            assertTrue(result.contains("...5 minutes later..."));
        }

        @Test
        @DisplayName("generates divider message")
        void dividerMessage() throws Exception {
            SequenceMessage message = createMockMessage("edge:divider", "Initialization");
            when(message.getRawSourceText()).thenReturn("==Initialization==");
            when(message.getSourceLineStart()).thenReturn(0);

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "==Initialization==");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceMessage(writer, message);

            assertTrue(result.contains("==Initialization=="));
        }

        @Test
        @DisplayName("generates return message")
        void returnMessage() throws Exception {
            SequenceMessage message = createMockMessage("edge:message", "result");
            when(message.getRawSourceText()).thenReturn("return result");
            when(message.getSourceLineStart()).thenReturn(0);

            LineMapper.LineInfo lineInfo = new LineMapper.LineInfo(0, "return result");
            when(mockLineMapper.getLineInfo(0)).thenReturn(lineInfo);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceMessage(writer, message);

            assertTrue(result.contains("return"));
            assertTrue(result.contains("result"));
        }

        private String invokeReplaceMessage(SequenceWriter writer, SequenceMessage message) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceMessage", SequenceMessage.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, message);
        }
    }

    @Nested
    @DisplayName("messageArrow")
    class MessageArrowTests {

        @Test
        @DisplayName("generates arrow variations")
        void arrowVariations() throws Exception {
            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());

            SequenceMessage solid = createMockMessageWithArrow(false, "block", "none");
            String solidResult = invokeMessageArrow(writer, solid);
            assertTrue(solidResult.contains("-") && solidResult.contains(">") && !solidResult.contains("--"));

            SequenceMessage dotted = createMockMessageWithArrow(true, "block", "none");
            String dottedResult = invokeMessageArrow(writer, dotted);
            assertTrue(dottedResult.contains("--"));

            SequenceMessage async = createMockMessageWithArrow(false, "open", "none");
            String asyncResult = invokeMessageArrow(writer, async);
            assertTrue(asyncResult.contains(">>"));

            SequenceMessage cross = createMockMessageWithArrow(false, "cross", "none");
            String crossResult = invokeMessageArrow(writer, cross);
            assertTrue(crossResult.contains("x"));
        }

        @Test
        @DisplayName("generates arrow with color")
        void coloredArrow() throws Exception {
            SequenceMessage message = createMockMessageWithArrow(false, "block", "none");
            when(message.getColor()).thenReturn("#red");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeMessageArrow(writer, message);

            assertTrue(result.contains("[#red]"));
        }

        @Test
        @DisplayName("generates bidirectional arrow")
        void bidirectionalArrow() throws Exception {
            SequenceMessage message = createMockMessageWithArrow(false, "block", "block");
            when(message.getStartHead()).thenReturn("block");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeMessageArrow(writer, message);

            assertTrue(result.contains("<") && result.contains(">"));
        }

        @Test
        @DisplayName("generates arrow with circle decoration")
        void arrowWithCircle() throws Exception {
            SequenceMessage message = createMockMessageWithArrow(false, "block", "none");
            when(message.getEndDecor()).thenReturn("circle");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeMessageArrow(writer, message);

            assertTrue(result.contains("o"));
        }

        private String invokeMessageArrow(SequenceWriter writer, SequenceMessage message) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("messageArrow", SequenceMessage.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, message);
        }
    }

    @Nested
    @DisplayName("replaceAnchor")
    class ReplaceAnchorTests {

        @Test
        @DisplayName("generates anchor with label")
        void anchorWithLabel() throws Exception {
            SequenceAnchor anchor = mock(SequenceAnchor.class);
            when(anchor.getRawSourceText()).thenReturn("{start} <-> {end} : Original Label");
            when(anchor.getLabel()).thenReturn("New Label");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceAnchor(writer, anchor);

            assertTrue(result.contains("{start}"));
            assertTrue(result.contains("{end}"));
            assertTrue(result.contains("<->"));
            assertTrue(result.contains("New Label"));
        }


        private String invokeReplaceAnchor(SequenceWriter writer, SequenceAnchor anchor) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceAnchor", SequenceAnchor.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, anchor);
        }
    }

    @Nested
    @DisplayName("replaceGroupStart")
    class ReplaceGroupStartTests {

        @Test
        @DisplayName("generates group types")
        void fragmentTypes() throws Exception {
            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());

            SequenceGroup alt = createMockGroup("alt", "condition1", false);
            String altResult = invokeReplaceGroupStart(writer, alt);
            assertTrue(altResult.contains("alt") && altResult.contains("condition1"));

            SequenceGroup opt = createMockGroup("opt", "optional condition", false);
            String optResult = invokeReplaceGroupStart(writer, opt);
            assertTrue(optResult.contains("opt"));

            SequenceGroup loop = createMockGroup("loop", "10 times", false);
            String loopResult = invokeReplaceGroupStart(writer, loop);
            assertTrue(loopResult.contains("loop") && loopResult.contains("10 times"));

            SequenceGroup named = createMockGroup("My Group", "Description", true);
            when(named.getComment()).thenReturn("Description");
            String namedResult = invokeReplaceGroupStart(writer, named);
            assertTrue(namedResult.contains("group") && namedResult.contains("My Group"));
        }

        @Test
        @DisplayName("includes colors")
        void groupWithColors() throws Exception {
            SequenceGroup group = createMockGroup("alt", "condition", false);
            when(group.getElementColor()).thenReturn("#blue");
            when(group.getBackColor()).thenReturn("#lightblue");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceGroupStart(writer, group);

            assertTrue(result.contains("#blue") && result.contains("#lightblue"));
        }

        private String invokeReplaceGroupStart(SequenceWriter writer, SequenceGroup group) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceGroupStart", SequenceGroup.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, group);
        }
    }

    @Nested
    @DisplayName("replaceGroupElse")
    class ReplaceGroupElseTests {

        @Test
        @DisplayName("generates else with label")
        void elseWithLabel() throws Exception {
            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceGroupElse(writer, "condition2", "else condition1");

            assertTrue(result.contains("else"));
            assertTrue(result.contains("condition2"));
        }

        @Test
        @DisplayName("generates else without label")
        void elseWithoutLabel() throws Exception {
            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceGroupElse(writer, "", "else");

            assertEquals("else", result.trim());
        }

        private String invokeReplaceGroupElse(SequenceWriter writer, String label, String source) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceGroupElse", String.class, String.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, label, source);
        }
    }

    @Nested
    @DisplayName("replaceEnglober")
    class ReplaceEngloberTests {

        @Test
        @DisplayName("generates box")
        void boxWithLabel() throws Exception {
            SequenceEnglober englober = mock(SequenceEnglober.class);
            when(englober.getRawSourceText()).thenReturn("box \"Frontend\"");
            when(englober.getLabel()).thenReturn("Frontend");
            when(englober.getColor()).thenReturn("#CCCCCC");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceEnglober(writer, englober);

            assertTrue(result.contains("box \"Frontend\""));
        }

        @Test
        @DisplayName("generates box with color")
        void boxWithColor() throws Exception {
            SequenceEnglober englober = mock(SequenceEnglober.class);
            when(englober.getRawSourceText()).thenReturn("box \"Backend\" #LightGreen");
            when(englober.getLabel()).thenReturn("Backend");
            when(englober.getColor()).thenReturn("#LightGreen");

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceEnglober(writer, englober);

            assertTrue(result.contains("box"));
            assertTrue(result.contains("#LightGreen"));
        }

        private String invokeReplaceEnglober(SequenceWriter writer, SequenceEnglober englober) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod("replaceEnglober", SequenceEnglober.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, englober);
        }
    }

    @Nested
    @DisplayName("replacePageDetails")
    class ReplacePageDetailsTests {

        @Test
        @DisplayName("generates single line page detail")
        void singleLinePageDetail() throws Exception {
            String content = puml("title Old Title", "participant A");
            Path file = createTempPumlFile(content);

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            List<String> result = invokeReplacePageDetails(writer, "title", "New Title", 1, 1);

            assertEquals(1, result.size());
            assertTrue(result.get(0).contains("title"));
            assertTrue(result.get(0).contains("New Title"));
        }

        @Test
        @DisplayName("generates multiline page detail")
        void multilinePageDetail() throws Exception {
            String content = puml("title", "Line 1", "Line 2", "end title", "participant A");
            Path file = createTempPumlFile(content);

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            List<String> result = invokeReplacePageDetails(writer, "title", "Line 1<br>Line 2", 1, 4);

            assertTrue(result.size() >= 3);
            assertTrue(result.getFirst().contains("title"));
            assertTrue(result.getLast().contains("end title"));
        }

        private List<String> invokeReplacePageDetails(SequenceWriter writer, String keyword,
                                                      String content, int startLine, int endLine) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod(
                    "replacePageDetails", String.class, String.class, int.class, int.class);
            method.setAccessible(true);

            return (List<String>) method.invoke(writer, keyword, content, startLine, endLine);
        }
    }

    @Nested
    @DisplayName("Note Replacement")
    class NoteReplacementTests {

        @Test
        @DisplayName("generates single line note")
        void singleLineNote() throws Exception {
            SequenceMessage message = createMockMessage("edge:note", "");
            SequenceNode fromNode = createSimpleMockNode("Alice", "PARTICIPANT");
            when(message.getFrom()).thenReturn(fromNode);
            when(message.getType()).thenReturn("edge:note");

            SequenceNote note = mock(SequenceNote.class);
            when(note.getRawSourceText()).thenReturn("note right of Alice : Comment");
            when(note.getLabel()).thenReturn("Comment");
            when(note.getPosition()).thenReturn("RIGHT");
            when(note.getShape()).thenReturn("NORMAL");
            when(note.getBackground()).thenReturn("#FFFFE0");
            when(note.isParalell()).thenReturn(false);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceSingleLineNote(writer, message, note);

            assertTrue(result.contains("note"));
            assertTrue(result.contains("Comment"));
        }

        @Test
        @DisplayName("generates hexagonal note")
        void hexagonalNote() throws Exception {
            SequenceMessage message = createMockMessage("edge:note", "");
            SequenceNode fromNode = createSimpleMockNode("Alice", "PARTICIPANT");
            when(message.getFrom()).thenReturn(fromNode);
            when(message.getType()).thenReturn("edge:note");

            SequenceNote note = mock(SequenceNote.class);
            when(note.getRawSourceText()).thenReturn("hnote over Alice : Hex");
            when(note.getLabel()).thenReturn("Hex");
            when(note.getPosition()).thenReturn("OVER");
            when(note.getShape()).thenReturn("HEXAGONAL");
            when(note.getBackground()).thenReturn("#FFFFE0");
            when(note.isParalell()).thenReturn(false);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceSingleLineNote(writer, message, note);

            assertTrue(result.contains("hnote"));
        }

        @Test
        @DisplayName("generates rectangular note")
        void rectangularNote() throws Exception {
            SequenceMessage message = createMockMessage("edge:note", "");
            SequenceNode fromNode = createSimpleMockNode("Alice", "PARTICIPANT");
            when(message.getFrom()).thenReturn(fromNode);
            when(message.getType()).thenReturn("edge:note");

            SequenceNote note = mock(SequenceNote.class);
            when(note.getRawSourceText()).thenReturn("rnote over Alice : Rect");
            when(note.getLabel()).thenReturn("Rect");
            when(note.getPosition()).thenReturn("OVER");
            when(note.getShape()).thenReturn("BOX");
            when(note.getBackground()).thenReturn("#FFFFE0");
            when(note.isParalell()).thenReturn(false);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceSingleLineNote(writer, message, note);

            assertTrue(result.contains("rnote"));
        }

        @Test
        @DisplayName("generates parallel note")
        void parallelNote() throws Exception {
            SequenceMessage message = createMockMessage("edge:note", "");
            SequenceNode fromNode = createSimpleMockNode("Alice", "PARTICIPANT");
            when(message.getFrom()).thenReturn(fromNode);
            when(message.getType()).thenReturn("edge:note");

            SequenceNote note = mock(SequenceNote.class);
            when(note.getRawSourceText()).thenReturn("/ note right : Parallel");
            when(note.getLabel()).thenReturn("Parallel");
            when(note.getPosition()).thenReturn("RIGHT");
            when(note.getShape()).thenReturn("NORMAL");
            when(note.getBackground()).thenReturn("#FFFFE0");
            when(note.isParalell()).thenReturn(true);

            SequenceWriter writer = new SequenceWriter(mockModel, testFile.toUri().toString());
            String result = invokeReplaceSingleLineNote(writer, message, note);

            assertTrue(result.contains("/"));
        }

        private String invokeReplaceSingleLineNote(SequenceWriter writer,
                                                   SequenceMessage message, SequenceNote note) throws Exception {
            Method method = SequenceWriter.class.getDeclaredMethod(
                    "replaceSingleLineNote", SequenceMessage.class, SequenceNote.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, message, note);
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class RealModelIntegrationTests {

        @Test
        @DisplayName("writes and preserves content")
        void simpleParticipant() throws IOException {
            String content = loadResource("participants/simple-participant.puml");
            Path file = createTempPumlFile(content);

            createMapper("participants/simple-participant.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("participant"));
        }

        @Test
        @DisplayName("preserves alias syntax")
        void withAliases() throws IOException {
            String content = loadResource("participants/with-aliases.puml");
            Path file = createTempPumlFile(content);

            createMapper("participants/with-aliases.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains(" as "));
        }

        @Test
        @DisplayName("preserves message arrows")
        void basicMessages() throws IOException {
            String content = loadResource("messages/basic-messages.puml");
            Path file = createTempPumlFile(content);

            createMapper("messages/basic-messages.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("->") || result.contains("-->"));
        }

        @Test
        @DisplayName("preserves alt/else/end structure")
        void altFragment() throws IOException {
            String content = loadResource("fragments/alt-fragment.puml");
            Path file = createTempPumlFile(content);

            createMapper("fragments/alt-fragment.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("alt"));
            assertTrue(result.contains("else"));
            assertTrue(result.contains("end"));
        }

        @Test
        @DisplayName("preserves activate/deactivate")
        void basicActivation() throws IOException {
            String content = loadResource("activation/basic-activation.puml");
            Path file = createTempPumlFile(content);

            createMapper("activation/basic-activation.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("activate"));
            assertTrue(result.contains("deactivate"));
        }

        @Test
        @DisplayName("preserves notes")
        void basicNotes() throws IOException {
            String content = loadResource("notes/basic-notes.puml");
            Path file = createTempPumlFile(content);

            createMapper("notes/basic-notes.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("note"));
        }

        @Test
        @DisplayName("preserves box structure")
        void basicBox() throws IOException {
            String content = loadResource("boxes/basic-box.puml");
            Path file = createTempPumlFile(content);

            createMapper("boxes/basic-box.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("box"));
            assertTrue(result.contains("end box"));
        }

        @Test
        @DisplayName("preserves complex diagram")
        void fullDiagram() throws IOException {
            String original = loadResource("complex/full-diagram.puml");
            Path file = createTempPumlFile(original);

            createMapper("complex/full-diagram.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertEquals(normalizeContent(original), normalizeContent(result));
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarioTests {

        @Test
        @DisplayName("handles multiple page detail modifications")
        void multipleModifications() throws IOException {
            String content = puml(
                    "title Original",
                    "header Original",
                    "footer Original",
                    "participant Foo"
            );
            Path file = createTempPumlFile(content);

            mockModel.titleModified = true;
            mockModel.title = "New Title";
            mockModel.titleLineStart = 1;
            mockModel.titleLineEnd = 1;

            mockModel.headerModified = true;
            mockModel.header = "New Header";
            mockModel.headerLineStart = 2;
            mockModel.headerLineEnd = 2;

            mockModel.footerModified = true;
            mockModel.footer = "New Footer";
            mockModel.footerLineStart = 3;
            mockModel.footerLineEnd = 3;

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertAll(
                    () -> assertTrue(result.contains("title New Title")),
                    () -> assertTrue(result.contains("header New Header")),
                    () -> assertTrue(result.contains("footer New Footer"))
            );
        }

        @Test
        @DisplayName("preserves comments in file")
        void preservesComments() throws IOException {
            String content = loadResource("edge-cases/with-comments.puml");
            Path file = createTempPumlFile(content);

            createMapper("edge-cases/with-comments.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("'"));
        }

        @Test
        @DisplayName("handles empty diagram")
        void handlesEmptyDiagram() throws IOException {
            String content = loadResource("edge-cases/empty-diagram.puml");
            Path file = createTempPumlFile(content);

            createMapper("edge-cases/empty-diagram.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("@startuml") && result.contains("@enduml"));
        }

        @Test
        @DisplayName("handles nested fragments")
        void handlesNestedFragments() throws IOException {
            String content = loadResource("complex/nested-fragments.puml");
            Path file = createTempPumlFile(content);

            createMapper("complex/nested-fragments.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("loop"));
            assertTrue(result.contains("alt"));
            assertTrue(result.contains("end"));
        }

        @Test
        @DisplayName("handles design patterns diagram")
        void handlesDesignPatterns() throws IOException {
            String content = loadResource("complex/design-patterns.puml");
            Path file = createTempPumlFile(content);

            createMapper("complex/design-patterns.puml");

            SequenceWriter writer = new SequenceWriter(model, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("participant"));
            assertTrue(result.contains("activate"));
        }

        @Test
        @DisplayName("handles line deletion")
        void handlesLineDeletion() throws IOException {
            String content = puml(
                    "participant A",
                    "participant B",
                    "A -> B : message"
            );
            Path file = createTempPumlFile(content);

            when(mockModel.getLinesToDelete()).thenReturn(List.of(new int[]{2, 2}));

            SequenceWriter writer = new SequenceWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertFalse(result.contains("participant B"));
        }
    }

    private SequenceNode createMockNode(String originalName, String newName, String type, int line) {
        SequenceNode node = mock(SequenceNode.class);
        when(node.getOriginalName()).thenReturn(originalName);
        when(node.getName()).thenReturn(newName);
        when(node.getType()).thenReturn(type);
        when(node.isModified()).thenReturn(true);
        when(node.hasLine()).thenReturn(true);
        when(node.getSourceLineStart()).thenReturn(line);
        when(node.getSourceLineEnd()).thenReturn(line);
        when(node.getOrder()).thenReturn(0);
        when(node.getBackground()).thenReturn("#5d4949");
        when(node.getLifeEvents()).thenReturn(new ArrayList<>());

        return node;
    }

    private SequenceNode createSimpleMockNode(String name, String type) {
        SequenceNode node = mock(SequenceNode.class);
        when(node.getName()).thenReturn(name);
        when(node.getType()).thenReturn(type);
        when(node.getOrder()).thenReturn(0);
        when(node.getBackground()).thenReturn("#5d4949");
        when(node.getId()).thenReturn(name.toLowerCase());
        when(node.getSourceLineStart()).thenReturn(0);
        when(node.getLifeEvents()).thenReturn(new ArrayList<>());

        return node;
    }

    private SequenceMessage createMockMessage(String type, String text) {
        SequenceMessage message = mock(SequenceMessage.class);
        when(message.getType()).thenReturn(type);
        when(message.getMessage()).thenReturn(text);
        when(message.isModified()).thenReturn(true);
        when(message.hasLine()).thenReturn(true);
        when(message.getNotes()).thenReturn(new ArrayList<>());
        when(message.isParallel()).thenReturn(false);
        when(message.isAnchorStart()).thenReturn(false);
        when(message.isAnchorEnd()).thenReturn(false);
        when(message.isShort()).thenReturn(false);

        return message;
    }

    private SequenceMessage createMockMessageWithArrow(boolean dotted, String endHead, String startHead) {
        SequenceMessage message = mock(SequenceMessage.class);
        when(message.isDotted()).thenReturn(dotted);
        when(message.getEndHead()).thenReturn(endHead);
        when(message.getStartHead()).thenReturn(startHead);
        when(message.getStartPart()).thenReturn("full");
        when(message.getEndPart()).thenReturn("full");
        when(message.getStartDecor()).thenReturn("none");
        when(message.getEndDecor()).thenReturn("none");
        when(message.getColor()).thenReturn("black");

        return message;
    }

    private SequenceGroup createMockGroup(String label, String comment, boolean isGroup) {
        SequenceGroup group = mock(SequenceGroup.class);
        when(group.getLabel()).thenReturn(label);
        when(group.getComment()).thenReturn(comment);
        when(group.isGroup()).thenReturn(isGroup);
        when(group.getRawSourceText()).thenReturn(label + " " + comment);
        when(group.getElementColor()).thenReturn("grey");
        when(group.getBackColor()).thenReturn("none");

        return group;
    }
}