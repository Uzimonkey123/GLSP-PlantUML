package com.diagrams.ClassDiagram.utils;

public class NoteCalculator {

    private final double charWidth = 6.5;
    private final double noteHorizontalOffset = 20.0;
    private final double noteVerticalOffset = 10.0;
    private final double safetyMargin = 30.0;
    private final double finalAdjustment = 15.0;

    public GeometryUtils.Dimensions calculateNoteDimensions(String noteText) {
        String[] lines = noteText.split("<br>");
        int maxLineLength = 0;

        for (String line : lines) {
            maxLineLength = Math.max(maxLineLength, line.length());
        }

        double notePadding = 10.0;
        double minNoteWidth = 100.0;
        double width = Math.max(maxLineLength * charWidth + notePadding * 2, minNoteWidth);
        double lineHeight = 14.0;
        double height = lines.length * lineHeight + notePadding * 2;

        return new GeometryUtils.Dimensions(width, height);
    }

    public GeometryUtils.Point adjustLabelForNote(GeometryUtils.Point labelPos, String message,
                                                  GeometryUtils.Dimensions noteDim,
                                                  String notePosition, double linkXAtLabelY) {
        if (notePosition == null) notePosition = "RIGHT";
        double labelWidth = (message != null ? message.length() : 0) * charWidth;

        return switch (notePosition) {
            case "TOP" -> labelPos.offset(0, noteDim.height() / 2 + noteVerticalOffset);
            case "BOTTOM" -> labelPos.offset(0, -(noteDim.height() / 2 + noteVerticalOffset));
            case "LEFT" -> {
                double noteX = labelPos.x() - labelWidth / 2 - noteHorizontalOffset - noteDim.width();
                if (noteX < linkXAtLabelY) {
                    yield labelPos.offset(linkXAtLabelY - noteX + safetyMargin, 0);
                }
                yield labelPos;
            }
            default -> {
                double labelLeftEdge = labelPos.x() - labelWidth / 2;
                if (labelLeftEdge < linkXAtLabelY) {
                    yield labelPos.offset(linkXAtLabelY - labelLeftEdge + safetyMargin, 0);
                }
                yield labelPos;
            }
        };
    }

    public GeometryUtils.Point calculateNotePosition(GeometryUtils.Point labelPos, String message,
                                                     GeometryUtils.Dimensions noteDim,
                                                     String notePosition, double linkXAtLabelY) {
        if (notePosition == null) notePosition = "RIGHT";
        double labelWidth = (message != null ? message.length() : 0) * charWidth;

        GeometryUtils.Point notePos = switch (notePosition) {
            case "TOP" -> new GeometryUtils.Point(
                    labelPos.x() - noteDim.width() / 2,
                    labelPos.y() - noteDim.height() - noteVerticalOffset
            );
            case "BOTTOM" -> new GeometryUtils.Point(
                    labelPos.x() - noteDim.width() / 2,
                    labelPos.y() + noteVerticalOffset
            );
            case "LEFT" -> new GeometryUtils.Point(
                    labelPos.x() - labelWidth / 2 - noteHorizontalOffset - noteDim.width(),
                    labelPos.y() - noteDim.height() / 2
            );
            default -> new GeometryUtils.Point(
                    labelPos.x() + labelWidth / 2 + noteHorizontalOffset,
                    labelPos.y() - noteDim.height() / 2
            );
        };

        if (notePos.x() < linkXAtLabelY) {
            notePos = notePos.offset(linkXAtLabelY - notePos.x() + safetyMargin, 0);
        }

        return notePos.offset(0, -finalAdjustment);
    }
}