/*
 * File: DelayEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for delay events.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import net.sourceforge.plantuml.sequencediagram.Delay;
import net.sourceforge.plantuml.sequencediagram.Event;

public class DelayEventHandler extends SequenceEventHandler {

    public DelayEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof Delay;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        Delay delay = (Delay) event;

        return ctx.getLineFinder().findDelayLine(String.join("<br>", delay.getText()), event);
    }

    @Override
    protected void process(Event event, int lineNum) {
        Delay delay = (Delay) event;

        String label = "";
        if (delay.getText() != null && delay.getText().size() > 0) {
            label = String.join("<br>", delay.getText());
        }

        String msgId = ctx.nextMessageId();
        SequenceMessage message = new SequenceMessage(msgId, null, null, label, null, "edge:delay");

        ctx.addMapperInfo(message, lineNum);
        ctx.getModel().messages.add(message);
    }
}