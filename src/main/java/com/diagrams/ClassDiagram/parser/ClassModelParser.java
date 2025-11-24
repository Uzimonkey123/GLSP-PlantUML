package com.diagrams.ClassDiagram.parser;

import com.GLSPPlantUML.parser.PlantUMLParser;
import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import com.diagrams.ClassDiagram.state.ClassModelState;
import com.google.inject.Inject;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.abel.Link;
import net.sourceforge.plantuml.classdiagram.ClassDiagram;
import net.sourceforge.plantuml.core.Diagram;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ClassModelParser implements PlantUMLParser<ClassModel>  {

    @Inject
    ClassModelState modelState;

    ClassModel model;

    @Override
    public ClassModel parse(File file) throws IOException {
        String originalText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        this.model = new ClassModel();

        // Prepare text for PlantUML
        String text = originalText;
        if (!text.contains("@startuml")) {
            text = "@startuml\n" + originalText + "\n@enduml";
        }

        // Parse with PlantUML
        SourceStringReader reader = new SourceStringReader(text);
        List<BlockUml> blocks = reader.getBlocks();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();

            if (d instanceof ClassDiagram cd) {
                Entity root = cd.getRootGroup();

                for (Entity entity : root.leafs()) {
                    handleEntity(entity);
                }

                List<Link> links = cd.getLinks();

                for (Link link : links) {
                    handleLink(link);
                }
            }
        }

        return model;
    }

    private void handleEntity(Entity entity) {
        String id = "ent-" + model.entities.size();
        String name = String.join("<br>", entity.getDisplay().toString());
        String type = entity.getLeafType().toString();

        List<EntityMethod> methods = new ArrayList<>();
        for (CharSequence method : entity.getBodier().getMethodsToDisplay()) {
            EntityMethod entityMethod = new EntityMethod(method.toString());
            methods.add(entityMethod);
        }

        List<EntityMethod> fields = new ArrayList<>();
        for (CharSequence field : entity.getBodier().getFieldsToDisplay()) {
            EntityMethod entityMethod = new EntityMethod(field.toString());
            fields.add(entityMethod);
        }

        int x = model.entities.size() * 40;
        int y = 0;

        ClassEntity newEntity = new ClassEntity(x, y, id, name, type, methods, fields);
        model.entities.add(newEntity);
        System.err.println(newEntity);
    }

    private void handleLink(Link link) {
        String id = "link-" + model.links.size();
        ClassEntity entity1 = model.getClassEntity(
                                String.join("<br>", link.getEntity1().getDisplay().toString()));
        ClassEntity entity2 = model.getClassEntity(
                                String.join("<br>", link.getEntity2().getDisplay().toString()));
        String type = link.getType().toString();
        String message = String.join("<br>", link.getLabel().toString());

        ClassLink newLink = new ClassLink(id, entity1, entity2, type, message);
        model.links.add(newLink);
        System.err.println(newLink);
    }
}
