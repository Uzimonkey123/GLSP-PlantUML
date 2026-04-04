/*
 * File: SequenceModeljava
 * Author: Norman Babiak
 * Description: Model for Sequence diagram, holding all related nodes, edges, details and lookups
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.model;

import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import com.diagrams.SequenceDiagram.utils.ElementRelocator;

import java.util.*;

public class SequenceModel {
    public List<SequenceNode> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();
    public List<SequenceAnchor> anchors = new ArrayList<>();
    public Map<Integer, Integer> messageSpaces = new HashMap<>();
    public List<SequenceGroup> groups = new ArrayList<>();
    public List<SequenceEnglober> englobers = new ArrayList<>();
    public List<SequenceNote> notes = new ArrayList<>();

    public int headerLineStart = -1;
    public int headerLineEnd = -1;
    public int footerLineStart = -1;
    public int footerLineEnd = -1;
    public int titleLineStart = -1;
    public int titleLineEnd = -1;
    public int mainframeLineNumber = -1;

    public boolean headerModified = false;
    public boolean footerModified = false;
    public boolean titleModified = false;
    public boolean mainframeModified = false;

    public String footer;
    public String header;
    public String title;
    public String mainframe;

    public boolean showFoot;
    public boolean invisibleNodes = false;
    public boolean isMainframe = false;

    LineMapper lineMapper;

    private final List<int[]> linesToDelete = new ArrayList<>();
    private final List<LineModification> lineModifications = new ArrayList<>();

    public static class LineModification {
        public final int lineNumber;
        public final String marker;

        public LineModification(int lineNumber, String marker) {
            this.lineNumber = lineNumber;
            this.marker = marker;
        }
    }

    public SequenceModel() {}

    public void setMapper(LineMapper lineMapper) {
        this.lineMapper = lineMapper;
    }

    public LineMapper getLineMapper() {
        return lineMapper;
    }


    public void markLinesForDeletion(int start, int end) {
        linesToDelete.add(new int[]{start, end});
    }

    public List<int[]> getLinesToDelete() {
        return linesToDelete;
    }

    public void clearLinesToDelete() {
        linesToDelete.clear();
    }

    public void markLineForMarkerRemoval(int lineNumber, String marker) {
        lineModifications.add(new LineModification(lineNumber, marker));
    }

    public void relocateAllElements(String newSourceText) {
        new ElementRelocator(this).relocateAll(newSourceText);
    }

    public SequenceNode getNode(String id) {
        return participants.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                // Get warning ignore, since node definitely exists at this point
                .get();
    }

    public String getNextParticipant(String current) {
        for (int i = 0; i < participants.size() - 1; i++) {
            if (participants.get(i).getId().equals(current)) {
                return participants.get(i + 1).getId();
            }
        }

        return current;
    }

    public String getPreviousParticipant(String current) {
        for (int i = 1; i < participants.size(); i++) {
            if (participants.get(i).getId().equals(current)) {
                return participants.get(i - 1).getId();
            }
        }

        return current;
    }

    public Collection<SequenceEnglober> reversedEnglobers() {
        List<SequenceEnglober> reversedList = new ArrayList<>(this.englobers);
        Collections.reverse(reversedList);

        return reversedList;
    }
}
