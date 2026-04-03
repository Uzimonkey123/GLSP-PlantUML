/*
 * File: NoteEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for standalone Note and parallel Notes events.
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.Note;
import net.sourceforge.plantuml.sequencediagram.Notes;

import java.util.List;

public class NoteEventHandler extends SequenceEventHandler {

    public NoteEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof Note || event instanceof Notes;
    }

    /**
     * Line resolution is handled inside processSeparateNote, since notes require multi-line detection
     */
    @Override
    protected int resolveLineNumber(Event event) {
        return -1;
    }

    @Override
    protected void process(Event event, int lineNum) {
        if (event instanceof Notes notes) {
            List<Note> noteList = notes.asList();
            for (int i = 0; i < noteList.size(); i++) {
                ctx.processSeparateNote(noteList.get(i), i > 0);
            }

        } else {
            ctx.processSeparateNote((Note) event, false);
        }
    }
}