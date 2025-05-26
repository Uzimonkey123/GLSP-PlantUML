package com.GLSPPlantUML.model;

import java.util.ArrayList;
import java.util.List;

public class SequenceModel {
    public static class SequenceMessage {
        private final String from;
        private final String to;
        private final String message;
        public SequenceMessage(String from, String to, String message) {
            this.from = from;
            this.to = to;
            this.message = message;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getMessage() {
            return message;
        }
    }

    public List<String> participants = new ArrayList<>();
    public List<SequenceMessage> messages = new ArrayList<>();

    public SequenceModel() {}
}
