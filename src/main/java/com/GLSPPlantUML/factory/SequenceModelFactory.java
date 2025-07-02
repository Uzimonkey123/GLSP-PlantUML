package com.GLSPPlantUML.factory;

import com.GLSPPlantUML.builders.AnchorBuild;
import com.GLSPPlantUML.builders.NodeBuild;
import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.GLSPPlantUML.state.SequenceModelState;
import com.GLSPPlantUML.utils.NodeGap;
import jakarta.inject.Inject;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.builder.impl.*;
import org.eclipse.glsp.server.features.core.model.GModelFactory;

import java.util.*;

import static org.eclipse.glsp.graph.util.GraphUtil.point;

public class SequenceModelFactory implements GModelFactory {
    private Map<String, Double> centre; // Map to store the middle of all nodes for lifeline
    private Map<String, Double> halfWidth; // Half size of nodes for created node arrows
    private List<GModelElement> elements = new ArrayList<>();
    private NodeGap gapCalculator;
    private NodeBuild nodeBuild;

    private Stack<SequenceAnchor> anchors = new Stack<>(); // Stack for anchors in the diagram
    private Map<String, SequenceAnchor> anchorMap = new HashMap<>(); // Map to store anchors with their ID for easier search

    private final List<Double> lifeEventYPos = new ArrayList<>();
    private final List<Double> messagesYPos = new ArrayList<>();

    private double cursor;

    @Inject
    protected SequenceModelState modelState;

    @Override
    public void createGModel() {
        SequenceModel model = modelState.getModel();

        double nodeY = 30;
        double nodeHeight = 30;
        double firstMsgY = nodeY + nodeHeight + 10;

        calculateYPositions(model, firstMsgY); // Get all Y coordinate information for messages/life events/ ver. spaces
        double totalHeight = messagesYPos.stream().mapToDouble(Double::doubleValue).max().orElse(firstMsgY);

        this.gapCalculator = new NodeGap(model.messages);
        this.nodeBuild = new NodeBuild();

        // Build all the nodes and create list to get their centers
        SequenceNodeFactory nodeFactory = new SequenceNodeFactory(model, nodeBuild, totalHeight,
                                                                    messagesYPos, gapCalculator);
        nodeFactory.createNodes();

        this.elements = nodeFactory.getElements();
        this.centre = nodeFactory.getCentre();
        this.halfWidth = nodeFactory.getHalfWidth();
        this.cursor = nodeFactory.getCursor();

        // Build the necessary life events and destroy icons
        SequenceLifeEventFactory leFactory = new SequenceLifeEventFactory(model, lifeEventYPos, centre,
                                                                            elements, messagesYPos);
        leFactory.createSequenceLifeEvents();

        // Populate anchor map with all anchors and match them with their ID
        for (SequenceAnchor anchor : model.anchors) {
            anchorMap.put(anchor.getAnchorId(), anchor);
        }

        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);

            addEdges(msg, model, i);

