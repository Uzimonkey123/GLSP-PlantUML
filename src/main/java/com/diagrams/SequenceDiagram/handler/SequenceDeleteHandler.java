/*
 * File: SequenceDeleteHandler.java
 * Author: Norman Babiak
 * Description: Handles deletion of sequence diagram elements
 * Date: 4.4.2026
 */

// TODO: Life event bug when multiple deletions of a nested life event + inline removal bugs

package com.diagrams.SequenceDiagram.handler;

import com.google.inject.Inject;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.SourceElement;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import com.diagrams.SequenceDiagram.utils.GroupAdjuster;
import org.eclipse.emf.common.command.AbstractCommand;
import org.eclipse.emf.common.command.Command;
import org.eclipse.glsp.server.actions.ActionDispatcher;
import org.eclipse.glsp.server.features.core.model.UpdateModelAction;
import org.eclipse.glsp.server.operations.DeleteOperation;
import org.eclipse.glsp.server.operations.OperationHandler;
import org.eclipse.glsp.graph.GModelElement;
import org.eclipse.glsp.graph.GModelRoot;

import java.util.*;

public class SequenceDeleteHandler implements OperationHandler<DeleteOperation> {

    @Inject
    protected SequenceModelState modelState;

    @Inject
    protected ActionDispatcher actionDispatcher;

    @Override
    public Optional<Command> createCommand(DeleteOperation operation) {
        return Optional.of(new DeleteCommand(operation, modelState, actionDispatcher));
    }

    @Override
    public Class<DeleteOperation> getHandledOperationType() {
        return DeleteOperation.class;
    }

    private static class DeleteCommand extends AbstractCommand {
        private final DeleteOperation operation;
        private final SequenceModelState modelState;
        private final ActionDispatcher actionDispatcher;

        private GModelRoot root;
        private SequenceModel model;
        private final List<String> removals = new ArrayList<>();
        private GroupAdjuster groupAdjuster;

        public DeleteCommand(DeleteOperation operation, SequenceModelState modelState,
                             ActionDispatcher actionDispatcher) {
            this.operation = operation;
            this.modelState = modelState;
            this.actionDispatcher = actionDispatcher;
        }

        @Override
        public boolean canExecute() {
            return operation != null && modelState != null
                    && operation.getElementIds() != null
                    && !operation.getElementIds().isEmpty();
        }

        @Override
        public void execute() {
            this.root = modelState.getRoot();
            this.model = modelState.getModel();
            this.groupAdjuster = new GroupAdjuster(model, removals);

            for (String id : operation.getElementIds()) {
                resolveAndDelete(id);
            }

            cleanupInvalidDestroys();
            flushRemovals();

            actionDispatcher.dispatch(new UpdateModelAction());
        }

        private void resolveAndDelete(String id) {
            SequenceNode participant = findParticipant(id);
            if (participant != null) {
                groupAdjuster.collectAllGroupIds();
                removeParticipant(participant);
                return;
            }

            SequenceMessage message = findMessage(id);
            if (message != null) {
                removeMessage(message);
                scheduleRemoval(id);
                return;
            }

            SequenceGroup group = findGroup(id);
            if (group != null) {
                removeGroup(group);
                scheduleRemoval(id);
                return;
            }

            SequenceNote note = findNote(id);
            if (note != null) {
                removeNote(id, note);
                return;
            }

            SequenceEnglober englober = findEnglober(id);
            if (englober != null) {
                removeEnglober(englober);
                scheduleRemoval(id);
                return;
            }

            SequenceAnchor anchor = findAnchor(id);
            if (anchor != null) {
                removeAnchor(anchor);
                scheduleRemoval(id);
                return;
            }

            SequenceLifeEvent lifeEvent = findLifeEvent(id);
            if (lifeEvent != null) {
                removeLifeEvent(lifeEvent);
                scheduleRemoval(id);
                return;
            }

            SequenceNode destroyOwner = findDestroyOwner(id);
            if (destroyOwner != null) {
                removeDestroyCross(destroyOwner);
                scheduleRemoval(id);
                return;
            }

            scheduleRemoval(id);
        }

