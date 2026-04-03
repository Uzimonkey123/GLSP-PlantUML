/*
 * File: GroupingLeafEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for group leaf events
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceGroup;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.GroupingLeaf;
import net.sourceforge.plantuml.sequencediagram.GroupingStart;
import net.sourceforge.plantuml.sequencediagram.GroupingType;

public class GroupingLeafEventHandler extends SequenceEventHandler {

    public GroupingLeafEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof GroupingLeaf;
    }

    @Override
    protected int resolveLineNumber(Event event) {
        GroupingLeaf leaf = (GroupingLeaf) event;

        if (leaf.getType() == GroupingType.ELSE) {
            return ctx.getLineFinder().findGroupElseLine(leaf.getComment() != null ? leaf.getComment() : "", event);
        }

        if (leaf.getType() == GroupingType.END) {
            return ctx.getLineFinder().findGroupEndLine(event);
        }

        return -1;
    }

    @Override
    protected void process(Event event, int lineNum) {
        GroupingLeaf leaf = (GroupingLeaf) event;
        GroupingStart parent = leaf.getGroupingStart();
        if (parent == null) return;

        SequenceGroup seqGroup = ctx.getGroup(parent);
        if (seqGroup == null) return;

        if (leaf.getType() == GroupingType.END) {
            seqGroup.setEndIndex(ctx.getModel().messages.size());

            if (lineNum >= 0 && seqGroup.hasLine()) {
                seqGroup.setSourceLines(seqGroup.getSourceLineStart(), lineNum);
            }
        }

        if (leaf.getType() == GroupingType.ELSE) {
            seqGroup.addSeparator(ctx.getModel().messages.size());

            if (lineNum >= 0) {
                seqGroup.addSeparatorLineNumber(lineNum);
            }

            seqGroup.addSeparatorLabel(leaf.getComment() != null ? leaf.getComment() : "");
        }
    }
}