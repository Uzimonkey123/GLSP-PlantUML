package com.diagrams.ClassDiagram.factory;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.ClassLink;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static guru.nidi.graphviz.engine.Engine.DOT;
import static guru.nidi.graphviz.model.Factory.*;
import static guru.nidi.graphviz.attribute.Attributes.attr;

public class ClassLayout {

    public void layoutEntities(List<ClassEntity> entities, List<ClassLink> links, Map<String, Size> dimensions) {
        MutableGraph graph = mutGraph("class_diagram").setDirected(true).graphAttrs().add(
                        attr("rankdir", "BT"),
                        attr("nodesep", 2.0),
                        attr("ranksep", 1.5),
                        attr("splines", "polyline"),
                        attr("pad", "0.5,0.5")
                );

        for (ClassEntity entity : entities) {
            MutableNode node = mutNode(entity.getId());

            Size size = dimensions.get(entity.getId());
            if (size != null) {
                // Transform width and height to graphViz constants
                double widthInches = size.width / 72.0;
                double heightInches = size.height / 72.0;

                node = node.add(
                        attr("width", String.format("%.2f", widthInches)),
                        attr("height", String.format("%.2f", heightInches)),
                        attr("fixedsize", "true"),
                        attr("shape", "box")
                );
            }

            graph.add(node);
        }

        for (ClassLink link : links) {
            if (link.getEntity1() != null && link.getEntity2() != null) {
                int minlen = calculateMinLen(link.getMessage());

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

    private void parseAndApplyPositions(String json, List<ClassEntity> entities,
                                        Map<String, Size> dimensions) {
        JSONObject result = new JSONObject(json);
        JSONArray objects = result.getJSONArray("objects");

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        Map<String, double[]> positions = new HashMap<>();

        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);
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

    private int calculateMinLen(String message) {
        if (message == null || message.isEmpty()) {
            return 2;
        }

        double textWidth = message.length() * 4;

        int minlen = (int) Math.ceil(textWidth / 36.0);
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