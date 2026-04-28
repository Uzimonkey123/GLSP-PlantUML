/*
 * File: ClassWriterTest.java
 * Author: Norman Babiak
 * Description: Tests for class writer, writing back into file the new version of diagram
 * Date: 28.4.2026
 */

package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.parser.ClassModelParser;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ClassWriter Tests")
class ClassWriterTest extends ClassDiagramTestBase {
    @Mock private ClassModel mockModel;
    @Mock private ClassLineMapper mockLineMapper;
    private AutoCloseable mocks;
    private Path testFile;
    private final ClassModelParser parser = new ClassModelParser();

    @BeforeEach
    void setup() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        testFile = createTempPumlFile(puml("class Foo"));
        when(mockModel.getLineMapper()).thenReturn(mockLineMapper);
        when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());
        mockModel.entities = new ArrayList<>();
        mockModel.links = new ArrayList<>();
        mockModel.packages = new ArrayList<>();
        mockModel.notes = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    @Test
    @DisplayName("writes file unchanged when no modifications")
    void noModifications() throws IOException {
        String content = puml("class Foo");
        Path file = createTempPumlFile(content);

        new ClassWriter(mockModel, file.toUri().toString()).write();

        assertEquals(normalizeContent(content), normalizeContent(Files.readString(file)));
    }

    @Test
    @DisplayName("renames entity in file")
    void renameEntity() throws IOException {
        Path file = createTempPumlFile(puml("class Foo"));
        ClassEntity entity = mock(ClassEntity.class);
        when(entity.getOriginalName()).thenReturn("Foo");
        when(entity.getName()).thenReturn("Bar");
        when(entity.getType()).thenReturn("CLASS");
        when(entity.isModified()).thenReturn(true);
        when(entity.hasLine()).thenReturn(true);
        when(entity.getSourceLineStart()).thenReturn(1);
        when(entity.getSourceLineEnd()).thenReturn(1);
        when(entity.getAlias()).thenReturn(null);
        when(entity.getVisibility()).thenReturn(null);
        when(entity.getGeneric()).thenReturn("");
        when(entity.isStereotype()).thenReturn(false);
        when(entity.getExplicitBackground()).thenReturn(null);
        when(entity.getRawSourceText()).thenReturn("class Foo");
        when(entity.getRawBody()).thenReturn(new ArrayList<>());
        mockModel.entities = List.of(entity);

        ClassLineMapper.LineInfo lineInfo = new ClassLineMapper.LineInfo(1, "class Foo", ClassLineMapper.LineType.ENTITY_DECLARATION);
        when(mockLineMapper.getLineInfos()).thenReturn(List.of(lineInfo));
        when(mockLineMapper.getLineInfo(1)).thenReturn(lineInfo);

        new ClassWriter(mockModel, file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("Bar"));
    }

    @Test
    @DisplayName("updates title, header, and footer")
    void updatePageDetails() throws IOException {
        Path file = createTempPumlFile(puml("title Original", "header Original", "footer Original", "class Foo"));
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

        new ClassWriter(mockModel, file.toUri().toString()).write();

        String result = Files.readString(file);
        assertAll(
                () -> assertTrue(result.contains("title New Title")),
                () -> assertTrue(result.contains("header New Header")),
                () -> assertTrue(result.contains("footer New Footer"))
        );
    }

    @Test
    @DisplayName("throws IOException for non-existent file")
    void nonExistentFile() {
        assertThrows(IOException.class, () -> new ClassWriter(mockModel, tempDir.resolve("nope.puml").toUri().toString()));
    }

    @Test
    @DisplayName("getNewToken prefers alias over name")
    void getNewToken() throws IOException {
        ClassEntity entity = mock(ClassEntity.class);
        when(entity.getName()).thenReturn("MyName");
        WriterContext context = new WriterContext(mockModel, testFile.toFile());

        when(entity.getAlias()).thenReturn("MyAlias");
        assertEquals("MyAlias", context.getNewToken(entity));

        when(entity.getAlias()).thenReturn(null);
        assertEquals("MyName", context.getNewToken(entity));
    }

    @Test
    @DisplayName("getEntityToken quotes names with spaces")
    void getEntityToken() throws IOException {
        ClassEntity entity = mock(ClassEntity.class);
        when(entity.getAlias()).thenReturn(null);
        WriterContext context = new WriterContext(mockModel, testFile.toFile());

        when(entity.getName()).thenReturn("My Class");
        assertEquals("\"My Class\"", context.getEntityToken(entity));

        when(entity.getName()).thenReturn("MyClass");
        assertEquals("MyClass", context.getEntityToken(entity));
    }

    @Test
    @DisplayName("detectSeparator finds custom separator")
    void detectSeparator() throws IOException {
        ClassLineMapper.LineInfo lineInfo = new ClassLineMapper.LineInfo(0, "set separator ::", ClassLineMapper.LineType.UNKNOWN);
        when(mockLineMapper.getLineInfos()).thenReturn(List.of(lineInfo));

        assertEquals("::", new WriterContext(mockModel, testFile.toFile()).detectSeparator());
    }

    @Test
    @DisplayName("integration: preserves simple-class content")
    void preserveSimpleClass() throws IOException {
        String content = loadResource("entities/simple-class.puml");
        Path file = createTempPumlFile(content);

        new ClassWriter(parser.parse(file.toFile()), file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("class User"));
    }

    @Test
    @DisplayName("integration: preserves full-diagram")
    void preserveFullDiagram() throws IOException {
        String original = loadResource("complex/full-diagram.puml");
        Path file = createTempPumlFile(original);

        new ClassWriter(parser.parse(file.toFile()), file.toUri().toString()).write();

        assertEquals(normalizeContent(original), normalizeContent(Files.readString(file)));
    }

    @Test
    @DisplayName("integration: preserves comments in file")
    void preserveComments() throws IOException {
        String content = loadResource("edge-cases/with-comments.puml");
        Path file = createTempPumlFile(content);

        new ClassWriter(parser.parse(file.toFile()), file.toUri().toString()).write();

        assertTrue(Files.readString(file).contains("'"));
    }
}
