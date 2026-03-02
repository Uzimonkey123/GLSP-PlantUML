package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;

import com.diagrams.ClassDiagram.parser.ClassModelParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        @DisplayName("returns alias when present")
        void returnsAlias() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getAlias()).thenReturn("MyAlias");
            when(entity.getName()).thenReturn("MyName");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("MyAlias", invokeGetNewToken(writer, entity));
        }

        @Test
        @DisplayName("returns name when alias is null or empty")
        void returnsNameWhenNoAlias() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getName()).thenReturn("MyName");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

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
        @DisplayName("quotes name with spaces or special characters")
        void quotesSpecialNames() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getAlias()).thenReturn(null);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            when(entity.getName()).thenReturn("My Class");
            assertEquals("\"My Class\"", invokeGetEntityToken(writer, entity));

            when(entity.getName()).thenReturn("My-Class");
            assertEquals("\"My-Class\"", invokeGetEntityToken(writer, entity));
        }

        @Test
        @DisplayName("does not quote simple name")
        void doesNotQuoteSimple() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getAlias()).thenReturn(null);
            when(entity.getName()).thenReturn("MyClass");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
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
        @DisplayName("returns default separator when none set")
        void returnsDefault() throws Exception {
            when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals(".", invokeDetectSeparator(writer));
        }

        @Test
        @DisplayName("detects custom separator")
        void detectsCustom() throws Exception {
            ClassLineMapper.LineInfo info = new ClassLineMapper.LineInfo(
                    0, "set separator ::", ClassLineMapper.LineType.UNKNOWN
            );
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(info));

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("::", invokeDetectSeparator(writer));
        }

        @Test
        @DisplayName("returns default when separator is 'none'")
        void returnsDefaultForNone() throws Exception {
            ClassLineMapper.LineInfo info = new ClassLineMapper.LineInfo(
                    0, "set separator none", ClassLineMapper.LineType.UNKNOWN
            );
            when(mockLineMapper.getLineInfos()).thenReturn(List.of(info));

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals(".", invokeDetectSeparator(writer));
        }

        private String invokeDetectSeparator(ClassWriter writer) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("detectSeparator");
            method.setAccessible(true);
            return (String) method.invoke(writer);
        }
    }

    @Nested
    @DisplayName("getEffectiveLine")
    class GetEffectiveLineTests {

        @Test
        @DisplayName("returns source line for valid index")
        void returnsSourceLine() throws Exception {
            Path file = createTempPumlFile(puml("class Foo"));

            ClassWriter writer = new ClassWriter(mockModel, file.toUri().toString());
            assertEquals("class Foo", invokeGetEffectiveLine(writer, 1));
        }

        @Test
        @DisplayName("returns empty string for invalid index")
        void returnsEmptyForInvalid() throws Exception {
            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            assertEquals("", invokeGetEffectiveLine(writer, 100));
            assertEquals("", invokeGetEffectiveLine(writer, -1));
        }

        private String invokeGetEffectiveLine(ClassWriter writer, int lineNum) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("getEffectiveLine", int.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, lineNum);
        }
    }

    @Nested
    @DisplayName("hasNameConflict")
    class HasNameConflictTests {

        @Test
        @DisplayName("returns true when another entity has same original name")
        void detectsConflict() throws Exception {
            ClassEntity entity1 = mock(ClassEntity.class);
            ClassEntity entity2 = mock(ClassEntity.class);
            when(entity1.getOriginalName()).thenReturn("SameName");
            when(entity2.getOriginalName()).thenReturn("SameName");
            mockModel.entities = List.of(entity1, entity2);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertTrue(invokeHasNameConflict(writer, entity1));
        }

        @Test
        @DisplayName("returns false when no conflict")
        void noConflict() throws Exception {
            ClassEntity entity1 = mock(ClassEntity.class);
            ClassEntity entity2 = mock(ClassEntity.class);
            when(entity1.getOriginalName()).thenReturn("Name1");
            when(entity2.getOriginalName()).thenReturn("Name2");
            mockModel.entities = List.of(entity1, entity2);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertFalse(invokeHasNameConflict(writer, entity1));
        }

        private boolean invokeHasNameConflict(ClassWriter writer, ClassEntity entity) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("hasNameConflict", ClassEntity.class);
            method.setAccessible(true);

            return (boolean) method.invoke(writer, entity);
        }
    }

    @Nested
    @DisplayName("buildDeclarationHeader")
    class BuildDeclarationHeaderTests {

        @ParameterizedTest
        @CsvSource({
                "CLASS, class",
                "INTERFACE, interface",
                "ENUM, enum",
                "ANNOTATION, annotation",
                "ABSTRACT, abstract",
                "DIAMOND, diamond",
                "CIRCLE, circle"
        })
        @DisplayName("generates correct type keyword")
        void generatesTypeKeyword(String type, String expectedKeyword) throws Exception {
            ClassEntity entity = createSimpleMockEntity("TestEntity", type);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
            assertTrue(result.startsWith(expectedKeyword + " "));
        }

        @Test
        @DisplayName("includes visibility prefix")
        void includesVisibility() throws Exception {
            ClassEntity entity = createSimpleMockEntity("TestClass", "CLASS");
            when(entity.getVisibility()).thenReturn("private");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
            assertTrue(result.startsWith("-"));
        }

        @Test
        @DisplayName("includes alias")
        void includesAlias() throws Exception {
            ClassEntity entity = createSimpleMockEntity("Full Name", "CLASS");
            when(entity.getAlias()).thenReturn("Alias");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
            assertTrue(result.contains("as"));
        }

        @Test
        @DisplayName("includes generic parameter")
        void includesGeneric() throws Exception {
            ClassEntity entity = createSimpleMockEntity("Container", "CLASS");
            when(entity.getGeneric()).thenReturn("T");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
            assertTrue(result.contains("<T>"));
        }

        @Test
        @DisplayName("includes stereotype")
        void includesStereotype() throws Exception {
            ClassEntity entity = createSimpleMockEntity("TestClass", "CLASS");
            when(entity.isStereotype()).thenReturn(true);
            when(entity.getStereotypeChar()).thenReturn('S');
            when(entity.getStereotypeColor()).thenReturn("blue");
            when(entity.getStereotypeName()).thenReturn("service");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
            assertTrue(result.contains("<<") && result.contains(">>"));
        }

        @Test
        @DisplayName("includes background color")
        void includesBackground() throws Exception {
            ClassEntity entity = createSimpleMockEntity("TestClass", "CLASS");
            when(entity.getExplicitBackground()).thenReturn("#lightblue");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            String result = invokeBuildDeclarationHeader(writer, entity);
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
        @DisplayName("builds with visibility")
        void buildsWithVisibility() throws Exception {
            EntityMethod member = mock(EntityMethod.class);
            when(member.getVisibilityChar()).thenReturn("public");
            when(member.getMethodName()).thenReturn("myMethod()");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("+ myMethod()", invokeBuildMemberLine(writer, member));
        }

        @Test
        @DisplayName("builds without visibility")
        void buildsWithoutVisibility() throws Exception {
            EntityMethod member = mock(EntityMethod.class);
            when(member.getVisibilityChar()).thenReturn(null);
            when(member.getMethodName()).thenReturn("myField");

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("myField", invokeBuildMemberLine(writer, member));
        }

        private String invokeBuildMemberLine(ClassWriter writer, EntityMethod member) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("buildMemberLine", EntityMethod.class);
            method.setAccessible(true);
            return (String) method.invoke(writer, member);
        }
    }

    @Nested
    @DisplayName("getDepth")
    class GetDepthTests {

        @Test
        @DisplayName("returns correct depth for package hierarchy")
        void returnsCorrectDepth() throws Exception {
            Package root = mock(Package.class);
            Package child = mock(Package.class);
            Package grandchild = mock(Package.class);

            when(root.getParentPackage()).thenReturn(null);
            when(child.getParentPackage()).thenReturn(root);
            when(grandchild.getParentPackage()).thenReturn(child);

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            assertEquals(1, invokeGetDepth(writer, root));
            assertEquals(2, invokeGetDepth(writer, child));
            assertEquals(3, invokeGetDepth(writer, grandchild));
        }

        private int invokeGetDepth(ClassWriter writer, Package pkg) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("getDepth", Package.class);
            method.setAccessible(true);

            return (int) method.invoke(writer, pkg);
        }
    }

    @Nested
    @DisplayName("buildFullPath")
    class BuildFullPathTests {

        @Test
        @DisplayName("builds path for nested packages")
        void buildsNestedPath() throws Exception {
            Package root = mock(Package.class);
            Package child = mock(Package.class);

            when(root.getOriginalName()).thenReturn("com");
            when(root.getName()).thenReturn("com");
            when(root.getParentPackage()).thenReturn(null);

            when(child.getOriginalName()).thenReturn("example");
            when(child.getName()).thenReturn("example");
            when(child.getParentPackage()).thenReturn(root);

            when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertEquals("com.example", invokeBuildFullPath(writer, child, true));
        }

        @Test
        @DisplayName("uses new name when useOriginal is false")
        void usesNewName() throws Exception {
            Package pkg = mock(Package.class);
            when(pkg.getOriginalName()).thenReturn("oldName");
            when(pkg.getName()).thenReturn("newName");
            when(pkg.getParentPackage()).thenReturn(null);

            when(mockLineMapper.getLineInfos()).thenReturn(new ArrayList<>());

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());

            assertEquals("oldName", invokeBuildFullPath(writer, pkg, true));
            assertEquals("newName", invokeBuildFullPath(writer, pkg, false));
        }

        private String invokeBuildFullPath(ClassWriter writer, Package pkg, boolean useOriginal) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("buildFullPath", Package.class, boolean.class);
            method.setAccessible(true);

            return (String) method.invoke(writer, pkg, useOriginal);
        }
    }

    @Nested
    @DisplayName("buildMemberRefMap")
    class BuildMemberRefMapTests {

        @Test
        @DisplayName("returns empty map for entity with no members")
        void emptyForNoMembers() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);
            when(entity.getRawBody()).thenReturn(new ArrayList<>());

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            assertTrue(invokeBuildMemberRefMap(writer, entity).isEmpty());
        }

        @Test
        @DisplayName("builds map for renamed members only")
        void buildsMapForRenamed() throws Exception {
            ClassEntity entity = mock(ClassEntity.class);

            EntityMethod renamed = mock(EntityMethod.class);
            when(renamed.getOriginalName()).thenReturn("oldField");
            when(renamed.getMethodName()).thenReturn("newField");

            EntityMethod unchanged = mock(EntityMethod.class);
            when(unchanged.getOriginalName()).thenReturn("sameField");
            when(unchanged.getMethodName()).thenReturn("sameField");

            when(entity.getRawBody()).thenReturn(List.of(renamed, unchanged));

            ClassWriter writer = new ClassWriter(mockModel, testFile.toUri().toString());
            Map<String, String> result = invokeBuildMemberRefMap(writer, entity);

            assertEquals(1, result.size());
            assertEquals("newField", result.get("oldField"));
        }

        @SuppressWarnings("unchecked")
        private Map<String, String> invokeBuildMemberRefMap(ClassWriter writer, ClassEntity entity) throws Exception {
            Method method = ClassWriter.class.getDeclaredMethod("buildMemberRefMap", ClassEntity.class);
            method.setAccessible(true);

            return (Map<String, String>) method.invoke(writer, entity);
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