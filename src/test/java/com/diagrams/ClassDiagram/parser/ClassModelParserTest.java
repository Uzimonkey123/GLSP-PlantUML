/*
 * File: ClassModelParserTest.java
 * Author: Norman Babiak
 * Description: Tests for class model parser
 * Date: 28.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassModelParser Tests")
class ClassModelParserTest extends ClassDiagramTestBase {
    private ClassModelParser parser;

    @BeforeEach
    void setup() { parser = new ClassModelParser(); }

    private ClassModel parse(String path) throws IOException {
        return parser.parse(createTempPumlFile(loadResource(path)).toFile());
    }

    private ClassModel parseSource(String source) throws IOException {
        return parser.parse(createTempPumlFile(source).toFile());
    }

    private Set<String> entityNames(ClassModel model) {
        return model.entities.stream().map(ClassEntity::getName).collect(Collectors.toSet());
    }

    private Set<String> entityTypes(ClassModel model) {
        return model.entities.stream().map(ClassEntity::getType).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("parses class with fields, methods, and line info")
    void simpleClass() throws IOException {
        ClassModel model = parse("entities/simple-class.puml");
        ClassEntity entity = model.entities.getFirst();

        assertEquals(1, model.entities.size());
        assertEquals("User", entity.getName());
        assertEquals("CLASS", entity.getType());
        assertFalse(entity.getFields().isEmpty());
        assertFalse(entity.getMethods().isEmpty());
        assertTrue(entity.hasLine());
    }

    @Test
    @DisplayName("parses all entity types")
    void allEntityTypes() throws IOException {
        Set<String> types = entityTypes(parse("entities/multiple-types.puml"));

        assertAll(
                () -> assertTrue(types.contains("CLASS")),
                () -> assertTrue(types.contains("INTERFACE")),
                () -> assertTrue(types.contains("ABSTRACT_CLASS")),
                () -> assertTrue(types.contains("ENUM")),
                () -> assertTrue(types.contains("ANNOTATION"))
        );
    }

    @Test
    @DisplayName("parses quoted names with aliases")
    void aliases() throws IOException {
        Optional<ClassEntity> entity = parse("entities/with-aliases.puml").entities.stream()
                .filter(ent -> "UserAccount".equals(ent.getAlias())).findFirst();

        assertTrue(entity.isPresent());
        assertEquals("User Account", entity.get().getName());
    }

    @Test
    @DisplayName("parses relationships with line tracking")
    void basicRelationships() throws IOException {
        ClassModel model = parse("relationships/basic-arrows.puml");

        assertEquals(5, model.links.size());
        assertTrue(model.links.stream().allMatch(ClassLink::hasLine));
    }

    @Test
    @DisplayName("parses inheritance with EXTENDS decorator")
    void inheritance() throws IOException {
        ClassModel model = parse("relationships/inheritance-hierarchy.puml");

        assertTrue(model.links.stream().anyMatch(link -> "EXTENDS".equals(link.getDecorator2())));
    }

    @Test
    @DisplayName("parses self-referencing relationships")
    void selfReference() throws IOException {
        ClassModel model = parseSource(puml("class Node", "Node --> Node : parent", "Node --> Node : children"));

        assertEquals(1, model.entities.size());
        assertEquals(2, model.links.size());
        assertTrue(model.links.stream().allMatch(link -> link.getEntity1().getId().equals(link.getEntity2().getId())));
    }

    @Test
    @DisplayName("hidden relationship parsed as INVISIBLE")
    void hiddenRelationship() throws IOException {
        ClassModel model = parseSource(puml("class A", "class B", "A -[hidden]-> B"));

        assertEquals("INVISIBLE", model.links.getFirst().getType());
    }

    @Test
    @DisplayName("parses packages with nesting")
    void packages() throws IOException {
        ClassModel simple = parse("packages/simple-packages.puml");
        ClassModel nested = parse("packages/nested-packages.puml");

        assertEquals(2, simple.packages.size());
        assertTrue(simple.packages.stream().anyMatch(pkg -> !pkg.getEntities().isEmpty()));
        assertTrue(nested.packages.stream().anyMatch(pkg -> pkg.getParentPackage() != null));
    }

    @Test
    @DisplayName("parses basic, on-link, and styled notes")
    void notes() throws IOException {
        ClassModel basic = parse("notes/basic-notes.puml");
        ClassModel onLinks = parse("notes/notes-on-links.puml");
        ClassModel styled = parse("notes/notes-with-styling.puml");

        assertTrue(basic.entities.stream().filter(entity -> "NOTE".equals(entity.getType())).count() >= 2);
        assertTrue(onLinks.links.stream().anyMatch(link -> link.getNoteOnLink() != null));
        assertTrue(styled.entities.stream()
                .filter(entity -> "NOTE".equals(entity.getType())).anyMatch(entity -> entity.getExplicitBackground() != null));
    }

    @Test
    @DisplayName("parses generics including nested and bounded")
    void generics() throws IOException {
        ClassEntity entity = parseSource(puml("class Map<K, V>")).entities.getFirst();
        ClassEntity bounded = parseSource(puml("class Container<T extends Comparable>")).entities.getFirst();

        assertTrue(entity.isGeneric());
        assertTrue(entity.getGeneric().contains("K") && entity.getGeneric().contains("V"));
        assertTrue(bounded.isGeneric());
    }

    @Test
    @DisplayName("parses generics, stereotypes, and colors")
    void specialCharacters() throws IOException {
        ClassModel model = parse("edge-cases/special-characters.puml");

        assertTrue(model.entities.stream().anyMatch(ClassEntity::isGeneric));
        assertTrue(model.entities.stream().anyMatch(ClassEntity::isStereotype));
        assertTrue(model.entities.stream().anyMatch(entity -> entity.getExplicitBackground() != null));
    }

    @Test
    @DisplayName("handles empty diagram, CRLF, and missing @startuml")
    void edgeCases() throws IOException {
        ClassModel empty = parse("edge-cases/empty-diagram.puml");
        ClassModel crlf = parseSource("@startuml\r\nclass A\r\nclass B\r\n@enduml");
        ClassModel noStart = parse("edge-cases/no-startuml.puml");

        assertTrue(empty.entities.isEmpty() && empty.links.isEmpty());
        assertEquals(2, crlf.entities.size());
        assertFalse(noStart.entities.isEmpty());
    }

    @Test
    @DisplayName("deduplicates classes, allows same name across packages")
    void duplicates() throws IOException {
        ClassModel flat = parseSource(puml("class User", "class User"));
        ClassModel packaged = parseSource(puml("package a {", "class User", "}", "package b {", "class User", "}"));

        assertEquals(1, flat.entities.size());
        assertEquals(2, packaged.entities.stream().filter(entity -> entity.getName().equals("User")).count());
    }

    @Test
    @DisplayName("layered architecture: entities, packages, links present")
    void layeredArchitecture() throws IOException {
        ClassModel model = parse("complex/layered-architecture.puml");
        Set<String> names = entityNames(model);

        assertAll(
                () -> assertEquals(3, model.packages.size()),
                () -> assertTrue(names.contains("ApplicationService")),
                () -> assertTrue(names.contains("User")),
                () -> assertTrue(names.contains("OrderRepository")),
                () -> assertTrue(model.links.size() >= 8),
                () -> assertFalse(model.title.isEmpty())
        );
    }

    @Test
    @DisplayName("resets state and sets maper/finder between parses")
    void parserState() throws IOException {
        ClassModel first = parse("entities/simple-class.puml");
        ClassModel second = parse("edge-cases/minimal.puml");

        assertNotSame(first, second);
        assertEquals("ent-0", second.entities.getFirst().getId());
        assertNotNull(first.getLineMapper());
        assertNotNull(first.getLineFinder());
    }
}
