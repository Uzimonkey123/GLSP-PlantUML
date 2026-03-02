package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.parser.ClassModelParser;
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

@DisplayName("ClassWriter Tests")
class ClassWriterTest extends ClassDiagramTestBase {
    @Mock private ClassModel mockModel;
    @Mock private ClassLineMapper mockLineMapper;

    private AutoCloseable mocks;
    private Path testFile;

    @BeforeEach
    void setUpMocks() throws IOException {
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
    void tearDownMocks() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("initializes with mock model")
        void initializeWithMock() throws IOException {
            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertNotNull(writer);
        }

        @Test
        @DisplayName("throws IOException for non-existent file")
        void throwsForNonExistent() {
            Path nonExistent = tempDir.resolve("nonexistent.puml");
            assertThrows(IOException.class, () -> new ClassWriter(mockModel, nonExistent.toUri().toString()));
        }

        @Test
        @DisplayName("initializes with real parsed model")
        void initializeWithRealModel() throws IOException {
            createMapper("entities/simple-class.puml");
            Path file = createTempPumlFile(loadResource("entities/simple-class.puml"));

            ClassWriter writer = new ClassWriter(model, file.toUri().toString());
            assertNotNull(writer);
        }
    }

    @Nested
    @DisplayName("write() Integration")
    class WriteIntegrationTests {

        @Test
        @DisplayName("writes file with no modifications")
        void writeNoModifications() throws IOException {
            String content = puml("class Foo");
            Path file = createTempPumlFile(content);

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertEquals(normalizeContent(content), normalizeContent(result));
        }

