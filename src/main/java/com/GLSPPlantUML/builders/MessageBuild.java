package com.GLSPPlantUML.builders;

import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.GEdgeBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;

import java.util.Map;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class MessageBuild {
    private final Map<String, Double> halfWidth;

    public MessageBuild(Map<String, Double> halfWidth) {
        this.halfWidth = halfWidth;
    }

    public GModelElement buildEdge(SequenceMessage msg, String sourceId, String targetId, double x1, double x2,
                                   double y, boolean incoming, boolean outgoing) {
        GEdgeBuilder eb = new GEdgeBuilder(msg.getType())
                .id(msg.getMsgId())
                .sourceId(sourceId)
                .targetId(targetId)
                .addRoutingPoint(point(x1, y))
                .addRoutingPoint(point(x2, y));

        addArguments(msg, eb, incoming, outgoing, targetId, x1, x2);

        return eb.build();
    }

    private void addArguments(SequenceMessage msg, GEdgeBuilder eb, boolean incoming, boolean outgoing, String targetId,
                              double x1, double x2) {
        if (msg.getType().equals("edge:divider")) {
            eb.addArgument("labelWidth", msg.getMessage().length());
        }

        // Additional arguments to get every side and aspect of the arrow
        if (msg.getType().equals("edge")) {
            eb.addArgument("headStart", msg.getStartHead());
            eb.addArgument("headEnd", msg.getEndHead());
            eb.addArgument("partStart", msg.getStartPart());
            eb.addArgument("partEnd", msg.getEndPart());
            eb.addArgument("circleStart", msg.getStartDecor());
            eb.addArgument("circleEnd", msg.getEndDecor());
            eb.addArgument("style", msg.isDotted() ? "dotted" : "solid");
            eb.addArgument("self", msg.isSelf());
            eb.addArgument("arrColor", msg.getColor());

            eb.addArgument("creating", msg.isCreating());
            eb.addArgument("toWidth", halfWidth.get(targetId));
        }

        if (incoming || outgoing) {
            eb.addArgument("incoming", incoming);
            eb.addArgument("outgoing", outgoing);
            eb.addArgument("isShort", msg.isShort());

            // For short arrows
            eb.addArgument("fromX", x1);
            eb.addArgument("toX", x2);
        }
    }

    public GModelElement buildMsgLabel(SequenceMessage msg, int msgIndex, double y, double shift, double yOffset) {
        return new GLabelBuilder("label:html")
                .id("label-"+ msgIndex)
                .text(msg.getMessage())
                .size(10, 10)
                .addArgument("numbering", msg.getNumbering())
                .position(shift, y - yOffset)
                .addArgument("selectable", true)
                .build();
    }

    public GModelElement buildReference(SequenceMessage msg, String from, String to, double x1, double x2,
                                        double y1, double y2) {
        return new GEdgeBuilder("edge:ref")
                .id(msg.getMsgId())
                .sourceId(from)
                .targetId(to)
                .addArgument("x1", x1)
                .addArgument("x2", x2)
                .addArgument("y1", y1)
                .addArgument("y2", y2)
                .build();
    }
}
