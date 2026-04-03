/*
 * File: HSpaceEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for HSpace events
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.HSpace;

public class HSpaceEventHandler extends SequenceEventHandler {

    public HSpaceEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof HSpace;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        return -1;
    }

    @Override
    protected void process(Event event, int lineNum) {
        HSpace hSpace = (HSpace) event;
        ctx.getModel().messageSpaces.put(ctx.getModel().messages.size(), hSpace.getPixel());
    }
}