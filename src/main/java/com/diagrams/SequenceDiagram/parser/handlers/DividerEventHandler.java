/*
 * File: DividerEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for divider events.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import net.sourceforge.plantuml.sequencediagram.Divider;
import net.sourceforge.plantuml.sequencediagram.Event;

public class DividerEventHandler extends SequenceEventHandler {

    public DividerEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof Divider;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        Divider div = (Divider) event;

        return ctx.getLineFinder().findDividerLine(String.join("<br>", div.getText()), event);
    }

    @Override
    protected void process(Event event, int lineNum) {
        Divider div = (Divider) event;
        String label = String.join("<br>", div.getText());

        String msgId = ctx.nextMessageId();
        SequenceMessage message = new SequenceMessage(msgId, null, null, label, null, "edge:divider");

        ctx.addMapperInfo(message, lineNum);
        ctx.getModel().messages.add(message);
    }
}