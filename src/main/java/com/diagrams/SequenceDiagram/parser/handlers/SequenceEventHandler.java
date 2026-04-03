/*
 * File: SequenceEventHandler.java
 * Author: Norman Babiak
 * Description: Abstract base for sequence event handlers using Template Method pattern.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import net.sourceforge.plantuml.sequencediagram.Event;

public abstract class SequenceEventHandler {
    protected final SequenceEventHandlerContext ctx;

    protected SequenceEventHandler(SequenceEventHandlerContext ctx) {
        this.ctx = ctx;
    }

    public abstract boolean canHandle(Event event);

    /**
     * resolves the source line number, then delegates to event-specific processing.
     */
    public final void handle(Event event) {
        int lineNum = resolveLineNumber(event);
        process(event, lineNum);
    }

    /**
     * Locate where this event lives in the PlantUML source text.
     */
    protected abstract int resolveLineNumber(Event event);

    /**
     * create model elements, update shared state, etc.
     */
    protected abstract void process(Event event, int lineNum);
}
