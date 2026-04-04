/*
 * File: LineFinder.java
 * Author: Norman Babiak
 * Description: Locates model elements in the PlantUML source by line type and content matching.
 * Date: 4.4.2026
 */

package com.diagrams.SequenceDiagram.reconstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LineFinder {
    private final LineMapper lineMapper;
    private final Map<Object, Integer> eventToLineMap;
    private int searchFrom;

    public LineFinder(LineMapper lineMapper, Map<Object, Integer> eventToLineMap) {
        this.lineMapper = lineMapper;
        this.eventToLineMap = eventToLineMap;
        this.searchFrom = 0;
    }

    public int findParticipantLine(String name, Object event) {
        return findLine(LineMapper.LineType.PARTICIPANT, name, event);
    }

    public int findMessageLine(String label, Object event) {
        return findLine(LineMapper.LineType.MESSAGE, label, event);
    }

    public int findDividerLine(String label, Object event) {
        return findLine(LineMapper.LineType.DIVIDER, label, event);
    }

    public int findDelayLine(String label, Object event) {
        return findLine(LineMapper.LineType.DELAY, label, event);
    }

    public int findReferenceLine(String text, Object event) {
        return findLine(LineMapper.LineType.REFERENCE, text, event);
    }

    public int findEndReferenceLine(String text, Object event) {
        return findLine(LineMapper.LineType.END_REFERENCE, text, event);
    }

    public int findGroupStartLine(String label, Object event) {
        return findLine(LineMapper.LineType.GROUP_START, label, event);
    }

    public int findGroupElseLine(String label, Object event) {
        return findLine(LineMapper.LineType.GROUP_ELSE, label, event);
    }

    public int findGroupEndLine(Object event) {
        return findLine(LineMapper.LineType.GROUP_END, null, event);
    }

    public int findAnchorLine(String startId, Object event) {
        String searchPattern = "{" + startId + "}";
        return findLine(LineMapper.LineType.ANCHOR, searchPattern, event);
    }

    public int findActivateLine(String participantName, Object event) {
        return findLineFromStart(LineMapper.LineType.ACTIVATE, participantName, event);
    }

    public int findDeactivateLine(String participantName, Object event) {
        return findLineFromStart(LineMapper.LineType.DEACTIVATE, participantName, event);
    }

    public int findDestroyLine(String participantName, Object event) {
        return findLineFromStart(LineMapper.LineType.DESTROY, participantName, event);
    }

    public int findReturnLine(String text, Object event) {
        return findLine(LineMapper.LineType.RETURN, text, event);
    }

    public int findCreateLine(String participantName, Object event) {
        return findLine(LineMapper.LineType.CREATE, participantName, event);
    }

    public int findEngloberLine(String boxName, Object event) {
        // Save searchFrom index because englober is in participant
        int savedSearchFrom = getCurrentPosition();
        resetSearch();

        int result = findLine(LineMapper.LineType.ENGLOBER, boxName, event);

        setPosition(savedSearchFrom);
        return result;
    }

    public int findNoteLine(String text, Object event) {
        return findLine(LineMapper.LineType.NOTE, text, event);
    }

    public int findNoteEndLine(Object event) {
        return findLine(LineMapper.LineType.END_NOTE, null, event);
    }

    private int findLine(LineMapper.LineType type, String searchText, Object event) {
        List<LineMapper.LineInfo> allLines = lineMapper.getLineInfos();
        int totalLines = allLines.size();
        boolean hasSearchText = searchText != null && !searchText.isEmpty();

        for (int i = searchFrom; i < totalLines; i++) {
            LineMapper.LineInfo info = allLines.get(i);

            if (info.type == type &&
               (!hasSearchText || info.originalText.contains(searchText))) {

                if (event != null) {
                    eventToLineMap.put(event, i);
                }

                searchFrom = i + 1;
                return i;
            }
        }

        return -1;
    }

    private final Set<Integer> usedLifeEventLines = new HashSet<>();

    private int findLineFromStart(LineMapper.LineType type, String searchText, Object event) {
        List<LineMapper.LineInfo> allLines = lineMapper.getLineInfos();
        boolean hasSearchText = searchText != null && !searchText.isEmpty();

        for (int i = 0; i < allLines.size(); i++) {
            LineMapper.LineInfo info = allLines.get(i);

            if (usedLifeEventLines.contains(i)) continue;

            if (info.type == type &&
                (!hasSearchText || info.originalText.contains(searchText))) {

                if (event != null) {
                    eventToLineMap.put(event, i);
                }

                usedLifeEventLines.add(i);
                return i;
            }
        }

        return -1;
    }

    public int getCurrentPosition() {
        return searchFrom;
    }

    public void setPosition(int position) {
        this.searchFrom = position;
    }

    public void resetSearch() {
        this.searchFrom = 0;
    }
}