        private void removeParticipant(SequenceNode participant) {
            markLines(participant);

            for (SequenceLifeEvent le : participant.getLifeEvents()) {
                markLifeEventLines(le, Collections.emptySet());
                scheduleRemoval("act-" + participant.getId() + "-" + le.getStartMessage());
            }

            if (participant.getDestroyIndex() != -1) {
                scheduleRemoval("dest-" + participant.getId() + "-" + participant.getDestroyIndex());
            }

            Set<Integer> removedIndices = removeConnectedMessages(participant);

            groupAdjuster.adjustGroups(removedIndices);
            groupAdjuster.removeEmptyGroups();
            removeOrphanedLifeEvents(removedIndices, participant);
            removeConnectedAnchors(participant);

            scheduleRemoval(participant.getId());
            model.participants.remove(participant);
        }

        /**
         * Removes all messages connected to a participant, returning their original indices.
         */
        private Set<Integer> removeConnectedMessages(SequenceNode participant) {
            Set<Integer> removedIndices = new HashSet<>();
            Iterator<SequenceMessage> it = model.messages.iterator();
            int index = 0;

            while (it.hasNext()) {
                SequenceMessage msg = it.next();

                if (participant.equals(msg.getFrom()) || participant.equals(msg.getTo())) {
                    markLines(msg);
                    scheduleRemoval(msg.getMsgId());
                    scheduleRemoval("label-" + msg.getMsgId().substring(4));

                    for (SequenceNote note : msg.getNotes()) {
                        markLines(note);
                        scheduleRemoval(note.getId());
                        model.notes.remove(note);
                    }

                    removedIndices.add(index);
                    it.remove();
                }

                index++;
            }

            return removedIndices;
        }

        private void removeConnectedAnchors(SequenceNode participant) {
            Iterator<SequenceAnchor> it = model.anchors.iterator();

            while (it.hasNext()) {
                SequenceAnchor anchor = it.next();

                if (anchor.getParticipant1Id().equals(participant.getId()) || anchor.getParticipant2Id().equals(participant.getId())) {
                    markLines(anchor);
                    scheduleRemoval(anchor.getAnchorId());
                    it.remove();
                }
            }
        }

        private void removeMessage(SequenceMessage message) {
            markLines(message);

            for (SequenceNote note : message.getNotes()) {
                markLines(note);
                scheduleRemoval(note.getId());
                model.notes.remove(note);
            }

            int messageIndex = model.messages.indexOf(message);
            model.messages.remove(message);
            if (messageIndex < 0) return;

            // Check for life events and destroy markers attached to this message
            for (SequenceNode p : model.participants) {
                for (SequenceLifeEvent le : p.getLifeEvents()) {
                    if (le.getStartMessage() == messageIndex || le.getEndMessage() == messageIndex) {
                        scheduleRemoval("act-" + p.getId() + "-" + le.getStartMessage());
                    }
                }

                if (p.getDestroyIndex() == messageIndex) {
                    scheduleRemoval("dest-" + p.getId() + "-" + messageIndex);
                    p.setDestroyIndex(-1);
                }
            }

            removeOrphanedLifeEvents(Set.of(messageIndex), null);
            groupAdjuster.collectAllGroupIds();
            groupAdjuster.adjustGroups(Set.of(messageIndex));
            groupAdjuster.removeEmptyGroups();
        }

        private void removeGroup(SequenceGroup group) {
            groupAdjuster.markGroupStructureLines(group);
            model.groups.remove(group);
        }

        private void removeNote(String clickedId, SequenceNote note) {
            String rectId = null;
            for (SequenceMessage msg : model.messages) {
                if (msg.getNotes().contains(note)) {
                    rectId = msg.getMsgId() + "-note";

                    break;
                }
            }

            markLines(note);

            for (SequenceMessage msg : model.messages) {
                msg.getNotes().remove(note);
            }

            // Remove empty standalone note messages
            List<String> emptyEdges = new ArrayList<>();
            model.messages.removeIf(msg -> {
                if ("edge:note".equals(msg.getType()) && msg.getNotes().isEmpty()) {
                    emptyEdges.add(msg.getMsgId());
                    return true;

                }

                return false;
            });

            model.notes.remove(note);

            scheduleRemoval(clickedId);
            if (!clickedId.equals(note.getId())) scheduleRemoval(note.getId());
            if (rectId != null && !clickedId.equals(rectId)) scheduleRemoval(rectId);
            emptyEdges.forEach(this::scheduleRemoval);
        }

