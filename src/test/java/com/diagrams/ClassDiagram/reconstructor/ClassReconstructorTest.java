/*
 * File: ClassReconstructorTest.java
 * Author: Norman Babiak
 * Description: Tests for reconstructor classes, line mapper and line finder
 * Date: 28.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.ClassDiagram.reconstructor;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper.LineType;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Class Reconstructor Tests")
class ClassReconstructorTest extends ClassDiagramTestBase {
    private ClassLineFinder finder;
    private Map<Object, Integer> finderMap;

    private void setupFinder(String resourcePath) {
        createMapper(resourcePath);
        finderMap = new HashMap<>();
        finder = new ClassLineFinder(lineMapper, finderMap);
    }

    private void setupFinderFromSource(String source) {
        model = new com.diagrams.ClassDiagram.model.ClassModel();
        lineMapper = new ClassLineMapper(source, model);
        finderMap = new HashMap<>();
        finder = new ClassLineFinder(lineMapper, finderMap);
    }

    private long countType(LineType type) {
        return lineMapper.getLineInfos().stream().filter(info -> info.type == type).count();
    }

    @Test
    @DisplayName("mapper: detects entity declarations, members, and block structure")
    void mapperEntityStructure() {
        createMapper("entities/simple-class.puml");

        assertLineType(0, LineType.START_UML);
        assertLineType(1, LineType.ENTITY_DECLARATION);
        assertEquals(4, countType(LineType.MEMBER));
        assertTrue(countType(LineType.BLOCK_END) >= 1);
        assertLineType(7, LineType.END_UML);
    }

    @Test
    @DisplayName("mapper: detects all entity keywords")
    void mapperEntityKeywords() {
        createMapper("entities/multiple-types.puml");

        Set<String> keywords = lineMapper.getLineInfos().stream()
                .filter(info -> info.type == LineType.ENTITY_DECLARATION || info.type == LineType.ENTITY_INLINE)
                .map(info -> info.trimmedText.split(" ")[0].toLowerCase())
                .collect(Collectors.toSet());

        assertTrue(keywords.containsAll(Set.of("class", "interface", "enum", "abstract", "annotation")));
    }

    @Test
    @DisplayName("mapper: detects relationships and arrow types")
    void mapperRelationships() {
        createMapper("relationships/basic-arrows.puml");
        assertEquals(5, countType(LineType.RELATIONSHIP));

        createMapper("relationships/all-arrow-types.puml");
        assertTrue(countType(LineType.RELATIONSHIP) >= 8);
    }

    @Test
    @DisplayName("mapper: detects packages, notes, comments, page details, direction")
    void mapperMiscTypes() {
        createMapper("packages/simple-packages.puml");
        assertEquals(2, countType(LineType.PACKAGE_DECLARATION));

        createMapper("notes/basic-notes.puml");
        assertTrue(countType(LineType.NOTE) >= 2);

        createMapper("edge-cases/with-comments.puml");
        assertTrue(lineMapper.getLineInfos().stream().anyMatch(info -> info.type == LineType.COMMENT && info.trimmedText.startsWith("'")));

        createMapper("complex/full-diagram.puml");
        assertTrue(countType(LineType.TITLE) > 0 && countType(LineType.HEADER) > 0 && countType(LineType.FOOTER) > 0);

        createMapper("edge-cases/left-to-right.puml");
        assertTrue(countType(LineType.LEFT_TO_RIGHT) > 0);
        assertTrue(model.isLeftToRight());
    }

    @Test
    @DisplayName("mapper: returns null for invalid indices")
    void mapperBounds() {
        createMapper("edge-cases/minimal.puml");

        assertNotNull(lineMapper.getLineInfo(0));
        assertNull(lineMapper.getLineInfo(-1));
        assertNull(lineMapper.getLineInfo(1000));
    }

    @Test
    @DisplayName("finder: locates entities by name and claims lines")
    void finderEntities() {
        setupFinder("entities/simple-class.puml");
        Object element = new Object();
        int line = finder.findEntityLine("User", element);

        assertEquals(1, line);
        assertEquals(line, finderMap.get(element));
        assertEquals(-1, finder.findEntityLine("User", new Object()), "Same line not found twice");
        assertEquals(-1, finder.findEntityLine("NonExistent", new Object()));
    }

    @Test
    @DisplayName("finder: locates relationships and advances position")
    void finderRelationships() {
        setupFinderFromSource(puml("class A", "class B", "A --> B : first", "A ..> B : second"));

        int line1 = finder.findRelationshipLine("A", "B", new Object());
        int line2 = finder.findRelationshipLine("A", "B", new Object());

        assertTrue(line1 >= 0 && line2 > line1);
    }

    @Test
    @DisplayName("finder: locates packages and notes")
    void finderPackagesAndNotes() {
        setupFinder("packages/simple-packages.puml");
        assertTrue(finder.findPackageLine("Domain Model", new Object()) >= 0);
        assertEquals(-1, finder.findPackageLine("nonexistent", new Object()));

        setupFinder("notes/basic-notes.puml");
        int noteLine = finder.findNoteLine("", new Object());
        assertTrue(noteLine >= 0);
        assertTrue(finder.findNoteEndLine(noteLine, new Object()) >= noteLine);
    }

    @Test
    @DisplayName("finder: locates title, header, footer")
    void finderPageDetails() {
        setupFinder("complex/full-diagram.puml");
        assertTrue(finder.findTitleLine() >= 0);
        assertTrue(finder.findHeaderLine() >= 0);
        assertTrue(finder.findFooterLine() >= 0);

        setupFinder("entities/simple-class.puml");
        assertEquals(-1, finder.findTitleLine());
    }

    @Test
    @DisplayName("finder: entity matching and alias extraction")
    void finderMatching() {
        assertTrue(ClassLineFinder.entityMatchesName("class User", "User"));
        assertTrue(ClassLineFinder.entityMatchesName("class \"User Account\" as User", "User Account"));
        assertFalse(ClassLineFinder.entityMatchesName("class UserService", "User"));

        assertEquals("UA", ClassLineFinder.extractAlias("class \"User Account\" as UA"));
        assertEquals("List", ClassLineFinder.extractAlias("class List<T>"));
        assertNull(ClassLineFinder.extractAlias("A --> B"));
    }

    @Test
    @DisplayName("finder: tracks multiple elements and handles self-references")
    void finderComplexTracking() {
        setupFinderFromSource(puml("class Node", "Node --> Node : parent", "Node --> Node : children"));

        int entityLine = finder.findEntityLine("Node", "entity");
        int linkLine1 = finder.findRelationshipLine("Node", "Node", "link1");
        int linkLine2 = finder.findRelationshipLine("Node", "Node", "link2");

        assertTrue(entityLine >= 0 && linkLine1 >= 0 && linkLine2 >= 0);
        assertNotEquals(linkLine1, linkLine2);
        assertEquals(3, finderMap.size());
    }

    @Test
    @DisplayName("finder: setPosition affects search start")
    void finderPosition() {
        setupFinderFromSource(puml("class A", "class B", "A --> B", "A --> B", "A --> B"));
        finder.setPosition(4);

        assertEquals(4, finder.findRelationshipLine("A", "B", new Object()));
    }
}
