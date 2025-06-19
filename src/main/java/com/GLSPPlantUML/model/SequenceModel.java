package com.GLSPPlantUML.model;

import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;

import java.util.ArrayList;
import java.util.List;

public class SequenceModel {
    public List<SequenceNode> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();

    public String footer;
    public String header;
    public String title;


    public SequenceModel() {}
}