        private void removeEnglober(SequenceEnglober englober) {
            markLines(englober);
            for (SequenceNode p : model.participants) {
                p.getEngloberIds().remove(englober.getId());
            }
            model.englobers.remove(englober);
        }

        private void removeAnchor(SequenceAnchor anchor) {
            markLines(anchor);
            model.anchors.remove(anchor);
        }

        private void removeLifeEvent(SequenceLifeEvent lifeEvent) {
            markLifeEventLines(lifeEvent, Collections.emptySet());
            for (SequenceNode p : model.participants) {
                if (p.removeLifeEvent(lifeEvent)) break;
            }
        }

        private void removeDestroyCross(SequenceNode participant) {
            int idx = participant.getDestroyIndex();
            if (idx == -1) return;

            SequenceLifeEvent le = participant.getLifeEventByEndMessage(idx);
            if (le != null) {
                int endLine = le.getSourceLineEnd();
                if (endLine >= 0) {
                    if (le.isInlineEnd()) {
                        stripMarker(endLine, le.getEndMarker());

                    } else {
                        model.markLinesForDeletion(endLine, endLine);
                    }
                }
            }

            participant.setDestroyIndex(-1);
        }

        /**
         * Marks source lines for a life event's start and end, handling inline markers eparately from standalone keyword lines.
         * Skips lines already scheduled for deletion by a parent message removal.
         */
        private void markLifeEventLines(SequenceLifeEvent le, Set<Integer> deletedMsgLines) {
            int startLine = le.getSourceLineStart();
            if (startLine >= 0) {
                if (le.isInlineStart()) {
                    if (!deletedMsgLines.contains(startLine)) {
                        stripMarker(startLine, le.getStartMarker());
                    }

                } else {
                    model.markLinesForDeletion(startLine, startLine);
                }
            }

            int endLine = le.getSourceLineEnd();
            if (endLine >= 0) {
                if (le.isInlineEnd()) {
                    if (!deletedMsgLines.contains(endLine)) {
                        stripMarker(endLine, le.getEndMarker());
                    }

                } else if (le.isReturnEnd()) {
                    if (!deletedMsgLines.contains(endLine)) {
                        model.markLinesForDeletion(endLine, endLine);
                    }

                } else {
                    model.markLinesForDeletion(endLine, endLine);
                }
            }
        }

        /**
         * Removes life events that reference deleted message indices and shifts remaining life event indices to account for removed messages.
         */
        private void removeOrphanedLifeEvents(Set<Integer> removedIndices, SequenceNode exclude) {
            Set<Integer> deletedLines = new HashSet<>();
            for (int[] range : model.getLinesToDelete()) {
                for (int l = range[0]; l <= range[1]; l++) deletedLines.add(l);
            }

            for (SequenceNode p : model.participants) {
                if (p == exclude) continue;

                List<SequenceLifeEvent> orphans = new ArrayList<>();
                for (SequenceLifeEvent le : p.getLifeEvents()) {
                    if (removedIndices.contains(le.getStartMessage()) || removedIndices.contains(le.getEndMessage())) {
                        orphans.add(le);
                    }
                }

                for (SequenceLifeEvent le : orphans) {
                    markLifeEventLines(le, deletedLines);
                    p.removeLifeEvent(le);
                }

                p.shiftLifeEventIndices(removedIndices);
            }
        }

        /**
         * Cleans up destroy markers that point beyond the current message count
         */
        private void cleanupInvalidDestroys() {
            int msgCount = model.messages.size();

            for (SequenceNode p : model.participants) {
                if (p.getDestroyIndex() >= msgCount) {
                    SequenceLifeEvent le = p.getLifeEventByEndMessage(p.getDestroyIndex());
                    if (le != null && le.getSourceLineEnd() >= 0) {
                        model.markLinesForDeletion(le.getSourceLineEnd(), le.getSourceLineEnd());
                    }
                    p.setDestroyIndex(-1);
                }
            }
        }


