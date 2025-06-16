package com.GLSPPlantUML.model;

import net.sourceforge.plantuml.klimt.color.ColorMapper;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.skin.ArrowConfiguration;
import net.sourceforge.plantuml.skin.ArrowDecoration;
import net.sourceforge.plantuml.skin.ArrowHead;
import net.sourceforge.plantuml.skin.ArrowPart;

import java.util.ArrayList;
import java.util.List;

public class SequenceModel {
    public static class SequenceMessage {
        private final String from;
        private final String to;
        private final String message;
        private final ArrowConfiguration arrowConfiguration;
        private final String messageType;
        private final String numbering;

        public SequenceMessage(String from, String to, String message, ArrowConfiguration arrowConfiguration, String messageType, String numbering) {
            this.from = from;
            this.to = to;
            this.message = message;
            this.arrowConfiguration = arrowConfiguration;
            this.messageType = messageType;
            this.numbering = numbering;
        }

        public String getNumbering() {
            return numbering;
        }

        public String getType() {
            return messageType;
        }

        private String getHead(ArrowHead arrowHead) {
            return switch (arrowHead) {
                case NORMAL -> "block";
                case ASYNC -> "open";
                case CROSSX -> "cross";
                default -> "none";
            };
        }

        private String getPart(ArrowPart arrowPart) {
            return switch (arrowPart) {
                case BOTTOM_PART -> "bottom";
                case TOP_PART -> "top";
                default -> "full";
            };
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getMessage() {
            return message;
        }

        public boolean isDotted() {
            return arrowConfiguration.isDotted();
        }

        public String getStartHead() {
            return this.getHead(arrowConfiguration.getDressing1().getHead());
        }

        public String getEndHead() {
            return this.getHead(arrowConfiguration.getDressing2().getHead());
        }

        public String getStartPart() {
            return this.getPart(arrowConfiguration.getDressing1().getPart());
        }

        public String getEndPart() {
            return this.getPart(arrowConfiguration.getDressing2().getPart());
        }

        public String getStartDecor() {
            return arrowConfiguration.getDecoration1() == ArrowDecoration.CIRCLE ? "circle" : "none";
        }

        public String getEndDecor() {
            return arrowConfiguration.getDecoration2() == ArrowDecoration.CIRCLE ? "circle" : "none";
        }

        public boolean isSelf() {
            return this.from.equals(this.to);
        }

        public String getColor() {
            HColor color = arrowConfiguration.getColor();
            if (color == null) {
                return "black";
            }

            return color.asString();
        }
    }

    public static class SequenceNode {
        private final String name;
        private final String type;
        private final int order;
        private final HColor background;

        public SequenceNode(String name, String type, int order, HColor background) {
            this.name = name;
            this.type = type;
            this.order = order;
            this.background = background;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public int getOrder() {
            return order;
        }

        public String getBackground() {
            return this.background != null ? this.background.asString() : "#5d4949";
        }
    }

    public List<SequenceNode> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();

    public SequenceModel() {}
}
