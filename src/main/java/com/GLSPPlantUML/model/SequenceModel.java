package com.GLSPPlantUML.model;

import com.GLSPPlantUML.model.SequenceParts.SequenceAnchor;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
