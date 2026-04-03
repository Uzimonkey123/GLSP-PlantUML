/*
 * File: SequenceEventHandlerRegistry.java
 * Author: Norman Babiak
 * Description: Registry that dispatches sequence events to the correct handler.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import net.sourceforge.plantuml.sequencediagram.Event;

import java.util.List;

public class SequenceEventHandlerRegistry {
    private final List<SequenceEventHandler> handlers;

    public SequenceEventHandlerRegistry(SequenceEventHandlerContext ctx) {
        this.handlers = List.of(
                new GroupingStartEventHandler(ctx),
                new GroupingLeafEventHandler(ctx),
                new MessageExoEventHandler(ctx),
                new MessageEventHandler(ctx),
                new DelayEventHandler(ctx),
                new DividerEventHandler(ctx),
                new SequenceLifeEventHandler(ctx),
                new HSpaceEventHandler(ctx),
                new ReferenceEventHandler(ctx),
                new NoteEventHandler(ctx)
        );
    }

    public void handle(Event event) {
        for (SequenceEventHandler handler : handlers) {
            if (handler.canHandle(event)) {
                handler.handle(event);

                return;
            }
        }
    }
}