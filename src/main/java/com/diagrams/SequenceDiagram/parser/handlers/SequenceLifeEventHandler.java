/*
 * File: SequenceLifeEventHandler.java
 * Author: Norman Babiak
 * Description: Handler for life events
 * Date: 2.4.2026
 */

package com.diagrams.SequenceDiagram.parser.handlers;

import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceLifeEvent;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceNode;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.sequencediagram.Event;
import net.sourceforge.plantuml.sequencediagram.LifeEvent;

import java.util.Stack;

public class SequenceLifeEventHandler extends SequenceEventHandler {

    public SequenceLifeEventHandler(SequenceEventHandlerContext ctx) {
        super(ctx);
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof LifeEvent;
    }

    /**
     * Life events use a separate search pass
     */
    @Override
    protected int resolveLineNumber(Event event) {
        LifeEvent le = (LifeEvent) event;
        String participantName = String.join("<br>", le.getParticipant().getDisplay(false));

        int savedPosition = ctx.getLineFinder().getCurrentPosition();
        ctx.getLineFinder().resetSearch();

        int lineNum = switch (le.getType()) {
            case ACTIVATE -> ctx.getLineFinder().findActivateLine(participantName, event);
            case DEACTIVATE -> ctx.getLineFinder().findDeactivateLine(participantName, event);
            case DESTROY -> ctx.getLineFinder().findDestroyLine(participantName, event);
            default -> -1;
        };

        ctx.getLineFinder().setPosition(savedPosition);
        return lineNum;
    }

    @Override
    protected void process(Event event, int lineNum) {
        LifeEvent le = (LifeEvent) event;
        String participant = String.join("<br>", le.getParticipant().getDisplay(false));
        HColor background = le.getSpecificColors().getBackColor();

        SequenceNode currentNode = ctx.getModel().participants.stream()
                .filter(n -> n.getName().equals(participant))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Error setting node for given life event."));

        ctx.initActivationStacks(participant);

        Stack<Integer> messageStack = ctx.getActivationStack(participant);
        Stack<HColor> colorStack = ctx.getActivationColorStack(participant);
        Stack<Integer> lineStack = ctx.getActivationLineStack(participant);

        int index = ctx.getModel().messages.size() - 1;

        switch (le.getType()) {
            case ACTIVATE -> {
                messageStack.push(index);
                colorStack.push(background);
                lineStack.push(lineNum);
            }

            case DEACTIVATE -> {
                if (messageStack.isEmpty()) return;

                int startIndex = messageStack.pop();
                HColor color = colorStack.pop();
                int activateLine = lineStack.pop();

                SequenceLifeEvent lifeEvent = new SequenceLifeEvent(startIndex, index, color);
                lifeEvent.setLevel(messageStack.size());

                ctx.addMapperInfo(lifeEvent, activateLine, lineNum);
                currentNode.addLifeEvent(lifeEvent);
            }

            case DESTROY -> {
                if (!messageStack.isEmpty()) {
                    int startIndex = messageStack.pop();
                    HColor color = colorStack.pop();

                    SequenceLifeEvent lifeEvent = new SequenceLifeEvent(startIndex, index, color);
                    lifeEvent.setLevel(messageStack.size());

                    ctx.addMapperInfo(lifeEvent, lineNum);
                    currentNode.addLifeEvent(lifeEvent);
                }

                currentNode.setDestroyIndex(index);
            }

            case CREATE -> {
                currentNode.setCreatedNode(true);
                currentNode.setCreatedIndex(ctx.getModel().messages.size());
            }
        }
    }
}