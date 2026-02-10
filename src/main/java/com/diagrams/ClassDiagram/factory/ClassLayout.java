package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import com.diagrams.ClassDiagram.model.ClassParts.Package;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import static guru.nidi.graphviz.engine.Engine.DOT;
import static guru.nidi.graphviz.model.Factory.*;
import static guru.nidi.graphviz.attribute.Attributes.attr;

public class ClassLayout {

    public void layoutEntities(List<ClassEntity> entities, List<ClassLink> links,
                               Map<String, Size> dimensions, List<Package> packages) {
        MutableGraph graph = mutGraph("class_diagram").setDirected(true).graphAttrs().add(
                        attr("rankdir", "BT"),
                        attr("nodesep", 2.0),
                        attr("ranksep", 1.3),
                        attr("splines", "polyline"),
                        attr("pad", "0.5,0.5")
                );

        // Add packages as clusters if they exist
        if (!packages.isEmpty()) {
            addPackagesToGraph(graph, packages, dimensions);
            addEntitiesWithoutPackages(graph, entities, packages, dimensions);

        } else {
            addEntitiesToGraph(graph, entities, dimensions);
        }

        for (ClassLink link : links) {
            if (link.getEntity1() != null && link.getEntity2() != null) {
                int minlen = calculateMinLen(link);

                graph.add(mutNode(link.getEntity1().getId())
                        .addLink(
                                to(mutNode(link.getEntity2().getId()))
                                        .add(attr("minlen", minlen))
                        ));
            }
        }

        String json = Graphviz.fromGraph(graph)
                .engine(DOT)
                .render(Format.JSON)
                .toString();

        parseAndApplyPositions(json, entities, dimensions);
    }

    private void addPackagesToGraph(MutableGraph graph, List<Package> packages,
                                    Map<String, Size> dimensions) {
        for (Package pkg : packages) {
            if (pkg.isTopLevel()) {
                addPackageCluster(graph, pkg, dimensions);
            }
        }
    }

    private void addEntitiesWithoutPackages(MutableGraph graph, List<ClassEntity> entities,
                                            List<Package> packages, Map<String, Size> dimensions) {
        Set<String> packagedEntityIds = new HashSet<>();
        for (Package pkg : packages) {
            for (ClassEntity entity : pkg.getAllEntities()) {
                packagedEntityIds.add(entity.getId());
            }
        }

        // Add entities not in any package
        for (ClassEntity entity : entities) {
            if (!packagedEntityIds.contains(entity.getId())) {
                MutableNode node = mutNode(entity.getId());
                Size size = dimensions.get(entity.getId());

                if (size != null) {
                    double widthInches = size.width / 72.0;
                    double heightInches = size.height / 72.0;

                    node.add(
                            attr("width", widthInches),
                            attr("height", heightInches),
                            attr("fixedsize", "true"),
                            attr("shape", "box")
                    );
                }

                graph.add(node);
            }
        }
    }

    private void addPackageCluster(MutableGraph parentGraph, Package pkg,
                                   Map<String, Size> dimensions) {
        MutableGraph cluster = mutGraph("cluster_" + pkg.getId()).setCluster(true).graphAttrs().add(
                        attr("label", pkg.getName()),
                        attr("style", "rounded"),
                        attr("bgcolor", pkg.getBackground()),
                        attr("penwidth", "1.5"),
                        attr("margin", "20")
                );

        for (ClassEntity entity : pkg.getEntities()) {
            addNode(dimensions, cluster, entity);
        }

        // Recursively add child packages
        for (Package childPkg : pkg.getChildPackages()) {
            addPackageCluster(cluster, childPkg, dimensions);
        }

        parentGraph.add(cluster);
    }

    private void addNode(Map<String, Size> dimensions, MutableGraph cluster, ClassEntity entity) {
        MutableNode node = mutNode(entity.getId());

        Size size = dimensions.get(entity.getId());
        if (size != null) {
            double widthInches = size.width / 72.0;
            double heightInches = size.height / 72.0;

            node = node.add(
                    attr("width", String.format(Locale.US, "%.2f", widthInches)),
                    attr("height", String.format(Locale.US, "%.2f", heightInches)),
                    attr("fixedsize", "true"),
                    attr("shape", "box")
            );
        }

        cluster.add(node);
    }

    private void addEntitiesToGraph(MutableGraph graph, List<ClassEntity> entities,
                                    Map<String, Size> dimensions) {
        for (ClassEntity entity : entities) {
            addNode(dimensions, graph, entity);
        }
    }

    private void parseAndApplyPositions(String json, List<ClassEntity> entities,
                                        Map<String, Size> dimensions) {
        JSONObject result = new JSONObject(json);
        JSONArray objects = result.getJSONArray("objects");

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        Map<String, double[]> positions = new HashMap<>();

        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);

            if (!obj.has("pos")) {
                continue;
            }

            String id = obj.getString("name");
            String pos = obj.getString("pos");
            String[] coords = pos.split(",");

            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);

            positions.put(id, new double[]{x, y});
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
        }

        for (ClassEntity entity : entities) {
            double[] pos = positions.get(entity.getId());
            Size size = dimensions.get(entity.getId());

            if (pos != null && size != null) {
                double centerX = pos[0] - minX;
                double centerY = pos[1] - minY;

                // Getting middle, since JSON returns top left point of object
                entity.setX(centerX - (size.width / 2) + 40);
                entity.setY(centerY - (size.height / 2) + 40);
            }
        }
    }

    private int calculateMinLen(ClassLink link) {
        int baseLength = link.getLength();

        String message = link.getMessage();
        int messageSpace = 0;

        if (message != null && !message.isEmpty()) {
            double textWidth = message.length() * 4;
            messageSpace = (int) Math.ceil(textWidth / 36.0);
        }

        int minlen = Math.max(baseLength, messageSpace);
        return Math.max(1, Math.min(minlen, 10));
    }

    public static class Size {
        public double width;
        public double height;

        public Size(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }
}