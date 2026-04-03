/*
 * File: MessageExoEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for external messages.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.MessageExo;
import net.sourceforge.plantuml.skin.ArrowConfiguration;

public class MessageExoEventHandler extends SequenceEventHandler {

    public MessageExoEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof MessageExo;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        MessageExo msg = (MessageExo) event;
        String label = String.join("\\n", msg.getLabel());
        int line = ctx.getLineFinder().findMessageLine(label, event);

        return line >= 0 ? line : ctx.getLineFinder().findReturnLine(label, event);
    }

    @Override
    protected void process(Event event, int lineNum) {
        MessageExo msg = (MessageExo) event;
        SequenceNode participant = ctx.getSequenceNode(msg.getParticipant());

        SequenceNode from;
        SequenceNode to;
        boolean incoming;
        boolean outgoing;

        switch (msg.getType()) {
            case TO_LEFT, TO_RIGHT -> {
                from = participant;
                to = null;
                incoming = false;
                outgoing = true;
            }
            default -> {
                from = null;
                to = participant;
                incoming = true;
                outgoing = false;
            }
        }

        boolean isShort = msg.isShortArrow();
        String num = msg.getMessageNumber() != null ? msg.getMessageNumber() : "";
        String label = String.join(" ", msg.getLabel());
        ArrowConfiguration arrowConfig = msg.getArrowConfiguration();

        String msgId = ctx.nextMessageId();

        SequenceMessage message = new SequenceMessage(
                msgId, from, to, label, arrowConfig, "edge", num,
                isShort, incoming, outgoing);

        ctx.addMapperInfo(message, lineNum);
        ctx.getModel().messages.add(message);

        if (msg.isParallel()) {
            message.setParallel(true);
        }

        ctx.handleMessageNotes(msg, message);
        ctx.getModel().invisibleNodes = true;
    }
}