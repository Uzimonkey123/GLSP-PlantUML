package com.GLSPPlantUML.utils;

import com.GLSPPlantUML.reconstructor.LineMapper;
import net.sourceforge.plantuml.sequencediagram.Event;

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