package com.GLSPPlantUML.utils;

import com.GLSPPlantUML.model.SequenceParts.SequenceEnglober;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EngloberRange {
    private final List<SequenceNode> participants;
    private final List<SequenceEnglober> englobers;

    public EngloberRange(List<SequenceNode> participants, List<SequenceEnglober> englobers) {
        this.participants = participants;
        this.englobers = englobers;
    }

    public void calculateEngloberRange() {
        Map<String, String> startMap = new HashMap<>();
        Map<String, String> endMap = new HashMap<>();

        for (SequenceNode p : this.participants) {
            for (String engloberId : p.getEngloberIds()) {
                if (!startMap.containsKey(engloberId)) {
                    startMap.put(engloberId, p.getId());
                }
                endMap.put(engloberId, p.getId());
            }
        }

        for (SequenceEnglober englober : this.englobers) {
            englober.setStartParticipantId(startMap.get(englober.getId()));
            englober.setEndParticipantId(endMap.get(englober.getId()));
        }
    }
}
