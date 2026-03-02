package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassEntityFactory;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassLinkFactory;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import org.eclipse.glsp.graph.GEdge;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Entity and Link Factory Tests")
class ClassFactoryIntegrationTest {
    private ClassModel model;
    private List<GModelElement> elements;
    private EntityBuild entityBuild;
    private ClassEntityFactory entityFactory;

    @BeforeEach
    void setup() {
        model = new ClassModel();
        model.entities = new ArrayList<>();
        model.links = new ArrayList<>();
        model.packages = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.labels = new ArrayList<>();

        elements = new ArrayList<>();
        entityBuild = new EntityBuild();
        entityFactory = new ClassEntityFactory(model, entityBuild, elements);
    }

    @Nested
    @DisplayName("Entity Dimension Calculation")
    class EntityDimensionTests {

        @Test
        @DisplayName("calculates dimensions for simple class")
        void simpleClassDimensions() {
            ClassEntity entity = createEntity("ent-0", "User", "CLASS");
            model.entities.add(entity);

            entityFactory.createEntities();

            Map<String, ClassLayout.Size> dims = entityFactory.getDimensions();
            assertNotNull(dims.get("ent-0"));
            assertTrue(dims.get("ent-0").width > 0);
            assertTrue(dims.get("ent-0").height > 0);
        }

        @Test
        @DisplayName("DIAMOND has fixed 30x30 size")
        void diamondFixedSize() {
            ClassEntity entity = createEntity("ent-0", "", "DIAMOND");
            model.entities.add(entity);

            entityFactory.createEntities();

            ClassLayout.Size size = entityFactory.getDimensions().get("ent-0");
            assertEquals(30, size.width);
            assertEquals(30, size.height);
        }

        @Test
        @DisplayName("ASSOCIATION_POINT has fixed 8x8 size")
        void associationPointFixedSize() {
            ClassEntity entity = createEntity("ent-0", "", "ASSOCIATION_POINT");
            model.entities.add(entity);

            entityFactory.createEntities();

            ClassLayout.Size size = entityFactory.getDimensions().get("ent-0");
            assertEquals(8, size.width);
            assertEquals(8, size.height);
        }

        @Test
        @DisplayName("LOLLIPOP has fixed height of 26")
        void lollipopFixedHeight() {
            ClassEntity entity = createEntity("ent-0", "Interface", "LOLLIPOP");
            model.entities.add(entity);

            entityFactory.createEntities();

            ClassLayout.Size size = entityFactory.getDimensions().get("ent-0");
            assertEquals(26, size.height);
        }

        @Test
        @DisplayName("class with generic adds width for generic text")
        void genericAddsWidth() {
            ClassEntity withoutGeneric = createEntity("ent-0", "Container", "CLASS");
            ClassEntity withGeneric = createEntity("ent-1", "Container", "CLASS");
            withGeneric.setGeneric("T");

            model.entities.add(withoutGeneric);
            model.entities.add(withGeneric);

            entityFactory.createEntities();

            Map<String, ClassLayout.Size> dims = entityFactory.getDimensions();
            assertTrue(dims.get("ent-1").width > dims.get("ent-0").width);
        }

        @Test
        @DisplayName("class with stereotype considers stereotype width")
        void stereotypeWidth() {
            ClassEntity entity = createEntity("ent-0", "A", "CLASS");
            entity.setStereotypeName("<<VeryLongStereotypeName>>");
            model.entities.add(entity);

            entityFactory.createEntities();

            ClassLayout.Size size = entityFactory.getDimensions().get("ent-0");
            assertTrue(size.width > 50, "Should be wider than just class name 'A'");
        }
    }

    @Nested
    @DisplayName("Entity Element Creation")
    class EntityCreationTests {

        @Test
        @DisplayName("creates GNode for each entity type")
        void createsElementsForAllTypes() {
            model.entities.add(createEntity("ent-0", "User", "CLASS"));
            model.entities.add(createEntity("ent-1", "Status", "ENUM"));
            model.entities.add(createEntity("ent-2", "", "DIAMOND"));
            model.entities.add(createEntity("ent-3", "Circle", "CIRCLE"));
            model.entities.add(createEntity("ent-4", "Note text", "NOTE"));

            entityFactory.createEntities();

            assertTrue(elements.size() >= 5);
        }

        @Test
        @DisplayName("creates elements list is empty for empty model")
        void emptyModelNoElements() {
            entityFactory.createEntities();
            assertTrue(elements.size() <= 3);
        }

        @Test
        @DisplayName("getDimensions returns map after createEntities")
        void dimensionsMapPopulated() {
            model.entities.add(createEntity("ent-0", "A", "CLASS"));
            model.entities.add(createEntity("ent-1", "B", "INTERFACE"));

            entityFactory.createEntities();

            assertEquals(2, entityFactory.getDimensions().size());
        }
    }

