package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassLineFinder Tests")
class ClassLineFinderTest extends ClassDiagramTestBase {
    private ClassLineFinder finder;
    private Map<Object, Integer> elementToLineMap = new HashMap<>();

    protected void createFinder(String resourcePath) {
        createMapper(resourcePath);
        elementToLineMap = new HashMap<>();
        finder = new ClassLineFinder(lineMapper, elementToLineMap);
    }

    private void createFinderFromSource(String source) {
        model = new com.diagrams.ClassDiagram.model.ClassModel();
        lineMapper = new ClassLineMapper(source, model);
        elementToLineMap = new HashMap<>();
        finder = new ClassLineFinder(lineMapper, elementToLineMap);
    }

    @Nested
    @DisplayName("Entity Line Finding")
    class EntityLineTests {

        @Test
        @DisplayName("finds simple class declaration")
        void findSimpleClass() {
            createFinder("entities/simple-class.puml");

            Object element = new Object();
            int line = finder.findEntityLine("User", element);

            assertEquals(1, line);
            assertEquals(line, elementToLineMap.get(element));
        }

        @Test
        @DisplayName("finds multiple entity types")
        void findMultipleTypes() {
            createFinder("entities/multiple-types.puml");

            assertTrue(finder.findEntityLine("Person", new Object()) >= 0);
            assertTrue(finder.findEntityLine("Animal", new Object()) >= 0);
            assertTrue(finder.findEntityLine("Color", new Object()) >= 0);
        }

