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

    public void adjustGroups(Set<Integer> removedIndices) {
        Iterator<SequenceGroup> it = model.groups.iterator();

        while (it.hasNext()) {
            SequenceGroup group = it.next();
            int start = group.getStartIndex();
            int end = group.getEndIndex();

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

    public void removeEmptyGroups() {
        List<SequenceGroup> sorted = new ArrayList<>(model.groups);
        sorted.sort((a, b) -> b.getSourceLineStart() - a.getSourceLineStart());

        for (SequenceGroup group : sorted) {
            if (!model.groups.contains(group)) continue;

            if (!hasDirectMessages(group)) {
                markGroupStructureLines(group);
                scheduleGroupRemoval(group);
                model.groups.remove(group);
            }
        }
    }

    public void markGroupStructureLines(SequenceGroup group) {
        if (!group.hasLine()) return;

        model.markLinesForDeletion(group.getSourceLineStart(), group.getSourceLineStart());
        model.markLinesForDeletion(group.getSourceLineEnd(), group.getSourceLineEnd());

        for (int sepLine : group.getSeparatorLineNumbers()) {
            model.markLinesForDeletion(sepLine, sepLine);
        }
    }

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

    private void rebuildSeparators(SequenceGroup group, Set<Integer> removedIndices, int newStart, int newEnd) {
        List<Integer> old = new ArrayList<>(group.getSeparatorList());
        group.clearSeparatorList();

        for (int sep : old) {
            if (removedIndices.contains(sep)) continue;

            int shift = 0;
            for (int r : removedIndices) {
                if (r < sep) shift++;
            }

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