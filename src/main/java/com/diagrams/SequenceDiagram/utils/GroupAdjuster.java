/*
 * File: GroupAdjuster.java
 * Author: Norman Babiak
 * Description: Class for group utils in delete handler
 * Date: 2.4.2026
 */

package com.diagrams.SequenceDiagram.utils;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceGroup;
import com.diagrams.SequenceDiagram.model.SequenceParts.SequenceMessage;

import java.util.*;

public class GroupAdjuster {

    private final SequenceModel model;
    private final List<String> removals;

    public GroupAdjuster(SequenceModel model, List<String> removals) {
        this.model = model;
        this.removals = removals;
    }

    public void collectAllGroupIds() {
        for (SequenceGroup g : model.groups) {
            int idx = g.getStartIndex();
            removals.add("group-" + idx);
            removals.add("group-label-" + idx);
            removals.add("group-comment-" + idx);

            for (int i = 0; i < g.getSeparatorList().size(); i++) {
                removals.add("group-separator-" + idx + "-" + i);
            }
        }
    }

    /**
     * Recalculates all group boundaries after messages at removedIndices have been deleted.
     */
    public void adjustGroups(Set<Integer> removedIndices) {
        Iterator<SequenceGroup> it = model.groups.iterator();

        while (it.hasNext()) {
            SequenceGroup group = it.next();
            int start = group.getStartIndex();
            int end = group.getEndIndex();

            // Count how many removed messages fall before start and between start and end
            int startShift = 0, endShift = 0, removedInRange = 0;
            for (int r : removedIndices) {
                if (r < start) {
                    startShift++;
                    endShift++;

                } else if (r < end) {
                    endShift++;
                    removedInRange++;
                }
            }

            int newStart = start - startShift;
            int newEnd = end - endShift;

            // Group collapses if its range is empty or all internal messages were removed
            if (newEnd <= newStart || removedInRange >= (end - start)) {
                markGroupStructureLines(group);
                scheduleGroupRemoval(group);
                it.remove();

            } else {
                group.setStartIndex(newStart);
                group.setEndIndex(newEnd);
                rebuildSeparators(group, removedIndices, newStart, newEnd);
            }
        }
    }

    /**
     * Removes groups that no longer contain any direct messages after deletion.
     */
    public void removeEmptyGroups() {
        List<SequenceGroup> sorted = new ArrayList<>(model.groups);
        sorted.sort((a, b) -> b.getSourceLineStart() - a.getSourceLineStart());

        for (SequenceGroup group : sorted) {
            // Group may have been removed by a previous iteration
            if (!model.groups.contains(group)) continue;

            if (!hasDirectMessages(group)) {
                markGroupStructureLines(group);
                scheduleGroupRemoval(group);
                model.groups.remove(group);
            }
        }
    }

    /**
     * Method for marking all lines of the group, start, end and separators
     */
    public void markGroupStructureLines(SequenceGroup group) {
        if (!group.hasLine()) return;

        model.markLinesForDeletion(group.getSourceLineStart(), group.getSourceLineStart());
        model.markLinesForDeletion(group.getSourceLineEnd(), group.getSourceLineEnd());

        for (int sepLine : group.getSeparatorLineNumbers()) {
            model.markLinesForDeletion(sepLine, sepLine);
        }
    }

    /**
     * Check if message has atleast one message in its own nested level
     */
    private boolean hasDirectMessages(SequenceGroup group) {
        int gStart = group.getSourceLineStart();
        int gEnd = group.getSourceLineEnd();

        for (SequenceMessage msg : model.messages) {
            if (!msg.hasLine()) continue;
            int line = msg.getSourceLineStart();
            if (line <= gStart || line >= gEnd) continue;

            boolean nested = model.groups.stream()
                    .filter(g -> g != group)
                    .anyMatch(g -> g.getSourceLineStart() > gStart
                            && g.getSourceLineEnd() < gEnd
                            && line > g.getSourceLineStart()
                            && line < g.getSourceLineEnd());

            if (!nested) return true;
        }

        return false;
    }

    /**
     * Rebuilds a group's separator list after message deletion.
     */
    private void rebuildSeparators(SequenceGroup group, Set<Integer> removedIndices, int newStart, int newEnd) {
        List<Integer> old = new ArrayList<>(group.getSeparatorList());
        group.clearSeparatorList();

        for (int sep : old) {
            // Drop separators that pointed to a removed message
            if (removedIndices.contains(sep)) continue;

            // Shift the separator index down by the count of removed messages before it
            int shift = 0;
            for (int r : removedIndices) {
                if (r < sep) shift++;
            }

            // Only keep the separator if it still falls within the group's new range
            int newSep = sep - shift;
            if (newSep >= newStart && newSep < newEnd) {
                group.addSeparator(newSep);
            }
        }
    }

    private void scheduleGroupRemoval(SequenceGroup group) {
        int idx = group.getStartIndex();
        removals.add("group-" + idx);
        removals.add("group-label-" + idx);
        removals.add("group-comment-" + idx);

        for (int i = 0; i < group.getSeparatorList().size(); i++) {
            removals.add("group-separator-" + idx + "-" + i);
        }
    }
}