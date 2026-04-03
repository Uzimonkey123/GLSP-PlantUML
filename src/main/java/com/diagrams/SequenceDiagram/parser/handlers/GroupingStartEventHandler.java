/*
 * File: GroupingStartEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for group start events
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceGroup;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.GroupingStart;

public class GroupingStartEventHandler extends SequenceEventHandler {

    public GroupingStartEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof GroupingStart;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        GroupingStart gs = (GroupingStart) event;

        return ctx.getLineFinder().findGroupStartLine(gs.getTitle() != null ? gs.getTitle() : "", event);
    }

    @Override
    protected void process(Event event, int lineNum) {
        GroupingStart gs = (GroupingStart) event;

        SequenceGroup group = new SequenceGroup(
                ctx.getModel().messages.size(),
                gs.getTitle(),
                gs.getComment(),
                gs.getLevel());

        ctx.addMapperInfo(group, lineNum);
        ctx.registerGroup(gs, group);
        ctx.getModel().groups.add(group);

        if (gs.getBackColorGeneral() != null) {
            group.setBackColor(gs.getBackColorGeneral().asString());
        }

        if (gs.getBackColorElement() != null) {
            group.setElementColor(gs.getBackColorElement().asString());
        }
    }
}