        @Test
        @DisplayName("renames entity in file")
        void renameEntity() throws IOException {
            String content = puml("class Foo");
            Path file = createTempPumlFile(content);

            ClassEntity entity = createMockEntity("Foo", "Bar", "CLASS", 1);
            when(entity.getRawSourceText()).thenReturn("class Foo");
            when(entity.getRawBody()).thenReturn(new ArrayList<>());
            mockModel.entities = List.of(entity);

            ClassLineMapper.LineInfo lineInfo = new ClassLineMapper.LineInfo(
                    1, "class Foo", ClassLineMapper.LineType.ENTITY_DECLARATION
            );
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(lineInfo));
            when(mockLineMapper.getLineInfo(1)).thenReturn(lineInfo);

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("Bar"));
        }

        @Test
        @DisplayName("updates page title")
        void updateTitle() throws IOException {
            String content = puml("title Old Title", "class Foo");
            Path file = createTempPumlFile(content);

            mockModel.titleModified = true;
            mockModel.title = "New Title";
            mockModel.titleLineStart = 1;
            mockModel.titleLineEnd = 1;

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("title New Title"));
        }

        @Test
        @DisplayName("updates page header")
        void updateHeader() throws IOException {
            String content = puml("header Old Header", "class Foo");
            Path file = createTempPumlFile(content);

            mockModel.headerModified = true;
            mockModel.header = "New Header";
            mockModel.headerLineStart = 1;
            mockModel.headerLineEnd = 1;

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("header New Header"));
        }

        @Test
        @DisplayName("updates page footer")
        void updateFooter() throws IOException {
            String content = puml("footer Old Footer", "class Foo");
            Path file = createTempPumlFile(content);

            mockModel.footerModified = true;
            mockModel.footer = "New Footer";
            mockModel.footerLineStart = 1;
            mockModel.footerLineEnd = 1;

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("footer New Footer"));
        }
    }

    @Nested
    @DisplayName("getNewToken")
    class GetNewTokenTests {

        @Test
        @DisplayName("prefers alias but falls back to name when missing")
        void prefersAliasOrFallsBackToName() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getName()).thenReturn("MyName");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            when(entity.getAlias()).thenReturn("MyAlias");
            assertEquals("MyAlias", invokeGetNewToken(writer, entity));

            when(entity.getAlias()).thenReturn(null);
            assertEquals("MyName", invokeGetNewToken(writer, entity));

            when(entity.getAlias()).thenReturn("");
            assertEquals("MyName", invokeGetNewToken(writer, entity));
        }

        private String invokeGetNewToken(ClassWriter writer, ClassEntity entity) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("getNewToken", ClassEntity.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, entity);
        }
    }

    @Nested
    @DisplayName("getEntityToken")
    class GetEntityTokenTests {

        @Test
        @DisplayName("returns alias when present")
        void returnsAlias() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getAlias()).thenReturn("MyAlias");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            assertEquals("MyAlias", invokeGetEntityToken(writer, entity));
        }

        @Test
        @DisplayName("handles quoting rules for entity names")
        void handlesQuotingRules() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getAlias()).thenReturn(null);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            when(entity.getName()).thenReturn("My Class");
            assertEquals("\"My Class\"", invokeGetEntityToken(writer, entity));

            when(entity.getName()).thenReturn("My-Class");
            assertEquals("\"My-Class\"", invokeGetEntityToken(writer, entity));

            when(entity.getName()).thenReturn("MyClass");
            assertEquals("MyClass", invokeGetEntityToken(writer, entity));
        }

        private String invokeGetEntityToken(ClassWriter writer, ClassEntity entity) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("getEntityToken", ClassEntity.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, entity);
        }
    }

    @Nested
    @DisplayName("detectSeparator")
    class DetectSeparatorTests {

        @Test
        @DisplayName("detects separator configuration correctly")
        void detectsSeparatorConfiguration() throws Exception {
            ClassWriter writer;

            when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());
            writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals(".", invokeDetectSeparator(writer));

            ClassLineMapper.LineInfo custom = new ClassLineMapper.LineInfo(
                    0, "set separator ::", ClassLineMapper.LineType.UNKNOWN
            );
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(custom));
            writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("::", invokeDetectSeparator(writer));

            ClassLineMapper.LineInfo none = new ClassLineMapper.LineInfo(
                    0, "set separator none", ClassLineMapper.LineType.UNKNOWN
            );
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(none));
            writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals(".", invokeDetectSeparator(writer));
        }

        private String invokeDetectSeparator(ClassWriter writer) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("detectSeparator");
            method.setAccessible(true);

            return (String) method.invoke(writer);
        }
    }

    @Nested
    @DisplayName("buildDeclarationHeader")
    class BuildDeclarationHeaderTests {

        @Test
        @DisplayName("builds declaration header with all relevant attributes")
        void buildsCompleteDeclarationHeader() throws Exception {
            ClassEntity entity = createSimpleMockEntity("Full Name", "CLASS");

            when(entity.getVisibility()).thenReturn("private");
            when(entity.getAlias()).thenReturn("Alias");
            when(entity.getGeneric()).thenReturn("T");
            when(entity.isStereotype()).thenReturn(true);
            when(entity.getStereotypeChar()).thenReturn('S');
            when(entity.getStereotypeColor()).thenReturn("blue");
            when(entity.getStereotypeName()).thenReturn("service");
            when(entity.getExplicitBackground()).thenReturn("#lightblue");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);

            assertTrue(result.startsWith("-"));
            assertTrue(result.contains("as Alias"));
            assertTrue(result.contains("<T>"));
            assertTrue(result.contains("<<") && result.contains(">>"));
            assertTrue(result.contains("#lightblue"));
        }

        private String invokeBuildDeclarationHeader(ClassWriter writer, ClassEntity entity) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("buildDeclarationHeader", ClassEntity.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, entity);
        }
    }

    @Nested
    @DisplayName("buildMemberLine")
    class BuildMemberLineTests {

        @Test
        @DisplayName("builds member line respecting visibility")
        void buildsMemberLineWithAndWithoutVisibility() throws Exception {
            EntityMethod member = mock(EntityMethod.class);
            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            when(member.getVisibilityChar()).thenReturn("public");
            when(member.getMethodName()).thenReturn("myMethod()");
            assertEquals("+ myMethod()", invokeBuildMemberLine(writer, member));

            when(member.getVisibilityChar()).thenReturn(null);
            when(member.getMethodName()).thenReturn("myField");
            assertEquals("myField", invokeBuildMemberLine(writer, member));
        }

        private String invokeBuildMemberLine(ClassWriter writer, EntityMethod member) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("buildMemberLine", EntityMethod.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, member);
        }
    }

    @Nested
    @DisplayName("Real Model Integration")
    class RealModelIntegrationTests {
        private final ClassModelParser parser = new ClassModelParser();

        @Test
        @DisplayName("simple-class: writes and preserves content")
        void simpleClass() throws IOException {
            String content = loadResource("entities/simple-class.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("class User"));
        }

        @Test
        @DisplayName("with-aliases: preserves alias syntax")
        void withAliases() throws IOException {
            String content = loadResource("entities/with-aliases.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains(" as "));
        }

        @Test
        @DisplayName("visibility-modifiers: preserves +/-/#/~")
        void visibilityModifiers() throws IOException {
            String content = loadResource("entities/visibility-modifiers.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("+") || result.contains("-") ||
                    result.contains("#") || result.contains("~"));
        }

        @Test
        @DisplayName("nested-packages: preserves package structure")
        void nestedPackages() throws IOException {
            String content = loadResource("packages/nested-packages.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("package"));
        }

        @Test
        @DisplayName("basic-arrows: preserves relationships")
        void basicArrows() throws IOException {
            String content = loadResource("relationships/basic-arrows.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("-->") || result.contains("--"));
        }

        @Test
        @DisplayName("basic-notes: preserves notes")
        void basicNotes() throws IOException {
            String content = loadResource("notes/basic-notes.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("note"));
        }

        @Test
        @DisplayName("full-diagram: preserves complex diagram")
        void fullDiagram() throws IOException {
            String original = loadResource("complex/full-diagram.puml");
            Path file = createTempPumlFile(original);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertEquals(normalizeContent(original), normalizeContent(result));
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarioTests {
        private final ClassModelParser parser = new ClassModelParser();

        @Test
        @DisplayName("handles multiple page detail modifications")
        void multipleModifications() throws IOException {
            String content = puml(
                    "title Original",
                    "header Original",
                    "footer Original",
                    "class Foo"
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

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
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
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("'"));
        }

        @Test
        @DisplayName("handles empty diagram")
        void handlesEmptyDiagram() throws IOException {
            String content = loadResource("edge-cases/empty-diagram.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("@startuml") && result.contains("@enduml"));
        }

        @Test
        @DisplayName("handles inheritance hierarchy")
        void handlesInheritanceHierarchy() throws IOException {
            String content = loadResource("relationships/inheritance-hierarchy.puml");
            Path file = createTempPumlFile(content);
            ClassModel parsedModel = parser.parse(file.toFile());

            ClassWriter writer = new ClassWriter(parsedModel, file.toUri().toString());
            writer.write();

            String result = Files.readString(file);
            assertTrue(result.contains("|>") || result.contains("extends"));
        }
    }

    private ClassEntity createMockEntity(String originalName, String newName, String type, int line) {
        ClassEntity entity = mock(ClassEntity.class);
        when(entity.getOriginalName()).thenReturn(originalName);
        when(entity.getName()).thenReturn(newName);
        when(entity.getType()).thenReturn(type);
        when(entity.isModified()).thenReturn(true);
        when(entity.hasLine()).thenReturn(true);
        when(entity.getSourceLineStart()).thenReturn(line);
        when(entity.getSourceLineEnd()).thenReturn(line);
        when(entity.getAlias()).thenReturn(null);
        when(entity.getVisibility()).thenReturn(null);
        when(entity.getGeneric()).thenReturn("");
        when(entity.isStereotype()).thenReturn(false);
        when(entity.getExplicitBackground()).thenReturn(null);

        return entity;
    }

    private ClassEntity createSimpleMockEntity(String name, String type) {
        ClassEntity entity = mock(ClassEntity.class);
        when(entity.getType()).thenReturn(type);
        when(entity.getName()).thenReturn(name);
        when(entity.getAlias()).thenReturn(null);
        when(entity.getVisibility()).thenReturn(null);
        when(entity.getGeneric()).thenReturn("");
        when(entity.isStereotype()).thenReturn(false);
        when(entity.getRawSourceText()).thenReturn("");
        when(entity.getExplicitBackground()).thenReturn(null);

        return entity;
    }
}