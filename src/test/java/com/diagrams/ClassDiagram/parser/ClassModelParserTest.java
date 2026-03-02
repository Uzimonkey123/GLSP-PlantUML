package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.ClassDiagramTestBase;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassModelParser Tests")
class ClassModelParserTest extends ClassDiagramTestBase {
    private ClassModelParser parser;

    @BeforeEach
    void setupParser() {
        parser = new ClassModelParser();
    }

    private ClassModel parse(String resourcePath) throws IOException {
        Path file = createTempPumlFile(loadResource(resourcePath));

        return parser.parse(file.toFile());
    }

    private Optional<ClassEntity> findEntity(ClassModel model, String name) {
        return model.entities.stream()
                .filter(e -> e.getName().equals(name))
                .findFirst();
    }

    private Set<String> entityNames(ClassModel model) {
        return model.entities.stream()
                .map(ClassEntity::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> entityTypes(ClassModel model) {
        return model.entities.stream()
                .map(ClassEntity::getType)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("Entity Parsing")
    class EntityParsingTests {

        @Test
        @DisplayName("simple-class: parses class with fields and methods")
        void parseSimpleClass() throws IOException {
            ClassModel model = parse("entities/simple-class.puml");

            assertEquals(1, model.entities.size());
            ClassEntity user = model.entities.getFirst();

            assertEquals("User", user.getName());
            assertEquals("CLASS", user.getType());
            assertFalse(user.getFields().isEmpty(), "Should have fields");
            assertFalse(user.getMethods().isEmpty(), "Should have methods");
            assertTrue(user.hasLine(), "Should have source line info");
        }

        @Test
        @DisplayName("multiple-types: parses class, interface, abstract, enum, annotation")
        void parseMultipleTypes() throws IOException {
            ClassModel model = parse("entities/multiple-types.puml");

            Set<String> types = entityTypes(model);

            assertAll(
                    () -> assertTrue(types.contains("CLASS"), "Should have CLASS"),
                    () -> assertTrue(types.contains("INTERFACE"), "Should have INTERFACE"),
                    // ABSTRACT in its own does not exist, ABSTRACT_CLASS = ABSTRACT/ABSTRACT_CLASS
                    () -> assertTrue(types.contains("ABSTRACT_CLASS"), "Should have ABSTRACT"),
                    () -> assertTrue(types.contains("ENUM"), "Should have ENUM"),
                    () -> assertTrue(types.contains("ANNOTATION"), "Should have ANNOTATION")
            );
        }

        @Test
        @DisplayName("with-aliases: parses quoted names with aliases")
        void parseWithAliases() throws IOException {
            ClassModel model = parse("entities/with-aliases.puml");

            Optional<ClassEntity> entity = model.entities.stream()
                    .filter(e -> "UserAccount".equals(e.getAlias()))
                    .findFirst();

            assertTrue(entity.isPresent(), "Should find entity with alias UserAccount");
            assertEquals("User Account", entity.get().getName());
        }

        @Test
        @DisplayName("visibility-modifiers: parses +, -, #, ~ prefixed members")
        void parseVisibilityModifiers() throws IOException {
            ClassModel model = parse("entities/visibility-modifiers.puml");

            ClassEntity entity = model.entities.getFirst();
            assertTrue(entity.getRawBody().size() >= 4, "Should have members with different visibilities");
        }
    }

    @Nested
    @DisplayName("Relationship Parsing")
    class RelationshipParsingTests {

        @Test
        @DisplayName("parses common relationship patterns correctly")
        void parsesCommonRelationshipPatterns() throws IOException {
            ClassModel basic = parse("relationships/basic-arrows.puml");
            ClassModel cardinality = parse("relationships/with-cardinality.puml");
            ClassModel directions = parse("relationships/with-directions.puml");

            assertEquals(5, basic.links.size());
            assertTrue(basic.links.stream().allMatch(ClassLink::hasLine));

            assertTrue(cardinality.links.stream()
                    .anyMatch(l -> l.getQuantifier1() != null || l.getQuantifier2() != null));

            assertTrue(directions.links.size() >= 4);
        }

        @Test
        @DisplayName("handles inheritance and extended arrow variations")
        void parsesAdvancedRelationshipForms() throws IOException {
            ClassModel inheritance = parse("relationships/inheritance-hierarchy.puml");
            ClassModel allTypes = parse("relationships/all-arrow-types.puml");

            boolean hasExtends = inheritance.links.stream().anyMatch(l -> "EXTENDS".equals(l.getDecorator2()));

            assertTrue(hasExtends);
            assertTrue(allTypes.links.size() >= 8);
        }
    }

    @Nested
    @DisplayName("Package Parsing")
    class PackageParsingTests {

        @Test
        @DisplayName("simple-packages: parses packages with entities")
        void parseSimplePackages() throws IOException {
            ClassModel model = parse("packages/simple-packages.puml");
            assertEquals(2, model.packages.size());

            boolean hasEntitiesInPackage = model.packages.stream().anyMatch(p -> !p.getEntities().isEmpty());
            assertTrue(hasEntitiesInPackage, "Packages should contain entities");
        }

        @Test
        @DisplayName("nested-packages: parses parent-child hierarchy")
        void parseNestedPackages() throws IOException {
            ClassModel model = parse("packages/nested-packages.puml");
            assertTrue(model.packages.size() >= 4, "Should have nested packages");

            boolean hasNestedPackage = model.packages.stream().anyMatch(p -> p.getParentPackage() != null);
            assertTrue(hasNestedPackage, "Should have child packages with parents");
        }

        @Test
        @DisplayName("container-types: parses different container types")
        void parseContainerTypes() throws IOException {
            ClassModel model = parse("packages/container-types.puml");

            assertEquals(7, model.packages.size(), "Should have 7 container types");

            Set<String> types = model.packages.stream()
                    .map(Package::getType)
                    .collect(Collectors.toSet());

            assertTrue(types.size() >= 3, "Should have different container types");
        }
    }

    @Nested
    @DisplayName("Note Parsing")
    class NoteParsingTests {

        @Test
        @DisplayName("basic-notes: parses single and multiline notes")
        void parseBasicNotes() throws IOException {
            ClassModel model = parse("notes/basic-notes.puml");

            long noteCount = model.entities.stream().filter(e -> "NOTE".equals(e.getType())).count();
            assertTrue(noteCount >= 2, "Should have multiple notes");
        }

        @Test
        @DisplayName("notes-on-links: parses notes on relationships")
        void parseNotesOnLinks() throws IOException {
            ClassModel model = parse("notes/notes-on-links.puml");

            boolean hasNoteOnLink = model.links.stream().anyMatch(l -> l.getNoteOnLink() != null);
            assertTrue(hasNoteOnLink, "Should have note attached to link");
        }

        @Test
        @DisplayName("notes-with-styling: parses colored notes")
        void parseNotesWithStyling() throws IOException {
            ClassModel model = parse("notes/notes-with-styling.puml");

            boolean hasColoredNote = model.entities.stream()
                    .filter(e -> "NOTE".equals(e.getType()))
                    .anyMatch(e -> e.getExplicitBackground() != null);

            assertTrue(hasColoredNote, "Should have notes with background color");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("empty-diagram: handles empty diagram")
        void parseEmptyDiagram() throws IOException {
            ClassModel model = parse("edge-cases/empty-diagram.puml");

            assertNotNull(model);
            assertTrue(model.entities.isEmpty());
            assertTrue(model.links.isEmpty());
            assertTrue(model.packages.isEmpty());
        }

        @Test
        @DisplayName("minimal: parses single class")
        void parseMinimal() throws IOException {
            ClassModel model = parse("edge-cases/minimal.puml");

            assertEquals(1, model.entities.size());
            assertEquals("A", model.entities.getFirst().getName());
        }

        @Test
        @DisplayName("no-startuml: auto-wraps content")
        void parseNoStartUml() throws IOException {
            ClassModel model = parse("edge-cases/no-startuml.puml");

            assertNotNull(model);
            assertFalse(model.entities.isEmpty());
        }

        @Test
        @DisplayName("with-comments: ignores comments")
        void parseWithComments() throws IOException {
            ClassModel model = parse("edge-cases/with-comments.puml");

            assertNotNull(model);
            assertFalse(model.entities.isEmpty());

            boolean hasCommentEntity = model.entities.stream()
                    .anyMatch(e -> e.getName().contains("'"));

            assertFalse(hasCommentEntity, "Comments should not become entities");
        }

        @Test
        @DisplayName("with-separators: parses member separators")
        void parseWithSeparators() throws IOException {
            ClassModel model = parse("edge-cases/with-separators.puml");

            ClassEntity entity = model.entities.getFirst();
            assertFalse(entity.getRawBody().isEmpty());
        }

        @Test
        @DisplayName("special-characters: parses generics, stereotypes, colors")
        void parseSpecialCharacters() throws IOException {
            ClassModel model = parse("edge-cases/special-characters.puml");

            assertTrue(model.entities.stream().anyMatch(ClassEntity::isGeneric), "Should parse generics");
            assertTrue(model.entities.stream().anyMatch(ClassEntity::isStereotype), "Should parse stereotypes");
            assertTrue(model.entities.stream().anyMatch(e -> e.getExplicitBackground() != null), "Should parse colors");
        }

        @Test
        @DisplayName("special-entity-types: parses diamond, circle")
        void parseSpecialEntityTypes() throws IOException {
            ClassModel model = parse("edge-cases/special-entity-types.puml");

            Set<String> types = entityTypes(model);

            assertTrue(types.contains("DIAMOND") || types.contains("CIRCLE"), "Should parse special entity types");
        }
    }

    @Nested
    @DisplayName("Complex Diagrams")
    class ComplexDiagramTests {

        @Test
        @DisplayName("full-diagram: parses all features")
        void parseFullDiagram() throws IOException {
            ClassModel model = parse("complex/full-diagram.puml");

            assertAll(
                    () -> assertTrue(model.entities.size() >= 5, "Should have multiple entities"),
                    () -> assertTrue(model.links.size() >= 3, "Should have multiple links"),
                    () -> assertTrue(model.packages.size() >= 2, "Should have multiple packages"),
                    () -> assertFalse(model.title.isEmpty(), "Should have title"),
                    () -> assertNotNull(model.header, "Should have header"),
                    () -> assertNotNull(model.footer, "Should have footer")
            );
        }

        @Test
        @DisplayName("layered-architecture: parses multi-layer application structure")
        void parseLayeredArchitecture() throws IOException {
            ClassModel model = parse("complex/layered-architecture.puml");

            assertEquals(3, model.packages.size(), "Should have 3 layer packages");

            assertTrue(model.packages.stream().allMatch(p -> p.getBackground() != null),
                    "All packages should have colors");

            Set<String> names = entityNames(model);
            assertAll(
                    () -> assertTrue(names.contains("ApplicationService")),
                    () -> assertTrue(names.contains("User")),
                    () -> assertTrue(names.contains("Order")),
                    () -> assertTrue(names.contains("OrderLine")),
                    () -> assertTrue(names.contains("Product")),
                    () -> assertTrue(names.contains("UserRepository")),
                    () -> assertTrue(names.contains("OrderRepository"))
            );

            Optional<ClassEntity> entity = findEntity(model, "Entity");
            assertTrue(entity.isPresent());
            assertEquals("ABSTRACT_CLASS", entity.get().getType());

            Optional<ClassEntity> orderStatus = findEntity(model, "OrderStatus");
            assertTrue(orderStatus.isPresent());
            assertEquals("ENUM", orderStatus.get().getType());

            Set<String> types = entityTypes(model);
            assertTrue(types.contains("INTERFACE"));

            boolean hasImplements = model.entities.stream()
                    .anyMatch(e -> e.getName().equals("JpaUserRepository") || e.getName().equals("JpaOrderRepository"));
            assertTrue(hasImplements, "Should have implementation classes");

            assertTrue(model.links.size() >= 8, "Should have many relationships");

            boolean hasCardinality = model.links.stream()
                    .anyMatch(l -> l.getQuantifier1() != null || l.getQuantifier2() != null);
            assertTrue(hasCardinality, "Should have relationships with cardinality");

            boolean hasNote = model.entities.stream()
                    .anyMatch(e -> "NOTE".equals(e.getType()));
            assertTrue(hasNote, "Should have note");

            assertFalse(model.title.isEmpty());
            assertFalse(model.header.isEmpty());
            assertFalse(model.footer.isEmpty());
        }

        @Test
        @DisplayName("design-patterns: parses singleton, factory, observer, strategy patterns")
        void parseDesignPatterns() throws IOException {
            ClassModel model = parse("complex/design-patterns.puml");

            assertEquals(4, model.packages.size());

            Set<String> names = entityNames(model);

            assertTrue(names.contains("DatabaseConnection"));
            Optional<ClassEntity> singleton = findEntity(model, "DatabaseConnection");
            assertTrue(singleton.isPresent());
            assertTrue(singleton.get().isStereotype(), "Singleton should have stereotype");

            assertTrue(names.contains("Vehicle"));
            assertTrue(names.contains("Car"));
            assertTrue(names.contains("Motorcycle"));
            assertTrue(names.contains("VehicleFactory"));

            assertTrue(names.contains("Observer"));
            assertTrue(names.contains("Subject"));
            assertTrue(names.contains("ConcreteSubject"));
            assertTrue(names.contains("ConcreteObserver"));

            assertTrue(names.contains("PaymentStrategy"));
            assertTrue(names.contains("CreditCardPayment"));
            assertTrue(names.contains("PayPalPayment"));
            assertTrue(names.contains("ShoppingCart"));

            Set<String> types = entityTypes(model);
            assertTrue(types.contains("INTERFACE"));

            assertTrue(model.links.size() >= 10, "Should have many relationships");

            long noteCount = model.entities.stream()
                    .filter(e -> "NOTE".equals(e.getType()))
                    .count();
            assertTrue(noteCount >= 2, "Should have notes");
        }

        @Test
        @DisplayName("self-references: parses self-referencing structures")
        void parseSelfReferences() throws IOException {
            ClassModel model = parse("complex/self-references.puml");

            Set<String> names = entityNames(model);
            assertAll(
                    () -> assertTrue(names.contains("TreeNode")),
                    () -> assertTrue(names.contains("Employee")),
                    () -> assertTrue(names.contains("LinkedListNode")),
                    () -> assertTrue(names.contains("Category"))
            );

            boolean hasGeneric = model.entities.stream()
                    .anyMatch(ClassEntity::isGeneric);
            assertTrue(hasGeneric, "Should have generic types");

            boolean hasSelfRef = model.links.stream()
                    .anyMatch(l -> l.getEntity1().getName().equals(l.getEntity2().getName()));
            assertTrue(hasSelfRef, "Should have self-referencing relationships");

            boolean hasCardinality = model.links.stream()
                    .anyMatch(l -> l.getQuantifier1() != null || l.getQuantifier2() != null);
            assertTrue(hasCardinality, "Should have cardinality");

            boolean hasNote = model.entities.stream()
                    .anyMatch(e -> "NOTE".equals(e.getType()));
            assertTrue(hasNote, "Should have note");
        }

        @Test
        @DisplayName("complex diagrams have proper source line tracking")
        void complexDiagramsHaveSourceLineTracking() throws IOException {
            ClassModel model = parse("complex/layered-architecture.puml");

            long entitiesWithLines = model.entities.stream()
                    .filter(e -> !"NOTE".equals(e.getType()))
                    .filter(ClassEntity::hasLine)
                    .count();

            assertTrue(entitiesWithLines >= 5, "Most entities should have source line info");

            long packagesWithLines = model.packages.stream()
                    .filter(Package::hasLine)
                    .count();

            assertTrue(packagesWithLines >= 2, "Packages should have source line info");
        }
    }

    @Nested
    @DisplayName("Parser State")
    class ParserStateTests {

        @Test
        @DisplayName("parser resets state between parses")
        void parserResetsState() throws IOException {
            ClassModel model1 = parse("entities/simple-class.puml");
            ClassModel model2 = parse("edge-cases/minimal.puml");

            assertNotSame(model1, model2);
            assertEquals("ent-0", model2.entities.getFirst().getId(), "IDs should reset");
        }

        @Test
        @DisplayName("model has line mapper set")
        void modelHasLineMapper() throws IOException {
            ClassModel model = parse("entities/simple-class.puml");

            assertNotNull(model.getLineMapper());
        }

        @Test
        @DisplayName("model has line finder set")
        void modelHasLineFinder() throws IOException {
            ClassModel model = parse("entities/simple-class.puml");

            assertNotNull(model.getLineFinder());
        }
    }
}