        @Test
        @DisplayName("finds entity with alias")
        void findEntityWithAlias() {
            createFinder("entities/with-aliases.puml");

            int line = finder.findEntityLine("User Account", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds inline entity")
        void findInlineEntity() {
            createFinder("entities/inline-entities.puml");

            int line = finder.findEntityLine("User", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("returns -1 for non-existent entity")
        void entityNotFound() {
            createFinder("entities/simple-class.puml");

            int line = finder.findEntityLine("NonExistent", new Object());

            assertEquals(-1, line);
        }

        @Test
        @DisplayName("handles generic type in entity name")
        void findGenericEntity() {
            createFinder("edge-cases/special-characters.puml");

            int line = finder.findEntityLine("Class-With-Dashes", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("claims line so it's not found twice")
        void claimsLine() {
            createFinderFromSource(puml("class A", "class B"));

            Object elem1 = new Object();
            Object elem2 = new Object();

            int line1 = finder.findEntityLine("A", elem1);
            int line2 = finder.findEntityLine("A", elem2);

            assertTrue(line1 >= 0);
            assertEquals(-1, line2, "Same line should not be found twice");
        }
    }

    @Nested
    @DisplayName("Relationship Line Finding")
    class RelationshipLineTests {

        @Test
        @DisplayName("finds basic relationship")
        void findBasicRelationship() {
            createFinder("relationships/basic-arrows.puml");

            int line = finder.findRelationshipLine("A", "B", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds relationship with cardinality")
        void findRelationshipWithCardinality() {
            createFinder("relationships/with-cardinality.puml");

            int line = finder.findRelationshipLine("Order", "OrderLine", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds multiple relationships between same entities")
        void findMultipleRelationships() {
            createFinderFromSource(puml(
                    "class A",
                    "class B",
                    "A --> B : first",
                    "A ..> B : second"
            ));

            Object elem1 = new Object();
            Object elem2 = new Object();

            int line1 = finder.findRelationshipLine("A", "B", elem1);
            int line2 = finder.findRelationshipLine("A", "B", elem2);

            assertTrue(line1 >= 0);
            assertTrue(line2 >= 0);
            assertNotEquals(line1, line2, "Should find different lines");
        }

        @Test
        @DisplayName("returns -1 for non-existent relationship")
        void relationshipNotFound() {
            createFinder("relationships/basic-arrows.puml");

            int line = finder.findRelationshipLine("X", "Y", new Object());

            assertEquals(-1, line);
        }

        @Test
        @DisplayName("advances search position after finding")
        void advancesSearchPosition() {
            createFinderFromSource(puml(
                    "class A",
                    "class B",
                    "A --> B",
                    "A --> B"
            ));

            int line1 = finder.findRelationshipLine("A", "B", new Object());
            int line2 = finder.findRelationshipLine("A", "B", new Object());

            assertTrue(line2 > line1);
        }
    }

    @Nested
    @DisplayName("Package Line Finding")
    class PackageLineTests {

        @Test
        @DisplayName("finds simple package")
        void findSimplePackage() {
            createFinder("packages/simple-packages.puml");

            int line = finder.findPackageLine("Domain Model", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds nested package")
        void findNestedPackage() {
            createFinder("packages/nested-packages.puml");

            int line = finder.findPackageLine("domain", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds container types as packages")
        void findContainerTypes() {
            createFinder("packages/container-types.puml");

            assertAll(
                    () -> assertTrue(finder.findPackageLine("Database", new Object()) >= 0),
                    () -> assertTrue(finder.findPackageLine("Rectangle Container", new Object()) >= 0),
                    () -> assertTrue(finder.findPackageLine("Folder Container", new Object()) >= 0)
            );
        }

        @Test
        @DisplayName("returns -1 for non-existent package")
        void packageNotFound() {
            createFinder("packages/simple-packages.puml");

            int line = finder.findPackageLine("nonexistent", new Object());

            assertEquals(-1, line);
        }
    }

    @Nested
    @DisplayName("Note Line Finding")
    class NoteLineTests {

        @Test
        @DisplayName("finds note by content")
        void findNoteByContent() {
            createFinder("notes/basic-notes.puml");

            int line = finder.findNoteLine("", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds styled note")
        void findStyledNote() {
            createFinder("notes/notes-with-styling.puml");

            int line = finder.findNoteLine("", new Object());

            assertTrue(line >= 0);
        }

        @Test
        @DisplayName("finds note end line")
        void findNoteEndLine() {
            createFinder("notes/basic-notes.puml");

            int startLine = finder.findNoteLine("", new Object());
            int endLine = finder.findNoteEndLine(startLine, new Object());

            assertTrue(endLine >= startLine);
        }
    }

    @Nested
    @DisplayName("Page Details Finding")
    class PageDetailsTests {

        @Test
        @DisplayName("finds title, header, footer")
        void findPageDetails() {
            createFinder("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(finder.findTitleLine() >= 0, "Should find title"),
                    () -> assertTrue(finder.findHeaderLine() >= 0, "Should find header"),
                    () -> assertTrue(finder.findFooterLine() >= 0, "Should find footer")
            );
        }

        @Test
        @DisplayName("returns -1 when page details missing")
        void pageDetailsMissing() {
            createFinder("entities/simple-class.puml");

            assertAll(
                    () -> assertEquals(-1, finder.findTitleLine()),
                    () -> assertEquals(-1, finder.findHeaderLine()),
                    () -> assertEquals(-1, finder.findFooterLine())
            );
        }
    }

    @Nested
    @DisplayName("Entity Matching")
    class EntityMatchingTests {

        @Test
        @DisplayName("matches simple class name")
        void matchSimpleName() {
            assertTrue(ClassLineFinder.entityMatchesName("class User", "User"));
        }

        @Test
        @DisplayName("matches quoted name")
        void matchQuotedName() {
            assertTrue(ClassLineFinder.entityMatchesName("class \"User Account\" as User", "User Account"));
        }

        @Test
        @DisplayName("matches name with generic stripped")
        void matchGenericStripped() {
            assertTrue(ClassLineFinder.entityMatchesName("class Container<T>", "Container"));
        }

        @Test
        @DisplayName("does not match partial name")
        void noPartialMatch() {
            assertFalse(ClassLineFinder.entityMatchesName("class UserService", "User"));
        }

        @Test
        @DisplayName("matches empty/null name to anything")
        void matchEmptyName() {
            assertTrue(ClassLineFinder.entityMatchesName("class Anything", ""));
            assertTrue(ClassLineFinder.entityMatchesName("class Anything", null));
        }
    }

    @Nested
    @DisplayName("Relationship Matching")
    class RelationshipMatchingTests {

        @Test
        @DisplayName("matches entity names in relationship")
        void matchRelationship() {
            assertTrue(ClassLineFinder.relMatchesName("A --> B", "A"));
            assertTrue(ClassLineFinder.relMatchesName("A --> B", "B"));
        }

        @Test
        @DisplayName("ignores label part")
        void ignoreLabel() {
            assertTrue(ClassLineFinder.relMatchesName("A --> B : uses", "A"));
            assertFalse(ClassLineFinder.relMatchesName("A --> B : uses", "uses"));
        }

        @Test
        @DisplayName("matches quoted names")
        void matchQuotedInRelationship() {
            assertTrue(ClassLineFinder.relMatchesName("\"User\" --> \"Order\"", "User"));
        }
    }

    @Nested
    @DisplayName("Alias Extraction")
    class AliasExtractionTests {

        @Test
        @DisplayName("extracts simple class name")
        void extractSimpleName() {
            assertEquals("User", ClassLineFinder.extractAlias("class User"));
        }

        @Test
        @DisplayName("extracts alias when present")
        void extractAlias() {
            assertEquals("UA", ClassLineFinder.extractAlias("class \"User Account\" as UA"));
        }

        @Test
        @DisplayName("extracts name from quoted without alias")
        void extractQuotedName() {
            assertEquals("User Account", ClassLineFinder.extractAlias("class \"User Account\""));
        }

        @Test
        @DisplayName("handles abstract class")
        void extractAbstractClass() {
            assertEquals("Base", ClassLineFinder.extractAlias("abstract class Base"));
        }

        @Test
        @DisplayName("handles interface")
        void extractInterface() {
            assertEquals("Runnable", ClassLineFinder.extractAlias("interface Runnable"));
        }

        @Test
        @DisplayName("handles enum")
        void extractEnum() {
            assertEquals("Status", ClassLineFinder.extractAlias("enum Status"));
        }

        @Test
        @DisplayName("returns null for non-entity line")
        void returnNullForNonEntity() {
            assertNull(ClassLineFinder.extractAlias("A --> B"));
            assertNull(ClassLineFinder.extractAlias("package domain"));
            assertNull(ClassLineFinder.extractAlias(null));
        }

        @Test
        @DisplayName("handles name with generic")
        void extractNameWithGeneric() {
            assertEquals("List", ClassLineFinder.extractAlias("class List<T>"));
        }

        @Test
        @DisplayName("handles name with stereotype")
        void extractNameWithStereotype() {
            assertEquals("Service", ClassLineFinder.extractAlias("class Service <<Singleton>>"));
        }

        @Test
        @DisplayName("handles name with color")
        void extractNameWithColor() {
            assertEquals("Important", ClassLineFinder.extractAlias("class Important #Red"));
        }
    }

    @Nested
    @DisplayName("Position Management")
    class PositionTests {

        @Test
        @DisplayName("setPosition affects search start")
        void setPositionAffectsSearch() {
            createFinderFromSource(puml(
                    "class A",
                    "class B",
                    "A --> B",
                    "A --> B",
                    "A --> B"
            ));

            finder.setPosition(4);
            int line = finder.findRelationshipLine("A", "B", new Object());

            assertEquals(4, line);
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexTests {

        @Test
        @DisplayName("full-diagram: finds all element types")
        void findAllInFullDiagram() {
            createFinder("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(finder.findTitleLine() >= 0),
                    () -> assertTrue(finder.findHeaderLine() >= 0),
                    () -> assertTrue(finder.findFooterLine() >= 0),
                    () -> assertTrue(finder.findPackageLine("Domain Layer", new Object()) >= 0 ||
                            finder.findPackageLine("NonExistent", new Object()) >= 0)
            );
        }

        @Test
        @DisplayName("tracks multiple elements to lines")
        void trackMultipleElements() {
            createFinderFromSource(puml(
                    "class A",
                    "class B",
                    "class C",
                    "A --> B",
                    "B --> C"
            ));

            Object elemA = "entityA";
            Object elemB = "entityB";
            Object link1 = "link1";
            Object link2 = "link2";

            finder.findEntityLine("A", elemA);
            finder.findEntityLine("B", elemB);
            finder.findRelationshipLine("A", "B", link1);
            finder.findRelationshipLine("B", "C", link2);

            assertAll(
                    () -> assertEquals(4, elementToLineMap.size()),
                    () -> assertNotNull(elementToLineMap.get(elemA)),
                    () -> assertNotNull(elementToLineMap.get(elemB)),
                    () -> assertNotNull(elementToLineMap.get(link1)),
                    () -> assertNotNull(elementToLineMap.get(link2))
            );
        }

        @Test
        @DisplayName("handles self-referencing entities")
        void handleSelfReference() {
            createFinderFromSource(puml(
                    "class Node",
                    "Node --> Node : parent",
                    "Node --> Node : children"
            ));

            int entity = finder.findEntityLine("Node", new Object());
            int link1 = finder.findRelationshipLine("Node", "Node", new Object());
            int link2 = finder.findRelationshipLine("Node", "Node", new Object());

            assertAll(
                    () -> assertTrue(entity >= 0),
                    () -> assertTrue(link1 >= 0),
                    () -> assertTrue(link2 >= 0),
                    () -> assertNotEquals(link1, link2)
            );
        }
    }
}