            // Additionally to adding edges, check for anchors and create them if needed
            setupAnchors(msg, i);
        }

        // Build the graph
        GGraph newGModel = new GGraphBuilder()
                .id("sequence-diagram")
                .addAll(elements)
                .build();

        // Update model state
        modelState.updateRoot(newGModel);
        modelState.getRoot().setRevision(-1);
    }

    private void calculateYPositions(SequenceModel model, double firstMsgY) {
        double hspace = 0;
        double msgGap = 35;

        for (int i = 0; i < model.messages.size(); i++) {
            SequenceMessage msg = model.messages.get(i);
            int lines = msg.getMessage().split("<br>").length;
            int extra = Math.max(0, (lines - 1) * 14);

            if (model.messageSpaces.containsKey(i)) {
                hspace += model.messageSpaces.get(i);
            } else {
                model.messageSpaces.put(i, extra);
            }

            hspace += extra;

            double y = firstMsgY + i * msgGap + hspace;
            messagesYPos.add(y);

            // If message is self call activation, the start of life event is lower
            // for deactivation or destroy of self message it does not set offset
            int finalI = i;
            boolean isStartOfLifeEvent = msg.isSelf() &&
                    model.participants.stream()
                    .anyMatch(node -> node.getLifeEvents().stream()
                            .anyMatch(event -> event.getStartMessage() == finalI));

            lifeEventYPos.add(isStartOfLifeEvent ? y + 15 : y);
        }

        int trailing = model.messageSpaces.getOrDefault(model.messages.size(), 0);
        if (trailing > 0) {
            // In case HSpace is added after the last message
            double lastY = messagesYPos.getLast() + msgGap + trailing;
            messagesYPos.add(lastY);
            lifeEventYPos.add(lastY);
        }
    }

    private void setupAnchors(SequenceMessage msg, int msgIndex) {
        double y = messagesYPos.get(msgIndex);
        // If message is an anchor start, get the top Y coordinate of it
        // and save it inside the anchors stack
        if (msg.isAnchorStart()) {
            SequenceAnchor concreteAnchor = anchorMap.get(msg.getAnchorId());
            concreteAnchor.setTopY(y);
            anchors.push(concreteAnchor);
        }

        if (msg.isAnchorEnd()) {
            SequenceAnchor concreteAnchor = anchors.pop();
            double gap = gapCalculator.getGaps(concreteAnchor.getParticipant1(), concreteAnchor.getParticipant2());

            AnchorBuild anchor = new AnchorBuild(concreteAnchor, y, gap);
            double xCoord = anchor.getXCoord(centre.get(concreteAnchor.getParticipant1()),
                                            centre.get(concreteAnchor.getParticipant2()));

            // Create anchor points in the middle of the messages
            generateAnchorPoints(concreteAnchor, concreteAnchor.getParticipant1(), xCoord, y);

            elements.add(anchor.build());

        }
    }

    private void addEdges(SequenceMessage msg, SequenceModel model, int msgIndex) {
        String sourceId, targetId;
        double x1, x2;
        double y = messagesYPos.get(msgIndex);

        String routingOne = msg.getFrom();
        String routingTwo = msg.getTo();
        if (msg.getType().equals("edge:delay") || msg.getType().equals("edge:divider")) {
            routingOne = model.participants.getFirst().getName();
            routingTwo = model.participants.getLast().getName();
        }

        if (msg.getType().equals("edge:ref")) { // If it is reference, just handle that and exit
            addReference(msg, model, msgIndex);
            return;
        }

        boolean incoming = msg.decideWay().equals("incoming");
        boolean outgoing = msg.decideWay().equals("outgoing");

        sourceId = routingOne;
        targetId = routingTwo;
        x1 = setX(routingOne, msgIndex);
        x2 = setX(routingTwo, msgIndex);

        if (incoming) {
            sourceId = "[";
            x1 = msg.getFrom().equals("[") ? 0 : cursor + halfWidth.get(model.participants.getLast().getName());

        } else if (outgoing) {
            targetId = "]";
            x2 = msg.getTo().equals("]") ? cursor + halfWidth.get(model.participants.getLast().getName()) : 0;

        }

        GEdgeBuilder eb;
        eb = new GEdgeBuilder(msg.getType())
                .id(msg.getMsgId())
                .sourceId(sourceId)
                .targetId(targetId)
                .addRoutingPoint(point(x1, y))
                .addRoutingPoint(point(x2, y));

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

        elements.add(eb.build());

        // Add labels to messages
        addLabels(msg, model, msgIndex, x1, x2, routingOne);
    }

    private void addLabels(SequenceMessage msg, SequenceModel model, int msgIndex, double x1, double x2,
                           String routingOne) {
        double labelShift;
        double y = messagesYPos.get(msgIndex);

        if (msg.isSelf()) {
            labelShift = (centre.get(routingOne) + centre.get(model.getNextParticipant(routingOne))) / 2;
        } else {
            labelShift = (x1 + x2) / 2;
        }

        int lineCount = msg.getMessage().split("<br>").length;
        double labelYOffset = lineCount > 1 ? lineCount * 14 : 6;

        elements.add(new GLabelBuilder("label:html")
                .id("label-"+msgIndex)
                .text(msg.getMessage())
                .addArgument("numbering", msg.getNumbering())
                .position(labelShift, y - labelYOffset)
                .build());
    }

    private void addReference(SequenceMessage msg, SequenceModel model, int msgIndex) {
        String from = msg.getFrom();
        String to = msg.getTo();

        double x1 = centre.get(from) - 25;
        double x2 = centre.get(to) + 25;

        String[] lines = msg.getMessage().split("<br>");
        int maxLineLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        int labelWidth = maxLineLength * 8 + 5;

        int lineCount = lines.length;
        int labelHeight = lineCount * 14;

        // Get y1 according to the current index - amount of lines and some padding
        double y1 = messagesYPos.get(msgIndex) - (labelHeight + 10);
        double y2 = messagesYPos.get(msgIndex);

        double baseWidth = x2 - x1;
        if (labelWidth > baseWidth) {
            x2 = x1 + labelWidth; // Move the ref just towards the right, no further extension to the left
        }

        elements.add(new GEdgeBuilder("edge:ref")
                .id(msg.getMsgId())
                .sourceId(from)
                .targetId(to)
                .addArgument("x1", x1)
                .addArgument("x2", x2)
                .addArgument("y1", y1)
                .addArgument("y2", y2)
                .build());

        addLabels(msg, model, msgIndex, x1, x2, from);
    }

    private void generateAnchorPoints(SequenceAnchor anchor, String from, double xCoord, double y) {
        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-top")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), anchor.getTopY())
                .size(0, 0)
                .build());

        elements.add(new GNodeBuilder()
                .id(anchor.getAnchorId() + "-bottom")
                .layout("vbox")
                .position(xCoord - halfWidth.get(from), y)
                .size(0, 0)
                .build());
    }

    private double setX(String participant, int messageIndex) {
        if (participant.equals("[") || participant.equals("]")) {
            return centre.getOrDefault(participant, 0.0);
        }

        SequenceNode node = modelState.getModel().getNode(participant);
        Optional<SequenceLifeEvent> lifeEventPos = node.getLifeEventAt(messageIndex);

        return lifeEventPos
                // Set the arrow more to the right if there is life event/nesting
                // or just return normal participant middle X
                .map(lifeEvent -> centre.get(participant) + (lifeEvent.getLevel() + 1) * 3)
                .orElse(centre.get(participant));
    }
}
