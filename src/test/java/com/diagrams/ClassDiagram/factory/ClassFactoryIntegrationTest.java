/*
 * File: ClassFactoryIntegrationTest.java
 * Author: Norman Babiak
 * Description: Integration tests for class factory
 * Date: 28.4.2026
 */

package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.builders.EntityBuild;
import com.diagrams.ClassDiagram.builders.LinkBuild;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassEntityFactory;
import com.diagrams.ClassDiagram.factory.ClassParts.ClassLinkFactory;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import org.eclipse.glsp.graph.GEdge;
import org.eclipse.glsp.graph.GModelElement;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Class Factory Tests")
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

    private ClassEntity entity(String id, String name, String type) {
        ClassEntity entity = new ClassEntity(1, 1, id, name, type, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }

    private ClassLink link(String id, ClassEntity src, ClassEntity tgt) {
        return new ClassLink(id, src, tgt, "ARROW", "", 1, "NONE", "ARROW", "", "");
    }

    private ClassLink link(String id, ClassEntity src, ClassEntity tgt, String type) {
        return new ClassLink(id, src, tgt, type, "", 1, "NONE", "ARROW", "", "");
    }

    @Test
    @DisplayName("class with generic is wider than without")
    void genericAddsWidth() {
        ClassEntity plain = entity("entity0", "Box", "CLASS");
        ClassEntity gen = entity("entity1", "Box", "CLASS");
        gen.setGeneric("T");
        model.entities.add(plain); model.entities.add(gen);
        entityFactory.createEntities();

        assertTrue(entityFactory.getDimensions().get("entity1").width > entityFactory.getDimensions().get("entity0").width);
    }

    @Test
    @DisplayName("stereotype widens entity beyond short name")
    void stereotypeWidth() {
        ClassEntity entity = entity("entity0", "A", "CLASS"); entity.setStereotypeName("<<VeryLongStereotypeName>>");
        model.entities.add(entity);
        entityFactory.createEntities();

        assertTrue(entityFactory.getDimensions().get("entity0").width > 50);
    }

    @Test
    @DisplayName("creates elements for all entity types")
    void createsAllEntityTypes() {
        model.entities.add(entity("entity0", "User", "CLASS"));
        model.entities.add(entity("entity1", "Status", "ENUM"));
        model.entities.add(entity("entity2", "", "DIAMOND"));
        model.entities.add(entity("entity3", "C", "CIRCLE"));
        model.entities.add(entity("entity4", "Note text", "NOTE"));
        entityFactory.createEntities();

        assertTrue(elements.size() >= 5);
    }

    @Test
    @DisplayName("no tips for members without tips")
    void noTips() {
        ClassEntity entity = entity("e0", "User", "CLASS");
        entity.getFields().add(new EntityMethod("+name: String"));
        model.entities.add(entity);
        entityFactory.createEntities();

        assertTrue(entityFactory.tipInfoList.isEmpty());
    }

    @Test
    @DisplayName("creates GEdge for link, skips invisible ones")
    void linksAndInvisible() {
        ClassEntity entity1 = entity("entity0", "A", "CLASS");
        entity1.setX(1);
        entity1.setY(1);

        ClassEntity entity2 = entity("entity1", "B", "CLASS");
        entity2.setX(200);
        entity2.setY(1);

        model.entities.add(entity1); model.entities.add(entity2);
        entityFactory.createEntities();
        ClassLinkFactory lf = new ClassLinkFactory(model, elements, entityFactory, entityBuild);

        model.links.add(link("vis", entity1, entity2));
        model.links.add(link("inv", entity1, entity2, "INVISIBLE"));
        lf.createLinks();

        assertTrue(elements.stream().anyMatch(e -> e instanceof GEdge && e.getId().equals("vis")));
        assertFalse(elements.stream().anyMatch(e -> e instanceof GEdge && e.getId().equals("inv")));
    }

    @Test
    @DisplayName("detects parallel edges between same entities")
    void parallelEdges() {
        ClassEntity entity1 = entity("entity0", "A", "CLASS");
        entity1.setX(1);
        entity1.setY(1);

        ClassEntity entity2 = entity("entity1", "B", "CLASS");
        entity2.setX(200);
        entity2.setY(1);

        model.entities.add(entity1);
        model.entities.add(entity2);
        entityFactory.createEntities();

        model.links.add(link("link0", entity1, entity2));
        model.links.add(link("link1", entity1, entity2));
        new ClassLinkFactory(model, elements, entityFactory, entityBuild).createLinks();

        assertEquals(2, elements.stream().filter(e -> e instanceof GEdge).map(e -> (GEdge) e)
                .filter(e -> e.getArgs().containsKey("parallelTotal")).count());
    }

    @Test
    @DisplayName("creates message and quantifier labels")
    void labels() {
        ClassEntity entity1 = entity("entity0", "A", "CLASS");
        entity1.setX(1);
        entity1.setY(1);

        ClassEntity entity2 = entity("entity1", "B", "CLASS");
        entity2.setX(200);
        entity2.setY(1);

        model.entities.add(entity1);
        model.entities.add(entity2);
        entityFactory.createEntities();

        ClassLink link = link("link0", entity1, entity2);
        link.getMessage().setLabel("uses");
        link.getQuantifier1().setLabel("1");
        link.getQuantifier2().setLabel("*");
        model.links.add(link);

        new ClassLinkFactory(model, elements, entityFactory, entityBuild).createLinks();

        assertTrue(model.labels.stream().anyMatch(la -> la.getLabel().equals("uses")));
        assertTrue(model.labels.stream().anyMatch(la -> la.getLabel().equals("1")));
        assertTrue(model.labels.stream().anyMatch(la -> la.getLabel().equals("*")));
    }

    @Test
    @DisplayName("buildEntity returns GNode with correct id, buildLink returns correct endpoints")
    void builders() {
        ClassEntity entity = entity("test-id", "TestClass", "CLASS");
        assertEquals("test-id", entityBuild.buildEntity(entity, 100, 80, List.of(), List.of(), List.of()).getId());

        ClassEntity src = entity("src", "A", "CLASS");
        ClassEntity tgt = entity("tgt", "B", "CLASS");
        GEdge edge = (GEdge) new LinkBuild().buildLink(link("link", src, tgt));

        assertEquals("src", edge.getSourceId());
        assertEquals("tgt", edge.getTargetId());
    }
}
