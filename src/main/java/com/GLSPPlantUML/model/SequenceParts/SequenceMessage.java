package com.GLSPPlantUML.model.SequenceParts;

import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.skin.ArrowConfiguration;
import net.sourceforge.plantuml.skin.ArrowDecoration;
import net.sourceforge.plantuml.skin.ArrowHead;
import net.sourceforge.plantuml.skin.ArrowPart;

import java.util.ArrayList;
import java.util.List;

public class SequenceMessage {
    private final String msgId;
    private final String from;
    private final String to;
    private String message = "";
    private ArrowConfiguration arrowConfiguration = null;
    private String messageType = "";
    private String numbering = "";
    private boolean isShort = false;
    private boolean isSelf = false;
    private boolean incoming = false;
    private boolean outgoing = false;
    private boolean creating = false;
    private boolean anchorStart = false;
    private boolean anchorEnd = false;
    private String anchorId = "";
    private List<SequenceNote> notes;

    public SequenceMessage(String msgId, boolean creating, String from, String to, String message, ArrowConfiguration arrowConfiguration,
                           String messageType, String numbering, boolean isShort, boolean isSelf) {
        this.msgId = msgId;
        this.from = from;
        this.to = to;
        this.message = message;
        this.arrowConfiguration = arrowConfiguration;
        this.messageType = messageType;
        this.isSelf = isSelf;
        this.creating = creating;
        this.numbering = numbering;
        this.isShort = isShort;

        this.notes = new ArrayList<>();
    }

    public SequenceMessage(String msgId, String from, String to, String message, ArrowConfiguration arrowConfiguration,
                           String messageType) {
        this.msgId = msgId;
        this.from = from;
        this.to = to;
        this.message = message;
        this.arrowConfiguration = arrowConfiguration;
        this.messageType = messageType;
    }

    public SequenceMessage(String msgId, String from, String to, String message, ArrowConfiguration arrowConfiguration,
                           String messageType, String numbering, boolean isShort, boolean incoming, boolean outgoing) {
        this.msgId = msgId;
        this.from = from;
        this.to = to;
        this.message = message;
        this.arrowConfiguration = arrowConfiguration;
        this.messageType = messageType;
        this.numbering = numbering;
        this.isShort = isShort;
        this.incoming = incoming;
        this.outgoing = outgoing;

        this.notes = new ArrayList<>();
    }

    public SequenceMessage(String msgId, String from, String to, String messageType) {
        this.msgId = msgId;
        this.from = from;
        this.to = to;
        this.messageType = messageType;

        this.notes = new ArrayList<>();
    }

    public String getMsgId() {
        return msgId;
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

    public void setMessage(String message) {
        this.message = message;
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
        return this.isSelf;
    }

    public String getColor() {
        HColor color = arrowConfiguration.getColor();
        if (color == null) {
            return "black";
        }

        return color.asString();
    }

    public String decideWay() {
        if (this.incoming) return "incoming";
        if (this.outgoing) return "outgoing";

        return "normal";
    }

    public boolean isShort() {
        return this.isShort;
    }

    public boolean isCreating() {
        return this.creating;
    }

    public void setAnchorStart(boolean anchorStart) {
        this.anchorStart = anchorStart;
    }

    public void setAnchorEnd(boolean anchorEnd) {
        this.anchorEnd = anchorEnd;
    }

    public boolean isAnchorStart() {
        return this.anchorStart;
    }

    public boolean isAnchorEnd() {
        return this.anchorEnd;
    }

    public String getAnchorId() {
        return this.anchorId;
    }

    public void setAnchorId(String anchorId) {
        this.anchorId = anchorId;
    }

    public List<SequenceNote> getNotes() {
        return this.notes;
    }

    public void addNotes(SequenceNote note) {
        this.notes.add(note);
    }
}
