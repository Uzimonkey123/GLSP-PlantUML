package com.GLSPPlantUML.model;

import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;

import java.util.*;

public class SequenceModel {
    public List<SequenceNode> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();
    public List<SequenceAnchor> anchors = new ArrayList<>();
    public Map<Integer, Integer> messageSpaces = new HashMap<>();

    public String footer;
    public String header;
    public String title;
    public boolean showFoot;


    public SequenceModel() {}

    public SequenceNode getNode(String name) {
        return participants.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                // Get warning ignore, since node definitely exists at this point
                .get();
    }
}
