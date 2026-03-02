package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassModelParser Edge Case Tests")
class ClassModelParserComplexTest extends ClassDiagramTestBase {
    private ClassModelParser parser;

    @BeforeEach
    void setUpParser() {
        parser = new ClassModelParser();
    }

    private ClassModel parse(String resourcePath) throws IOException {
        Path file = createTempPumlFile(loadResource(resourcePath));

        return parser.parse(file.toFile());
    }

    private ClassModel parseSource(String source) throws IOException {
        Path file = createTempPumlFile(source);

        return parser.parse(file.toFile());
    }

    @Nested
    @DisplayName("Whitespace and Formatting")
    class WhitespaceTests {

        @Test
        @DisplayName("handles tabs and mixed indentation")
        void handleMixedIndentation() throws IOException {
            ClassModel result = parseSource(puml(
                    "class User {",
                    "\t+name: String",
                    "  \t  +login(): void",
                    "}"
            ));

            assertFalse(result.entities.isEmpty());
            assertFalse(result.entities.getFirst().getRawBody().isEmpty());
        }

        @Test
        @DisplayName("handles multiple empty lines between declarations")
        void handleEmptyLines() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "",
                    "",
                    "class B",
                    "",
                    "A --> B"
            ));

            assertEquals(2, result.entities.size());
            assertEquals(1, result.links.size());
        }

        @Test
        @DisplayName("handles Windows CRLF line endings")
        void handleCRLF() throws IOException {
            String source = "@startuml\r\nclass User\r\nclass Order\r\n@enduml";
            ClassModel result = parseSource(source);

            assertEquals(2, result.entities.size());
        }

        @Test
        @DisplayName("handles mixed line endings")
        void handleMixedLineEndings() throws IOException {
            String source = "@startuml\nclass A\r\nclass B\rclass C\n@enduml";
            ClassModel result = parseSource(source);

            assertNotNull(result);
            assertTrue(result.entities.size() >= 2);
        }
    }

    @Nested
    @DisplayName("Empty and Minimal Content")
    class EmptyContentTests {

        @Test
        @DisplayName("empty-diagram: handles empty @startuml/@enduml")
        void handleEmptyDiagram() throws IOException {
            ClassModel result = parse("edge-cases/empty-diagram.puml");

            assertNotNull(result);
            assertTrue(result.entities.isEmpty());
        }

        @Test
        @DisplayName("minimal: handles single class")
        void handleMinimal() throws IOException {
            ClassModel result = parse("edge-cases/minimal.puml");

            assertEquals(1, result.entities.size());
        }

        @Test
        @DisplayName("handles empty class body with braces")
        void handleEmptyClassBody() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Empty {",
                    "}"
            ));

            assertTrue(result.entities.getFirst().getRawBody().isEmpty());
        }

        @Test
        @DisplayName("handles empty package")
        void handleEmptyPackage() throws IOException {
            ClassModel result = parseSource(puml(
                    "package Empty {",
                    "}"
            ));

            assertEquals(1, result.packages.size());
            assertTrue(result.packages.getFirst().getEntities().isEmpty());
        }

        @Test
        @DisplayName("with-comments: ignores comments correctly")
        void handleComments() throws IOException {
            ClassModel result = parse("edge-cases/with-comments.puml");

            assertNotNull(result);

            boolean hasCommentEntity = result.entities.stream().anyMatch(e -> e.getName().contains("'"));
            assertFalse(hasCommentEntity);
        }
    }

    @Nested
    @DisplayName("Self-Referencing Relationships")
    class SelfReferenceTests {

        @Test
        @DisplayName("handles single self-reference")
        void handleSingleSelfReference() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Node",
                    "Node --> Node : parent"
            ));

            assertEquals(1, result.entities.size());
            assertEquals(1, result.links.size());

            ClassLink link = result.links.getFirst();
            assertEquals(link.getEntity1().getId(), link.getEntity2().getId());
        }

        @Test
        @DisplayName("handles multiple self-references on same entity")
        void handleMultipleSelfReferences() throws IOException {
            ClassModel result = parseSource(puml(
                    "class TreeNode",
                    "TreeNode --> TreeNode : parent",
                    "TreeNode --> TreeNode : children"
            ));

            assertEquals(1, result.entities.size());
            assertEquals(2, result.links.size());

            assertTrue(result.links.stream().allMatch(l -> l.getEntity1().getId().equals(l.getEntity2().getId())));
        }

        @Test
        @DisplayName("handles self-reference with different decorators")
        void handleSelfReferenceDecorators() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Category",
                    "Category *-- Category : subcategories",
                    "Category o-- Category : related"
            ));

            assertEquals(2, result.links.size());
            assertNotEquals(
                    result.links.get(0).getDecorator1(),
                    result.links.get(1).getDecorator1()
            );
        }
    }

    @Nested
    @DisplayName("Relationship Edge Cases")
    class RelationshipTests {

        @Test
        @DisplayName("handles multiple relationships between same entities")
        void handleParallelRelationships() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "class B",
                    "A --> B : uses",
                    "A ..> B : depends",
                    "A --o B : aggregates"
            ));

            assertEquals(3, result.links.size());
        }

        @Test
        @DisplayName("handles circular relationships")
        void handleCircularRelationships() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "class B",
                    "class C",
                    "A --> B",
                    "B --> C",
                    "C --> A"
            ));

            assertEquals(3, result.links.size());
        }

        @Test
        @DisplayName("handles hidden relationship")
        void handleHiddenRelationship() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "class B",
                    "A -[hidden]-> B"
            ));

            assertEquals(1, result.links.size());
            assertEquals("INVISIBLE", result.links.getFirst().getType());
        }

        @Test
        @DisplayName("handles bidirectional relationship")
        void handleBidirectional() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "class B",
                    "A <--> B"
            ));

            assertEquals(1, result.links.size());
        }

        @Test
        @DisplayName("handles relationship with both cardinalities")
        void handleBothCardinalities() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Order",
                    "class OrderLine",
                    "Order \"1\" *-- \"1..*\" OrderLine"
            ));

            ClassLink link = result.links.getFirst();
            assertNotNull(link.getQuantifier1());
            assertNotNull(link.getQuantifier2());
        }

        @Test
        @DisplayName("with-directions: handles directional arrows")
        void handleDirectionalArrows() throws IOException {
            ClassModel result = parse("relationships/with-directions.puml");

            assertTrue(result.links.size() >= 4);
        }

        @Test
        @DisplayName("all-arrow-types: handles all decorator combinations")
        void handleAllArrowTypes() throws IOException {
            ClassModel result = parse("relationships/all-arrow-types.puml");

            assertTrue(result.links.size() >= 8);
        }
    }

    @Nested
    @DisplayName("Package Edge Cases")
    class PackageTests {

        @Test
        @DisplayName("handles deeply nested packages (5 levels)")
        void handleDeeplyNested() throws IOException {
            ClassModel result = parseSource(puml(
                    "package a {",
                    "  package b {",
                    "    package c {",
                    "      package d {",
                    "        package e {",
                    "          class Deep",
                    "        }",
                    "      }",
                    "    }",
                    "  }",
                    "}"
            ));

            assertEquals(5, result.packages.size());
            assertEquals(1, result.entities.size());
        }

        @Test
        @DisplayName("handles quoted package name with spaces")
        void handlePackageWithSpaces() throws IOException {
            ClassModel result = parseSource(puml(
                    "package \"My Package\" {",
                    "  class A",
                    "}"
            ));

            assertEquals("My Package", result.packages.getFirst().getName());
        }

        @Test
        @DisplayName("handles relationship across packages")
        void handleCrossPackageRelationship() throws IOException {
            ClassModel result = parseSource(puml(
                    "package Domain {",
                    "  class User",
                    "}",
                    "package Service {",
                    "  class UserService",
                    "}",
                    "UserService --> User"
            ));

            assertEquals(1, result.links.size());
        }

        @Test
        @DisplayName("container-types: handles all container types")
        void handleContainerTypes() throws IOException {
            ClassModel result = parse("packages/container-types.puml");

            assertEquals(7, result.packages.size());
        }
    }

    @Nested
    @DisplayName("Member Edge Cases")
    class MemberTests {

        @Test
        @DisplayName("with-separators: handles separator lines in class")
        void handleSeparators() throws IOException {
            ClassModel result = parse("edge-cases/with-separators.puml");

            assertFalse(result.entities.getFirst().getRawBody().isEmpty());
        }

        @Test
        @DisplayName("handles method with many parameters")
        void handleManyParameters() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Calc {",
                    "  +calc(a: int, b: int, c: int, d: int): int",
                    "}"
            ));

            assertFalse(result.entities.getFirst().getRawBody().isEmpty());
        }

        @Test
        @DisplayName("handles field with default value")
        void handleDefaultValue() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Config {",
                    "  -timeout: int = 30",
                    "}"
            ));

            assertFalse(result.entities.getFirst().getRawBody().isEmpty());
        }

        @Test
        @DisplayName("static-abstract-members: handles {static} and {abstract}")
        void handleStaticAbstract() throws IOException {
            ClassModel result = parse("entities/static-abstract-members.puml");

            boolean hasAbstract = result.entities.stream().anyMatch(e -> e.getType().equals("ABSTRACT_CLASS"));
            assertTrue(hasAbstract);
        }
    }

    @Nested
    @DisplayName("Special Entity Types")
    class SpecialEntityTests {

        @Test
        @DisplayName("special-entity-types: parses diamond, circle, lollipop")
        void handleSpecialTypes() throws IOException {
            ClassModel result = parse("edge-cases/special-entity-types.puml");

            boolean hasSpecial = result.entities.stream()
                    .anyMatch(e -> e.getType().equals("DIAMOND") || e.getType().equals("CIRCLE"));
            assertTrue(hasSpecial);
        }

        @Test
        @DisplayName("special-characters: parses generics, stereotypes, colors")
        void handleSpecialCharacters() throws IOException {
            ClassModel result = parse("edge-cases/special-characters.puml");

            assertAll(
                    () -> assertTrue(result.entities.stream().anyMatch(ClassEntity::isGeneric)),
                    () -> assertTrue(result.entities.stream().anyMatch(ClassEntity::isStereotype)),
                    () -> assertTrue(result.entities.stream().anyMatch(e -> e.getExplicitBackground() != null))
            );
        }
    }

    @Nested
    @DisplayName("Generic Type Edge Cases")
    class GenericTests {

        @Test
        @DisplayName("handles multiple generic parameters")
        void handleMultipleParams() throws IOException {
            ClassModel result = parseSource(puml("class Map<K, V>"));

            ClassEntity entity = result.entities.getFirst();
            assertTrue(entity.isGeneric());
            assertTrue(entity.getGeneric().contains("K"));
            assertTrue(entity.getGeneric().contains("V"));
        }

        @Test
        @DisplayName("handles nested generic types")
        void handleNestedGenerics() throws IOException {
            ClassModel result = parseSource(puml("class Complex<Map<String, List<T>>>"));

            assertTrue(result.entities.getFirst().isGeneric());
        }

        @Test
        @DisplayName("handles bounded generic type")
        void handleBoundedGeneric() throws IOException {
            ClassModel result = parseSource(puml("class Container<T extends Comparable>"));

            assertTrue(result.entities.getFirst().isGeneric());
        }
    }

    @Nested
    @DisplayName("Note Edge Cases")
    class NoteTests {

        @Test
        @DisplayName("handles floating note")
        void handleFloatingNote() throws IOException {
            ClassModel result = parseSource(puml(
                    "note \"Floating\" as N1",
                    "class A"
            ));

            boolean hasNote = result.entities.stream().anyMatch(e -> e.getType().equals("NOTE"));
            assertTrue(hasNote);
        }

        @Test
        @DisplayName("handles multiple notes on same entity")
        void handleMultipleNotes() throws IOException {
            ClassModel result = parseSource(puml(
                    "class A",
                    "note left of A : Left",
                    "note right of A : Right"
            ));

            long noteCount = result.entities.stream().filter(e -> e.getType().equals("NOTE")).count();
            assertTrue(noteCount >= 2);
        }

        @Test
        @DisplayName("notes-on-links: handles note attached to link")
        void handleNoteOnLink() throws IOException {
            ClassModel result = parse("notes/notes-on-links.puml");

            boolean hasNoteOnLink = result.links.stream().anyMatch(l -> l.getNoteOnLink() != null);
            assertTrue(hasNoteOnLink);
        }
    }

    @Nested
    @DisplayName("Duplicate and Conflict Handling")
    class DuplicateTests {

        @Test
        @DisplayName("handles duplicate class declarations (PlantUML merges)")
        void handleDuplicateClass() throws IOException {
            ClassModel result = parseSource(puml("class User", "class User"));

            assertEquals(1, result.entities.size());
        }

        @Test
        @DisplayName("handles same class name in different packages")
        void handleSameNameDifferentPackages() throws IOException {
            ClassModel result = parseSource(puml(
                    "package domain {",
                    "  class User",
                    "}",
                    "package dto {",
                    "  class User",
                    "}"
            ));

            long count = result.entities.stream().filter(e -> e.getName().equals("User")).count();
            assertEquals(2, count);
        }
    }

    @Nested
    @DisplayName("PlantUML Directives")
    class DirectiveTests {

        @Test
        @DisplayName("no-startuml: auto-wraps content without @startuml")
        void handleNoStartUml() throws IOException {
            ClassModel result = parse("edge-cases/no-startuml.puml");

            assertFalse(result.entities.isEmpty());
        }

        @Test
        @DisplayName("left-to-right: handles direction directive")
        void handleLeftToRight() throws IOException {
            ClassModel result = parse("edge-cases/left-to-right.puml");

            assertNotNull(result);
            assertFalse(result.entities.isEmpty());
        }

        @Test
        @DisplayName("handles 'remove' directive")
        void handleRemove() throws IOException {
            ClassModel result = parseSource(puml(
                    "class User",
                    "class Order",
                    "remove Order"
            ));

            boolean hasOrder = result.entities.stream().anyMatch(e -> e.getName().equals("Order"));
            assertFalse(hasOrder);
        }

        @Test
        @DisplayName("handles 'together' block")
        void handleTogether() throws IOException {
            ClassModel result = parseSource(puml(
                    "together {",
                    "  class A",
                    "  class B",
                    "}"
            ));

            assertEquals(2, result.entities.size());
        }
    }

    @Nested
    @DisplayName("Inheritance Edge Cases")
    class InheritanceTests {

        @Test
        @DisplayName("handles inline extends keyword")
        void handleInlineExtends() throws IOException {
            ClassModel result = parseSource(puml(
                    "class Parent",
                    "class Child extends Parent"
            ));

            assertFalse(result.links.isEmpty());
        }

        @Test
        @DisplayName("handles inline implements keyword")
        void handleInlineImplements() throws IOException {
            ClassModel result = parseSource(puml(
                    "interface Runnable",
                    "class Task implements Runnable"
            ));

            assertFalse(result.links.isEmpty());
        }

        @Test
        @DisplayName("inheritance-hierarchy: parses complex hierarchy")
        void handleInheritanceHierarchy() throws IOException {
            ClassModel result = parse("relationships/inheritance-hierarchy.puml");

            assertTrue(result.links.size() >= 3);

            boolean hasExtends = result.links.stream().anyMatch(l -> "EXTENDS".equals(l.getDecorator2()));
            assertTrue(hasExtends);
        }
    }

    @Nested
    @DisplayName("Scale Tests")
    class ScaleTests {

        @Test
        @DisplayName("handles 50 entities")
        void handleManyEntities() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\n");
            for (int i = 0; i < 50; i++) {
                sb.append("class C").append(i).append("\n");
            }
            sb.append("@enduml");

            ClassModel result = parseSource(sb.toString());
            assertEquals(50, result.entities.size());
        }

        @Test
        @DisplayName("handles entity with 50 members")
        void handleManyMembers() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\nclass Big {\n");
            for (int i = 0; i < 50; i++) {
                sb.append("  +field").append(i).append(": String\n");
            }
            sb.append("}\n@enduml");

            ClassModel result = parseSource(sb.toString());
            assertEquals(50, result.entities.getFirst().getRawBody().size());
        }

        @Test
        @DisplayName("handles chain of 20 relationships")
        void handleManyRelationships() throws IOException {
            StringBuilder sb = new StringBuilder("@startuml\n");
            for (int i = 0; i < 21; i++) {
                sb.append("class C").append(i).append("\n");
            }

            for (int i = 0; i < 20; i++) {
                sb.append("C").append(i).append(" --> C").append(i + 1).append("\n");
            }
            sb.append("@enduml");

            ClassModel result = parseSource(sb.toString());
            assertEquals(20, result.links.size());
        }
    }
}
