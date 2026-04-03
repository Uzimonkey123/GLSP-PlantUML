/*
 * File: ReferenceEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for reference events.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.Reference;

public class ReferenceEventHandler extends SequenceEventHandler {

    public ReferenceEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof Reference;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        Reference ref = (Reference) event;
        SequenceNode first = ctx.getSequenceNode(ref.getParticipant().getFirst());

        return ctx.getLineFinder().findReferenceLine("ref over " + first.getName(), ref);
    }

    @Override
    protected void process(Event event, int lineNum) {
        Reference ref = (Reference) event;
        SequenceNode first = ctx.getSequenceNode(ref.getParticipant().getFirst());
        SequenceNode last = ctx.getSequenceNode(ref.getParticipant().getLast());

        String label = String.join("<br>", ref.getStrings());
        int endLine = label.contains("<br>")
                ? ctx.getLineFinder().findEndReferenceLine("end ref", ref)
                : lineNum;

        String msgId = ctx.nextMessageId();
        SequenceMessage message = new SequenceMessage(msgId, first, last, label, null, "edge:ref");

        ctx.addMapperInfo(message, lineNum, endLine);
        ctx.getModel().messages.add(message);
    }
}