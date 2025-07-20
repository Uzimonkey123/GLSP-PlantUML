package com.GLSPPlantUML.utils;

public record NotePosition(double x1, double x2, String source, String target) {

    public NotePosition {
        // Swap if later participant is defined first
        if (x2 < x1) {
            double temp = x1;
            x1 = x2;
            x2 = temp;
        }
    }
}
