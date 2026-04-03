/*
 * File: MessageEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for regular sequence diagram messages between participants.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.Message;
import net.sourceforge.plantuml.skin.ArrowConfiguration;

public class MessageEventHandler extends SequenceEventHandler {

    public MessageEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof Message;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        Message msg = (Message) event;
        String label = String.join("\\n", msg.getLabel());
        int line = ctx.getLineFinder().findMessageLine(label, event);

        return line >= 0 ? line : ctx.getLineFinder().findReturnLine(label, event);
    }

    @Override
    protected void process(Event event, int lineNum) {
        Message msg = (Message) event;
        SequenceNode from = ctx.getSequenceNode(msg.getParticipant1());
        SequenceNode to = ctx.getSequenceNode(msg.getParticipant2());

        String num = msg.getMessageNumber() != null ? msg.getMessageNumber() : "";
        String label = String.join("<br>", msg.getLabel());
        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();
        boolean isSelf = msg.isSelfMessage();

        String msgId = ctx.nextMessageId();

        SequenceMessage message = new SequenceMessage(msgId, msg.isCreate(), from, to, label, arrowConfig,
                "edge", num, false, isSelf);

        ctx.addMapperInfo(message, lineNum);
        ctx.getModel().messages.add(message);

        if (msg.isParallel()) {
            message.setParallel(true);
        }

        ctx.handleMessageNotes(msg, message);

        if (msg.getAnchor() != null) {
            ctx.setupAnchor(msg, from, to);
        }
    }
}