package com.GLSPPlantUML.reconstructor;

import java.util.List;
import java.util.Map;

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
        return findLine(LineMapper.LineType.ACTIVATE, participantName, event);
    }

    public int findDeactivateLine(String participantName, Object event) {
        return findLine(LineMapper.LineType.DEACTIVATE, participantName, event);
    }

    public int findDestroyLine(String participantName, Object event) {
        return findLine(LineMapper.LineType.DESTROY, participantName, event);
    }

    public int findReturnLine(String text, Object event) {
        return findLine(LineMapper.LineType.RETURN, text, event);
    }

    public int findCreateLine(String participantName, Object event) {
        return findLine(LineMapper.LineType.CREATE, participantName, event);
    }

    public int findEngloberLine(String boxName, Object event) {
        // Save searchFrom index because englober is in participant
        int savedSearchFrom = searchFrom;
        searchFrom = 0;

        int result = findLine(LineMapper.LineType.ENGLOBER, boxName, event);

        searchFrom = savedSearchFrom;
        return result;
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

    public void resetSearch() {
        this.searchFrom = 0;
    }
}