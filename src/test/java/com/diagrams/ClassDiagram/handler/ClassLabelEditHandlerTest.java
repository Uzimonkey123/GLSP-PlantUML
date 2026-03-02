package com.diagrams.ClassDiagram.handler;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.*;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import com.diagrams.ClassDiagram.state.ClassModelState;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelIndex;
import org.eclipse.glsp.server.features.directediting.ApplyLabelEditOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    @Mock private ApplyLabelEditOperation mockOperation;

    @BeforeEach
    void setup() throws Exception {
        handler = new ClassLabelEditHandler();

        model = new ClassModel();
        model.entities = new ArrayList<>();
        model.links = new ArrayList<>();
        model.packages = new ArrayList<>();
        model.notes = new ArrayList<>();
        model.labels = new ArrayList<>();

        Field modelStateField = handler.getClass().getSuperclass().getSuperclass().getDeclaredField("modelState");
        modelStateField.setAccessible(true);
        modelStateField.set(handler, mockModelState);

        when(mockModelState.getModel()).thenReturn(model);
        when(mockModelState.getIndex()).thenReturn(mockIndex);
    }

    private void setupLabel(String labelId, String currentText) {
        when(mockLabel.getId()).thenReturn(labelId);
        when(mockLabel.getText()).thenReturn(currentText);
        when(mockIndex.getByClass(labelId, GLabel.class)).thenReturn(Optional.of(mockLabel));
        when(mockOperation.getLabelId()).thenReturn(labelId);
    }

    @Nested
    @DisplayName("Entity Labels")
    class EntityLabelTests {

        @Test
        @DisplayName("updates entity name")
        void updatesEntityName() {
            ClassEntity entity = createEntity("ent-0", "OldName");
            model.entities.add(entity);

            setupLabel("ent-0-label-name", "OldName");
            when(mockOperation.getText()).thenReturn("NewName");

            handler.createCommand(mockOperation);

            assertEquals("NewName", entity.getName());
            assertTrue(entity.isModified());
        }

        @Test
        @DisplayName("updates entity stereotype")
        void updatesEntityStereotype() {
            ClassEntity entity = createEntity("ent-0", "MyClass");
            entity.setStereotypeName("<<old>>");
            model.entities.add(entity);

            setupLabel("ent-0-label-stereotype", "<<old>>");
            when(mockOperation.getText()).thenReturn("<<service>>");

            handler.createCommand(mockOperation);

            assertEquals("<<service>>", entity.getStereotypeName());
            assertTrue(entity.isModified());
        }

        @Test
        @DisplayName("updates entity generic")
        void updatesEntityGeneric() {
            ClassEntity entity = createEntity("ent-0", "Container");
            entity.setGeneric("T");
            model.entities.add(entity);

            setupLabel("ent-0-generic", "T");
            when(mockOperation.getText()).thenReturn("K, V");

            handler.createCommand(mockOperation);

            assertEquals("K, V", entity.getGeneric());
            assertTrue(entity.isModified());
        }

        @Test
        @DisplayName("updates entity field")
        void updatesEntityField() {
            ClassEntity entity = createEntity("ent-0", "User");
            EntityMethod field = new EntityMethod("+name: String");
            entity.getFields().add(field);
            entity.getRawBody().add(field);
            model.entities.add(entity);

            setupLabel("ent-0-field-0", "+name: String");
            when(mockOperation.getText()).thenReturn("+fullName: String");

            handler.createCommand(mockOperation);

            assertEquals("fullName: String", entity.getFields().getFirst().getMethodName());
            assertEquals("public", entity.getFields().getFirst().getVisibilityChar());
            assertTrue(entity.isModified());
        }

        @Test
        @DisplayName("updates entity method")
        void updatesEntityMethod() {
            ClassEntity entity = createEntity("ent-0", "User");
            EntityMethod method = new EntityMethod("+getName(): String");
            entity.getMethods().add(method);
            entity.getRawBody().add(method);
            model.entities.add(entity);

            setupLabel("ent-0-method-0", "+getName(): String");
            when(mockOperation.getText()).thenReturn("+getFullName(): String");

            handler.createCommand(mockOperation);

            assertEquals("getFullName(): String", entity.getMethods().getFirst().getMethodName());
            assertEquals("public", entity.getMethods().getFirst().getVisibilityChar());
            assertTrue(entity.isModified());
        }

        @Test
        @DisplayName("ignores invalid field index")
        void ignoresInvalidFieldIndex() {
            ClassEntity entity = createEntity("ent-0", "User");
            model.entities.add(entity);

            setupLabel("ent-0-field-99", "invalid");
            when(mockOperation.getText()).thenReturn("new");

            assertDoesNotThrow(() -> handler.createCommand(mockOperation));
        }

        @Test
        @DisplayName("ignores invalid method index")
        void ignoresInvalidMethodIndex() {
            ClassEntity entity = createEntity("ent-0", "User");
            model.entities.add(entity);

            setupLabel("ent-0-method-99", "invalid");
            when(mockOperation.getText()).thenReturn("new");

            assertDoesNotThrow(() -> handler.createCommand(mockOperation));
        }
    }

    @Nested
    @DisplayName("Link Labels")
    class LinkLabelTests {

        @Test
        @DisplayName("updates link message label")
        void updatesLinkLabel() {
            ClassLabel linkLabel = new ClassLabel(0, 0, "link-0-msg", "old");
            model.labels.add(linkLabel);

            setupLabel("link-0-msg", "old");
            when(mockOperation.getText()).thenReturn("uses");

            handler.createCommand(mockOperation);

            assertEquals("uses", linkLabel.getLabel());
            assertTrue(linkLabel.isModified());
        }

        @Test
        @DisplayName("updates source qualifier")
        void updatesSourceQualifier() {
            ClassEntity e1 = createEntity("ent-0", "A");
            ClassEntity e2 = createEntity("ent-1", "B");
            ClassLink link = new ClassLink("link-0", e1, e2, "ARROW", "", 1, "NONE", "ARROW", "", "");
            link.setSourceQualifier("old");
            model.links.add(link);

            setupLabel("link-qual-src-link-0", "old");
            when(mockOperation.getText()).thenReturn("key");

            handler.createCommand(mockOperation);

            assertEquals("key", link.getSourceQualifier());
            assertTrue(link.isModified());
        }

        @Test
        @DisplayName("updates target qualifier")
        void updatesTargetQualifier() {
            ClassEntity e1 = createEntity("ent-0", "A");
            ClassEntity e2 = createEntity("ent-1", "B");
            ClassLink link = new ClassLink("link-0", e1, e2, "ARROW", "", 1, "NONE", "ARROW", "", "");
            link.setTargetQualifier("old");
            model.links.add(link);

            setupLabel("link-qual-tgt-link-0", "old");
            when(mockOperation.getText()).thenReturn("value");

            handler.createCommand(mockOperation);

            assertEquals("value", link.getTargetQualifier());
            assertTrue(link.isModified());
        }
    }

    @Nested
    @DisplayName("Note Labels")
    class NoteLabelTests {

        @Test
        @DisplayName("updates note text")
        void updatesNoteText() {
            ClassEntity note = createEntity("note-0", "Old note");
            model.notes.add(note);

            setupLabel("note-0-label-name", "Old note");
            when(mockOperation.getText()).thenReturn("New note text");

            handler.createCommand(mockOperation);

            assertEquals("New note text", note.getName());
            assertTrue(note.isModified());
        }
    }

    @Nested
    @DisplayName("Package Labels")
    class PackageLabelTests {

        @Test
        @DisplayName("updates package name")
        void updatesPackageName() {
            Package pkg = new Package("pkg-0", "oldpackage", "folder");
            model.packages.add(pkg);

            setupLabel("pkg-0-name", "oldpackage");
            when(mockOperation.getText()).thenReturn("newpackage");

            handler.createCommand(mockOperation);

            assertEquals("newpackage", pkg.getName());
            assertTrue(pkg.isModified());
        }
    }

    @Nested
    @DisplayName("Page Details")
    class PageDetailsTests {

        @Test
        @DisplayName("updates title")
        void updatesTitle() {
            model.title = "Old Title";

            setupLabel("title", "Old Title");
            when(mockOperation.getText()).thenReturn("New Title");

            handler.createCommand(mockOperation);

            assertEquals("New Title", model.title);
            assertTrue(model.titleModified);
        }

        @Test
        @DisplayName("updates header")
        void updatesHeader() {
            model.header = "Old Header";

            setupLabel("header", "Old Header");
            when(mockOperation.getText()).thenReturn("New Header");

            handler.createCommand(mockOperation);

            assertEquals("New Header", model.header);
            assertTrue(model.headerModified);
        }

        @Test
        @DisplayName("updates footer")
        void updatesFooter() {
            model.footer = "Old Footer";

            setupLabel("footer", "Old Footer");
            when(mockOperation.getText()).thenReturn("New Footer");

            handler.createCommand(mockOperation);

            assertEquals("New Footer", model.footer);
            assertTrue(model.footerModified);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("throws when label not found")
        void throwsWhenLabelNotFound() {
            when(mockOperation.getLabelId()).thenReturn("nonexistent");
            when(mockIndex.getByClass("nonexistent", GLabel.class)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> handler.createCommand(mockOperation));
        }

        @Test
        @DisplayName("returns empty command when text unchanged")
        void returnsEmptyCommandWhenUnchanged() {
            ClassEntity entity = createEntity("ent-0", "SameName");
            model.entities.add(entity);

            setupLabel("ent-0-label-name", "SameName");
            when(mockOperation.getText()).thenReturn("SameName");

            Optional<Command> command = handler.createCommand(mockOperation);

            assertTrue(command.isEmpty() || !command.get().canExecute());
        }

        @Test
        @DisplayName("handles entity with multiple fields and methods")
        void handlesMultipleFieldsAndMethods() {
            ClassEntity entity = createEntity("ent-0", "User");
            entity.getFields().add(new EntityMethod("+field0"));
            entity.getFields().add(new EntityMethod("+field1"));
            entity.getMethods().add(new EntityMethod("+method0()"));
            entity.getMethods().add(new EntityMethod("+method1()"));
            entity.getRawBody().add(new EntityMethod("+field0"));
            entity.getRawBody().add(new EntityMethod("+field1"));
            entity.getRawBody().add(new EntityMethod("+method0()"));
            entity.getRawBody().add(new EntityMethod("+method1()"));
            model.entities.add(entity);

            setupLabel("ent-0-field-1", "+field1");
            when(mockOperation.getText()).thenReturn("+updatedField1");

            handler.createCommand(mockOperation);

            assertEquals("updatedField1", entity.getFields().get(1).getMethodName());
            assertEquals("public", entity.getFields().get(1).getVisibilityChar());
            assertEquals("field0", entity.getFields().get(0).getMethodName());
        }

        @Test
        @DisplayName("ignores malformed field suffix")
        void ignoresMalformedSuffix() {
            ClassEntity entity = createEntity("ent-0", "User");
            model.entities.add(entity);

            setupLabel("ent-0-field-abc", "invalid");
            when(mockOperation.getText()).thenReturn("new");

            assertDoesNotThrow(() -> handler.createCommand(mockOperation));
        }
    }

    private ClassEntity createEntity(String id, String name) {
        ClassEntity entity = new ClassEntity(0, 0, id, name, "CLASS",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        entity.setStereotypeName("");

        return entity;
    }
}