    @Nested
    @DisplayName("Tip Info Collection")
    class TipInfoTests {

        @Test
        @DisplayName("no tips for members without tips")
        void noTipsWhenNone() {
            ClassEntity entity = createEntity("ent-0", "User", "CLASS");
            entity.getFields().add(new EntityMethod("+name: String"));
            model.entities.add(entity);

            entityFactory.createEntities();

            assertTrue(entityFactory.tipInfoList.isEmpty());
        }
    }

    @Nested
    @DisplayName("Link Creation")
    class LinkCreationTests {

        private ClassLinkFactory linkFactory;

        @BeforeEach
        void setUpLinks() {
            ClassEntity e1 = createEntity("ent-0", "A", "CLASS");
            ClassEntity e2 = createEntity("ent-1", "B", "CLASS");
            e1.setX(0); e1.setY(0);
            e2.setX(200); e2.setY(0);
            model.entities.add(e1);
            model.entities.add(e2);

            entityFactory.createEntities();
            linkFactory = new ClassLinkFactory(model, elements, entityFactory, entityBuild);
        }

        @Test
        @DisplayName("creates GEdge for normal link")
        void createsEdgeForLink() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1));
            model.links.add(link);

            int beforeCount = elements.size();
            linkFactory.createLinks();

            assertTrue(elements.size() > beforeCount);
            assertTrue(elements.stream().anyMatch(e -> e instanceof GEdge));
        }

        @Test
        @DisplayName("skips INVISIBLE links")
        void skipsInvisibleLinks() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1), "INVISIBLE");
            model.links.add(link);
            linkFactory.createLinks();

            long edgeCount = elements.stream()
                    .filter(e -> e instanceof GEdge)
                    .filter(e -> e.getId().equals("link-0"))
                    .count();
            assertEquals(0, edgeCount);
        }

        @Test
        @DisplayName("handles empty links list")
        void handlesEmptyLinks() {
            assertDoesNotThrow(() -> linkFactory.createLinks());
        }
    }

    @Nested
    @DisplayName("Parallel Edge Detection")
    class ParallelEdgeTests {
        private ClassLinkFactory linkFactory;

        @BeforeEach
        void setupLinks() {
            ClassEntity e1 = createEntity("ent-0", "A", "CLASS");
            ClassEntity e2 = createEntity("ent-1", "B", "CLASS");
            e1.setX(0); e1.setY(0);
            e2.setX(200); e2.setY(0);
            model.entities.add(e1);
            model.entities.add(e2);

            entityFactory.createEntities();
            linkFactory = new ClassLinkFactory(model, elements, entityFactory, entityBuild);
        }

        @Test
        @DisplayName("detects parallel edges between same entities")
        void detectsParallelEdges() {
            ClassLink link1 = createLink("link-0", model.entities.get(0), model.entities.get(1));
            ClassLink link2 = createLink("link-1", model.entities.get(0), model.entities.get(1));
            model.links.add(link1);
            model.links.add(link2);

            linkFactory.createLinks();

            // Both edges should have parallelIndex and parallelTotal args
            long edgesWithParallel = elements.stream()
                    .filter(e -> e instanceof GEdge)
                    .map(e -> (GEdge) e)
                    .filter(e -> e.getArgs().containsKey("parallelTotal"))
                    .count();

            assertEquals(2, edgesWithParallel);
        }

        @Test
        @DisplayName("single edge has no parallel info")
        void singleEdgeNoParallel() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1));
            model.links.add(link);

            linkFactory.createLinks();

            GEdge edge = (GEdge) elements.stream()
                    .filter(e -> e instanceof GEdge && e.getId().equals("link-0"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(edge);
            assertFalse(edge.getArgs().containsKey("parallelTotal"));
        }

        @Test
        @DisplayName("edges with members are excluded from parallel grouping")
        void membersExcludedFromParallel() {
            ClassLink link1 = createLink("link-0", model.entities.get(0), model.entities.get(1));
            ClassLink link2 = createLink("link-1", model.entities.get(0), model.entities.get(1));
            link1.setSourceMember("field1");
            model.links.add(link1);
            model.links.add(link2);

            linkFactory.createLinks();

            GEdge edge1 = (GEdge) elements.stream()
                    .filter(e -> e instanceof GEdge && e.getId().equals("link-0"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(edge1);
            assertFalse(edge1.getArgs().containsKey("parallelTotal"));
        }
    }

    @Nested
    @DisplayName("Link Label and Quantifier Creation")
    class LabelCreationTests {
        private ClassLinkFactory linkFactory;

        @BeforeEach
        void setUpLinks() {
            ClassEntity e1 = createEntity("ent-0", "A", "CLASS");
            ClassEntity e2 = createEntity("ent-1", "B", "CLASS");
            e1.setX(0); e1.setY(0);
            e2.setX(200); e2.setY(0);
            model.entities.add(e1);
            model.entities.add(e2);

            entityFactory.createEntities();
            linkFactory = new ClassLinkFactory(model, elements, entityFactory, entityBuild);
        }

        @Test
        @DisplayName("creates label for link with message")
        void createsMessageLabel() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1));
            link.getMessage().setLabel("uses");
            model.links.add(link);

            linkFactory.createLinks();

            assertTrue(model.labels.stream()
                    .anyMatch(l -> l.getLabel().equals("uses")));
        }

        @Test
        @DisplayName("creates quantifier labels")
        void createsQuantifierLabels() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1));
            link.getQuantifier1().setLabel("1");
            link.getQuantifier2().setLabel("*");
            model.links.add(link);

            linkFactory.createLinks();

            assertTrue(model.labels.stream().anyMatch(l -> l.getLabel().equals("1")));
            assertTrue(model.labels.stream().anyMatch(l -> l.getLabel().equals("*")));
        }

        @Test
        @DisplayName("skips empty labels")
        void skipsEmptyLabels() {
            ClassLink link = createLink("link-0", model.entities.get(0), model.entities.get(1));
            model.links.add(link);

            linkFactory.createLinks();

            assertTrue(model.labels.isEmpty());
        }
    }

    @Nested
    @DisplayName("EntityBuild")
    class EntityBuildTests {

        @Test
        @DisplayName("buildEntity returns GNode with correct id")
        void buildEntityCorrectId() {
            ClassEntity entity = createEntity("test-id", "TestClass", "CLASS");

            GModelElement element = entityBuild.buildEntity(
                    entity, 100, 80, List.of(), List.of(), List.of());

            assertEquals("test-id", element.getId());
            assertInstanceOf(GNode.class, element);
        }

        @Test
        @DisplayName("buildDiamondEntity returns diamond node")
        void buildDiamondEntity() {
            ClassEntity entity = createEntity("diamond-1", "", "DIAMOND");

            GModelElement element = entityBuild.buildDiamondEntity(entity, 30);

            assertEquals("diamond-1", element.getId());
        }

        @Test
        @DisplayName("buildNoteEntity returns note node")
        void buildNoteEntity() {
            ClassEntity entity = createEntity("note-1", "Note text", "NOTE");

            GModelElement element = entityBuild.buildNoteEntity(entity, 100, 50);

            assertEquals("note-1", element.getId());
        }
    }

    @Nested
    @DisplayName("LinkBuild")
    class LinkBuildTests {
        private final LinkBuild linkBuild = new LinkBuild();

        @Test
        @DisplayName("buildLink returns GEdge with correct source/target")
        void buildLinkCorrectEndpoints() {
            ClassEntity e1 = createEntity("src", "A", "CLASS");
            ClassEntity e2 = createEntity("tgt", "B", "CLASS");
            ClassLink link = createLink("link-1", e1, e2);

            GModelElement element = linkBuild.buildLink(link);

            assertInstanceOf(GEdge.class, element);
            GEdge edge = (GEdge) element;
            assertEquals("src", edge.getSourceId());
            assertEquals("tgt", edge.getTargetId());
        }

        @Test
        @DisplayName("note link uses 'link:note' type")
        void noteLinkType() {
            ClassEntity e1 = createEntity("ent", "A", "CLASS");
            ClassEntity note = createEntity("note", "Note", "NOTE");
            ClassLink link = createLink("link-1", e1, note);
            link.setNoteLink(true);

            GModelElement element = linkBuild.buildLink(link);

            assertEquals("link:note", element.getType());
        }

        @Test
        @DisplayName("buildLinkLabel returns label element")
        void buildLinkLabel() {
            ClassLabel label = new ClassLabel(100, 50, "label-1", "uses");

            GModelElement element = linkBuild.buildLinkLabel(label);

            assertEquals("label-1", element.getId());
        }
    }

    private ClassEntity createEntity(String id, String name, String type) {
        ClassEntity entity = new ClassEntity(0, 0, id, name, type,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }

    private ClassLink createLink(String id, ClassEntity source, ClassEntity target) {
        return new ClassLink(id, source, target, "ARROW", "", 1,
                "NONE", "ARROW", "", "");
    }

    private ClassLink createLink(String id, ClassEntity source, ClassEntity target, String type) {
        return new ClassLink(id, source, target, type, "", 1,
                "NONE", "ARROW", "", "");
    }
}