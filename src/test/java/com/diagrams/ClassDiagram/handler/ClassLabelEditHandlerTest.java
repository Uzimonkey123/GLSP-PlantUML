/*
 * File: ClassLabelEditHandlerTest.java
 * Author: Norman Babiak
 * Description: Tests for changing labels in the diagram
 * Date: 28.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelIndex;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassLabelEditHandler Tests")
class ClassLabelEditHandlerTest {
    private ClassLabelEditHandler handler;
    private ClassModel model;
    @Mock private ClassModelState mockModelState;
    @Mock private GModelIndex mockIndex;
    @Mock private GLabel mockLabel;
    @Mock private ApplyLabelEditOperation mockOp;

    @BeforeEach
    void setup() throws Exception {
        handler = new ClassLabelEditHandler();
        model = new ClassModel();
        model.entities = new ArrayList<>();
        model.links = new ArrayList<>();
        model.packages = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.labels = new ArrayList<>();

        Field field = handler.getClass().getSuperclass().getSuperclass().getDeclaredField("modelState");
        field.setAccessible(true);
        field.set(handler, mockModelState);

        when(mockModelState.getModel()).thenReturn(model);
        when(mockModelState.getIndex()).thenReturn(mockIndex);
    }

    private void label(String id, String text) {
        when(mockLabel.getId()).thenReturn(id);
        when(mockLabel.getText()).thenReturn(text);
        when(mockIndex.getByClass(id, GLabel.class)).thenReturn(Optional.of(mockLabel));
        when(mockOp.getLabelId()).thenReturn(id);
    }

    private ClassEntity entity(String id, String name) {
        ClassEntity entity = new ClassEntity(0, 0, id, name, "CLASS", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }

    @Test
    @DisplayName("updates entity name, stereotype, and generic")
    void entityNameStereotypeGeneric() {
        ClassEntity entity = entity("ent-0", "Old");
        entity.setStereotypeName("<<old>>");
        entity.setGeneric("T");
        model.entities.add(entity);

        label("ent-0-label-name", "Old");
        when(mockOp.getText()).thenReturn("New");
        handler.createCommand(mockOp);
        assertEquals("New", entity.getName());

        label("ent-0-label-stereotype", "<<old>>");
        when(mockOp.getText()).thenReturn("<<new>>");
        handler.createCommand(mockOp);
        assertEquals("<<new>>", entity.getStereotypeName());

        label("ent-0-generic", "T");
        when(mockOp.getText()).thenReturn("K, V");
        handler.createCommand(mockOp);
        assertEquals("K, V", entity.getGeneric());
        assertTrue(entity.isModified());
    }

    @Test
    @DisplayName("updates entity field and method with visibility parsing")
    void entityMembers() {
        ClassEntity entity = entity("ent-0", "User");
        EntityMethod field = new EntityMethod("+name: String");
        EntityMethod method = new EntityMethod("+getName(): String");
        entity.getFields().add(field);
        entity.getRawBody().add(field);
        entity.getMethods().add(method);
        entity.getRawBody().add(method);
        model.entities.add(entity);

        label("ent-0-field-0", "+name: String");
        when(mockOp.getText()).thenReturn("+fullName: String");
        handler.createCommand(mockOp);
        assertEquals("fullName: String", entity.getFields().getFirst().getMethodName());
        assertEquals("public", entity.getFields().getFirst().getVisibilityChar());

        label("ent-0-method-0", "+getName(): String");
        when(mockOp.getText()).thenReturn("-getFullName(): String");
        handler.createCommand(mockOp);
        assertEquals("getFullName(): String", entity.getMethods().getFirst().getMethodName());
        assertEquals("private", entity.getMethods().getFirst().getVisibilityChar());
    }

    @Test
    @DisplayName("updates link message, source and target qualifiers")
    void linkLabelsAndQualifiers() {
        ClassEntity entity1 = entity("ent-0", "A");
        ClassEntity entity2 = entity("ent-1", "B");

        ClassLink link = new ClassLink("link-0", entity1, entity2, "ARROW", "", 1, "NONE", "ARROW", "", "");
        link.setSourceQualifier("old-src");
        link.setTargetQualifier("old-tgt");
        model.links.add(link);

        ClassLabel label = new ClassLabel(0, 0, "link-0-msg", "old"); model.labels.add(label);

        label("link-0-msg", "old");
        when(mockOp.getText()).thenReturn("uses");
        handler.createCommand(mockOp);
        assertEquals("uses", label.getLabel());

        label("link-qual-src-link-0", "old-src");
        when(mockOp.getText()).thenReturn("key");
        handler.createCommand(mockOp);
        assertEquals("key", link.getSourceQualifier());

        label("link-qual-tgt-link-0", "old-tgt");
        when(mockOp.getText()).thenReturn("value");
        handler.createCommand(mockOp);
        assertEquals("value", link.getTargetQualifier());
    }

    @Test
    @DisplayName("updates note text")
    void noteText() {
        ClassEntity note = entity("note-0", "Old note");
        model.notes.add(note);

        label("note-0-label-name", "Old note");
        when(mockOp.getText()).thenReturn("New note");

        handler.createCommand(mockOp);
        assertEquals("New note", note.getName());
    }

    @Test
    @DisplayName("updates package name")
    void packageName() {
        Package pkg = new Package("pkg-0", "old", "folder");
        model.packages.add(pkg);

        label("pkg-0-name", "old");
        when(mockOp.getText()).thenReturn("new");

        handler.createCommand(mockOp);
        assertEquals("new", pkg.getName());
    }

    @Test
    @DisplayName("updates title, header, footer")
    void pageDetails() {
        model.title = "T";
        model.header = "H";
        model.footer = "F";

        label("title", "T");
        when(mockOp.getText()).thenReturn("T2");
        handler.createCommand(mockOp);

        label("header", "H");
        when(mockOp.getText()).thenReturn("H2");
        handler.createCommand(mockOp);

        label("footer", "F");
        when(mockOp.getText()).thenReturn("F2");
        handler.createCommand(mockOp);

        assertEquals("T2", model.title);
        assertEquals("H2", model.header);
        assertEquals("F2", model.footer);
    }

    @Test
    @DisplayName("throws when label not found")
    void labelNotFound() {
        when(mockOp.getLabelId()).thenReturn("x");
        when(mockIndex.getByClass("x", GLabel.class)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> handler.createCommand(mockOp));
    }
}