        /**
         * Unified line marking for any model element extending SourceElement.
         */
        private void markLines(SourceElement element) {
            if (element.hasLine()) {
                model.markLinesForDeletion(element.getSourceLineStart(), element.getSourceLineEnd());
            }
        }

        /**
         * Schedules an inline marker for removal from a source line rather than deleting the entire line.
         */
        private void stripMarker(int line, String marker) {
            if (marker != null && !marker.isEmpty()) {
                model.markLineForMarkerRemoval(line, marker);
            }
        }

        private void scheduleRemoval(String id) {
            removals.add(id);
        }

        private void flushRemovals() {
            for (String id : removals) {
                removeFromTree(root, id);
            }
            removals.clear();
        }

        private boolean removeFromTree(GModelElement parent, String id) {
            Iterator<GModelElement> it = parent.getChildren().iterator();

            while (it.hasNext()) {
                GModelElement child = it.next();

                if (id.equals(child.getId())) {
                    it.remove();
                    return true;
                }

                if (removeFromTree(child, id)) return true;
            }

            return false;
        }


        private SequenceNode findParticipant(String id) {
            return model.participants.stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        private SequenceMessage findMessage(String id) {
            return model.messages.stream()
                    .filter(m -> m.getMsgId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        private SequenceGroup findGroup(String id) {
            return model.groups.stream()
                    .filter(g -> ("group-" + g.getStartIndex()).equals(id))
                    .findFirst()
                    .orElse(null);
        }

        private SequenceNote findNote(String id) {
            SequenceNote direct = model.notes.stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (direct != null) return direct;

            if (id.endsWith("-note")) {
                String parentId = id.substring(0, id.length() - "-note".length());

                for (SequenceMessage msg : model.messages) {
                    if (msg.getMsgId().equals(parentId) && !msg.getNotes().isEmpty()) {
                        return msg.getNotes().getFirst();
                    }
                }
            }

            if (id.startsWith("msg-note-")) {
                for (SequenceMessage msg : model.messages) {
                    if (msg.getMsgId().equals(id) && "edge:note".equals(msg.getType())
                            && !msg.getNotes().isEmpty()) {
                        return msg.getNotes().getFirst();
                    }
                }

                try {
                    int idx = Integer.parseInt(id.substring("msg-note-".length()));
                    int count = 0;

                    for (SequenceMessage msg : model.messages) {
                        if ("edge:note".equals(msg.getType())) {
                            if (count == idx && !msg.getNotes().isEmpty()) {
                                return msg.getNotes().getFirst();
                            }

                            count++;
                        }
                    }

                } catch (NumberFormatException ignored) {
                    // TODO
                }
            }

            return null;
        }

        private SequenceEnglober findEnglober(String id) {
            return model.englobers.stream()
                    .filter(e -> e.getId().equals(id))
                    .findFirst().orElse(null);
        }

        private SequenceAnchor findAnchor(String id) {
            return model.anchors.stream()
                    .filter(a -> a.getAnchorId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        private SequenceLifeEvent findLifeEvent(String id) {
            if (!id.startsWith("act-")) return null;

            String rest = id.substring(4);
            int lastDash = rest.lastIndexOf('-');

            try {
                String participantId = rest.substring(0, lastDash);
                int startMsg = Integer.parseInt(rest.substring(lastDash + 1));
                SequenceNode p = model.getNode(participantId);
                return p != null ? p.getLifeEventByStartMessage(startMsg) : null;

            } catch (NumberFormatException e) {
                return null;
            }
        }

        private SequenceNode findDestroyOwner(String id) {
            if (!id.startsWith("dest-")) return null;

            String rest = id.substring(5);
            int lastDash = rest.lastIndexOf('-');
            if (lastDash <= 0) return null;

            return model.getNode(rest.substring(0, lastDash));
        }


        @Override
        public boolean canUndo() {
            return false;
        }

        @Override
        public void undo() {

        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public Collection<?> getResult() {
            return Collections.emptyList();
        }

        @Override
        public Collection<?> getAffectedObjects() {
            return Collections.emptyList();
        }

        @Override
        public String getLabel() {
            return "Delete Elements";
        }

        @Override
        public String getDescription() {
            return "Delete selected elements from sequence diagram";
        }
    }
}