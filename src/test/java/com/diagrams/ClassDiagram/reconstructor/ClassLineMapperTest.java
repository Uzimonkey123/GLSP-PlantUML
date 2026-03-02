package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper.LineInfo;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper.LineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassLineMapper Tests")
class ClassLineMapperTest extends ClassDiagramTestBase {
    private long countLineType(LineType type) {
        return lineMapper.getLineInfos().stream()
                .filter(info -> info.type == type)
                .count();
    }

    private boolean hasLineType(LineType type) {
        return countLineType(type) > 0;
    }

    private boolean hasLineMatching(LineType type, String contains) {
        return lineMapper.getLineInfos().stream()
                .anyMatch(info -> info.type == type && info.trimmedText.contains(contains));
    }

    private List<LineInfo> getLinesOfType(LineType type) {
        return lineMapper.getLineInfos().stream()
                .filter(info -> info.type == type)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("Entity Declaration Detection")
    class EntityDeclarationTests {

        @Test
        @DisplayName("simple-class: detects class with body")
        void detectSimpleClass() {
            createMapper("entities/simple-class.puml");

            assertLineType(0, LineType.START_UML);
            assertLineType(1, LineType.ENTITY_DECLARATION);
            assertTrue(hasLineType(LineType.MEMBER));
            assertTrue(hasLineType(LineType.BLOCK_END));
            assertLineType(7, LineType.END_UML);
        }

        @Test
        @DisplayName("multiple-types: detects class, interface, enum, abstract, annotation")
        void detectMultipleTypes() {
            createMapper("entities/multiple-types.puml");

            List<LineInfo> declarations = getLinesOfType(LineType.ENTITY_DECLARATION);
            declarations.addAll(getLinesOfType(LineType.ENTITY_INLINE));

            Set<String> foundKeywords = declarations.stream()
                    .map(info -> info.trimmedText.split(" ")[0].toLowerCase())
                    .collect(Collectors.toSet());

            assertAll(
                    () -> assertTrue(foundKeywords.contains("class")),
                    () -> assertTrue(foundKeywords.contains("interface")),
                    () -> assertTrue(foundKeywords.contains("enum")),
                    () -> assertTrue(foundKeywords.contains("abstract")),
                    () -> assertTrue(foundKeywords.contains("annotation"))
            );
        }

        @Test
        @DisplayName("inline-entities: detects inline declarations")
        void detectInlineEntities() {
            createMapper("entities/inline-entities.puml");

            assertTrue(countLineType(LineType.ENTITY_DECLARATION) >= 2);
        }

        @Test
        @DisplayName("with-aliases: detects 'as' keyword in declarations")
        void detectAliases() {
            createMapper("entities/with-aliases.puml");

            List<LineInfo> declarations = getLinesOfType(LineType.ENTITY_DECLARATION);

            boolean hasAlias = declarations.stream().anyMatch(info -> info.trimmedText.contains(" as "));

            assertTrue(hasAlias);
            assertTrue(declarations.size() >= 2);
        }

        @Test
        @DisplayName("visibility-modifiers: detects members with +/-/#/~")
        void detectVisibilityModifiers() {
            createMapper("entities/visibility-modifiers.puml");

            List<LineInfo> members = getLinesOfType(LineType.MEMBER);

            Set<Character> foundVisibilities = members.stream()
                    .map(info -> info.trimmedText.charAt(0))
                    .filter(c -> c == '+' || c == '-' || c == '#' || c == '~')
                    .collect(Collectors.toSet());

            assertTrue(foundVisibilities.size() >= 3, "Should detect multiple visibility modifiers");
        }

        @Test
        @DisplayName("static-abstract-members: detects {static} and {abstract}")
        void detectStaticAbstractMembers() {
            createMapper("entities/static-abstract-members.puml");

            boolean hasStatic = hasLineMatching(LineType.MEMBER, "{static}");
            boolean hasAbstract = hasLineMatching(LineType.MEMBER, "{abstract}");

            assertTrue(hasStatic || hasAbstract, "Should detect static or abstract members");
        }
    }

    @Nested
    @DisplayName("Relationship Detection")
    class RelationshipTests {

        @Test
        @DisplayName("basic-arrows: detects 5 relationships")
        void detectBasicArrows() {
            createMapper("relationships/basic-arrows.puml");

            assertEquals(5, countLineType(LineType.RELATIONSHIP));
        }

        @Test
        @DisplayName("with-cardinality: detects quoted cardinality")
        void detectCardinality() {
            createMapper("relationships/with-cardinality.puml");

            assertTrue(hasLineMatching(LineType.RELATIONSHIP, "\""));
        }

        @Test
        @DisplayName("with-directions: detects -up->, -down->, etc.")
        void detectDirectionalArrows() {
            createMapper("relationships/with-directions.puml");

            List<LineInfo> relationships = getLinesOfType(LineType.RELATIONSHIP);

            boolean hasDirectional = relationships.stream()
                    .anyMatch(info -> {
                        String text = info.trimmedText.toLowerCase();
                        return text.contains("-up->") || text.contains("-down->") ||
                                text.contains("-left->") || text.contains("-right->") ||
                                text.contains("-u->") || text.contains("-d->") ||
                                text.contains("-l->") || text.contains("-r->");
                    });

            assertTrue(hasDirectional);
        }

        @Test
        @DisplayName("all-arrow-types: detects various arrow styles")
        void detectAllArrowTypes() {
            createMapper("relationships/all-arrow-types.puml");

            assertTrue(countLineType(LineType.RELATIONSHIP) >= 8);
        }
    }

    @Nested
    @DisplayName("Package Detection")
    class PackageTests {

        @Test
        @DisplayName("simple-packages: detects 2 packages")
        void detectSimplePackages() {
            createMapper("packages/simple-packages.puml");

            assertEquals(2, countLineType(LineType.PACKAGE_DECLARATION));
        }

        @Test
        @DisplayName("nested-packages: detects 4+ nested packages")
        void detectNestedPackages() {
            createMapper("packages/nested-packages.puml");

            assertTrue(countLineType(LineType.PACKAGE_DECLARATION) >= 4);
        }

        @Test
        @DisplayName("container-types: detects 7 container declarations")
        void detectContainerTypes() {
            createMapper("packages/container-types.puml");

            assertEquals(7, countLineType(LineType.PACKAGE_DECLARATION));
        }
    }

    @Nested
    @DisplayName("Note Detection")
    class NoteTests {

        @Test
        @DisplayName("basic-notes: detects note, body, and end note")
        void detectBasicNotes() {
            createMapper("notes/basic-notes.puml");

            assertAll(
                    () -> assertTrue(countLineType(LineType.NOTE) >= 2, "Should have notes"),
                    () -> assertTrue(countLineType(LineType.NOTE_BODY) >= 1, "Should have note body"),
                    () -> assertTrue(countLineType(LineType.END_NOTE) >= 1, "Should have end note")
            );
        }

        @Test
        @DisplayName("notes-on-links: detects note on link syntax")
        void detectNotesOnLinks() {
            createMapper("notes/notes-on-links.puml");

            assertTrue(hasLineType(LineType.NOTE));
        }

        @Test
        @DisplayName("notes-with-styling: detects colored notes")
        void detectStyledNotes() {
            createMapper("notes/notes-with-styling.puml");

            boolean hasColoredNote = getLinesOfType(LineType.NOTE).stream().anyMatch(info -> info.trimmedText.contains("#"));
            assertTrue(hasColoredNote);
        }
    }

    @Nested
    @DisplayName("Comment Detection")
    class CommentTests {

        @Test
        @DisplayName("with-comments: detects single-line and multiline comments")
        void detectComments() {
            createMapper("edge-cases/with-comments.puml");

            List<LineInfo> comments = getLinesOfType(LineType.COMMENT);

            boolean hasSingleLine = comments.stream().anyMatch(info -> info.trimmedText.startsWith("'"));
            boolean hasMultiLine = comments.stream().anyMatch(info -> info.trimmedText.startsWith("/'"));

            assertAll(
                    () -> assertTrue(hasSingleLine, "Should detect single-line comments"),
                    () -> assertTrue(hasMultiLine, "Should detect multiline comments")
            );
        }
    }

    @Nested
    @DisplayName("Member Detection")
    class MemberTests {

        @Test
        @DisplayName("simple-class: detects 4 members (2 fields + 2 methods)")
        void detectMembers() {
            createMapper("entities/simple-class.puml");

            assertEquals(4, countLineType(LineType.MEMBER));
        }

        @Test
        @DisplayName("with-separators: detects separator lines")
        void detectSeparators() {
            createMapper("edge-cases/with-separators.puml");

            assertTrue(countLineType(LineType.SEPARATOR) >= 1);
        }
    }

    @Nested
    @DisplayName("Page Details Detection")
    class PageDetailsTests {

        @Test
        @DisplayName("full-diagram: detects title, header, footer")
        void detectPageDetails() {
            createMapper("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(hasLineType(LineType.TITLE), "Should detect title"),
                    () -> assertTrue(hasLineType(LineType.HEADER), "Should detect header"),
                    () -> assertTrue(hasLineType(LineType.FOOTER), "Should detect footer")
            );
        }
    }

    @Nested
    @DisplayName("Direction Detection")
    class DirectionTests {

        @Test
        @DisplayName("left-to-right: detects direction directive and sets model flag")
        void detectLeftToRight() {
            createMapper("edge-cases/left-to-right.puml");

            assertAll(
                    () -> assertTrue(hasLineType(LineType.LEFT_TO_RIGHT)),
                    () -> assertTrue(model.isLeftToRight())
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("empty-diagram: handles empty content")
        void handleEmptyDiagram() {
            createMapper("edge-cases/empty-diagram.puml");

            assertAll(
                    () -> assertTrue(hasLineType(LineType.START_UML)),
                    () -> assertTrue(hasLineType(LineType.END_UML)),
                    () -> assertEquals(0, countLineType(LineType.ENTITY_DECLARATION))
            );
        }

        @Test
        @DisplayName("minimal: handles single class")
        void handleMinimal() {
            createMapper("edge-cases/minimal.puml");

            assertEquals(1, countLineType(LineType.ENTITY_DECLARATION));
        }

        @Test
        @DisplayName("special-characters: handles generics and stereotypes")
        void handleSpecialCharacters() {
            createMapper("edge-cases/special-characters.puml");

            List<LineInfo> entities = getLinesOfType(LineType.ENTITY_DECLARATION);
            entities.addAll(getLinesOfType(LineType.ENTITY_INLINE));

            boolean hasGeneric = entities.stream()
                    .anyMatch(info -> info.trimmedText.contains("<"));
            boolean hasStereotype = entities.stream()
                    .anyMatch(info -> info.trimmedText.contains("<<"));

            assertAll(
                    () -> assertTrue(hasGeneric, "Should handle generic types"),
                    () -> assertTrue(hasStereotype, "Should handle stereotypes")
            );
        }

        @Test
        @DisplayName("handles empty lines correctly")
        void handleEmptyLines() {
            String source = puml("", "class A", "", "class B", "");
            lineMapper = new ClassLineMapper(source, model);

            assertTrue(countLineType(LineType.EMPTY) >= 2);
        }

        @Test
        @DisplayName("no-startuml: handles content without @startuml")
        void handleNoStartUml() {
            createMapper("edge-cases/no-startuml.puml");

            assertFalse(lineMapper.getLineInfos().isEmpty());
        }

        @Test
        @DisplayName("getLineInfo returns correct info for valid line")
        void getLineInfoValid() {
            createMapper("entities/simple-class.puml");

            LineInfo info = lineMapper.getLineInfo(0);

            assertNotNull(info);
            assertEquals(LineType.START_UML, info.type);
        }

        @Test
        @DisplayName("getLineInfo returns null for invalid line number")
        void getLineInfoInvalid() {
            createMapper("edge-cases/minimal.puml");

            assertAll(
                    () -> assertNull(lineMapper.getLineInfo(-1)),
                    () -> assertNull(lineMapper.getLineInfo(1000))
            );
        }
    }

    @Nested
    @DisplayName("Complex Diagram")
    class ComplexDiagramTests {

        @Test
        @DisplayName("full-diagram: detects all line types")
        void detectAllLineTypes() {
            createMapper("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(hasLineType(LineType.START_UML)),
                    () -> assertTrue(hasLineType(LineType.END_UML)),
                    () -> assertTrue(hasLineType(LineType.TITLE)),
                    () -> assertTrue(hasLineType(LineType.HEADER)),
                    () -> assertTrue(hasLineType(LineType.FOOTER)),
                    () -> assertTrue(hasLineType(LineType.PACKAGE_DECLARATION)),
                    () -> assertTrue(countLineType(LineType.ENTITY_DECLARATION) +
                            countLineType(LineType.ENTITY_INLINE) >= 3),
                    () -> assertTrue(hasLineType(LineType.RELATIONSHIP))
            );
        }
    }
}