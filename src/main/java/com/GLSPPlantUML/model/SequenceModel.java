package com.GLSPPlantUML.model;

import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceGroup;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;

import java.util.*;

public class SequenceModel {
    public List<SequenceNode> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();
    public List<SequenceAnchor> anchors = new ArrayList<>();
    public Map<Integer, Integer> messageSpaces = new HashMap<>();
    public List<SequenceGroup> groups = new ArrayList<>();

    public String footer;
    public String header;
    public String title;
    public boolean showFoot;


    public SequenceModel() {}

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
}
