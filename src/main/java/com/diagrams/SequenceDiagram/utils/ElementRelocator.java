/*
 * File: ElementRelocator.java
 * Author: Norman Babiak
 * Description: Refreshes source-line references for all elements
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.utils;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.reconstructor.LineFinder;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;

import java.util.*;

public class ElementRelocator {

    private final SequenceModel model;
    private LineMapper lineMapper;

    public ElementRelocator(SequenceModel model) {
        this.model = model;
    }

    public void relocateAll(String newSourceText) {
        this.lineMapper = new LineMapper(newSourceText);
        model.setMapper(lineMapper);

        Map<Object, Integer> elementToLineMap = new HashMap<>();
        LineFinder lineFinder = new LineFinder(lineMapper, elementToLineMap);

        relocateParticipants(lineFinder);
        relocateMessages(lineFinder);
        relocateGroups(lineFinder);
        relocateEnglobers(lineFinder);
        relocateAnchors(lineFinder);
        relocatePageElements();

        model.titleModified = false;
        model.headerModified = false;
        model.footerModified = false;
        model.mainframeModified = false;

        model.clearLinesToDelete();
    }

    private void relocateParticipants(LineFinder lineFinder) {
        lineFinder.resetSearch();

        for (SequenceNode node : model.participants) {
            String name = node.getName();

            lineFinder.resetSearch();
            int line = lineFinder.findParticipantLine(name, node);
            if (line < 0) {
                line = lineFinder.findCreateLine(name, node);
            }

            if (line >= 0) {
                node.setSourceLines(line, line);
                LineMapper.LineInfo info = lineMapper.getLineInfo(line);
                if (info != null) node.setRawSourceText(info.originalText);

            } else {
                node.setSourceLines(-1, -1);
            }

            node.clearModified();
            relocateLifeEvents(node);
        }
    }

    private void relocateLifeEvents(SequenceNode node) {
        String participantName = node.getName();
        Set<Integer> usedLines = new HashSet<>();

        // Sort by start message index so activation/deactivation pairs are matched in order
        List<SequenceLifeEvent> lifeEvents = new ArrayList<>(node.getLifeEvents());
        lifeEvents.sort(Comparator.comparingInt(SequenceLifeEvent::getStartMessage));

        List<Integer> activateLines = collectKeywordLines("activate ", participantName);
        List<Integer> deactivateLines = collectKeywordLines("deactivate ", participantName);
        List<Integer> destroyLines = collectKeywordLines("destroy ", participantName);

        for (SequenceLifeEvent le : lifeEvents) {
            le.setInlineStart(false);
            le.setInlineEnd(false);
            le.setReturnEnd(false);
            le.setStartMarker("");
            le.setEndMarker("");

            int activateLine = findNextUnused(activateLines, usedLines, -1);

            // If the life event ends at the destroy index, look for "destroy" instead of "deactivate"
            boolean endsWithDestroy = (le.getEndMessage() == node.getDestroyIndex());
            List<Integer> endLines = endsWithDestroy ? destroyLines : deactivateLines;
            int endLine = findNextUnused(endLines, usedLines, activateLine);

            if (activateLine >= 0 || endLine >= 0) {
                le.setSourceLines(activateLine, endLine);

            } else {
                le.setSourceLines(-1, -1);
            }
        }

        for (SequenceLifeEvent le : lifeEvents) {
            if (le.getSourceLineStart() < 0) matchInlineStart(le);
            if (le.getSourceLineEnd() < 0) matchInlineEnd(le);
        }
    }

    /**
     * Scans all source lines for standalone keyword declarations matching a participant.
     */
    private List<Integer> collectKeywordLines(String keyword, String participantName) {
        List<Integer> lines = new ArrayList<>();

        for (LineMapper.LineInfo info : lineMapper.getLineInfos()) {
            String text = info.originalText.trim().toLowerCase();
            if (text.startsWith(keyword) && matchesParticipant(text.substring(keyword.length()), participantName)) {
                lines.add(info.lineNumber);
            }
        }

        return lines;
    }

    /**
     * Returns the first line from candidates that hasn't been used yet and appears after the given line number.
     */
    private int findNextUnused(List<Integer> candidates, Set<Integer> usedLines, int afterLine) {
        for (int line : candidates) {
            if (!usedLines.contains(line) && line > afterLine) {
                usedLines.add(line);

                return line;
            }
        }

        return -1;
    }

    private void matchInlineStart(SequenceLifeEvent le) {
        int startMsgIdx = le.getStartMessage();
        if (startMsgIdx < 0 || startMsgIdx >= model.messages.size()) return;

        SequenceMessage startMsg = model.messages.get(startMsgIdx);
        if (!startMsg.hasLine()) return;

        LineMapper.LineInfo info = lineMapper.getLineInfo(startMsg.getSourceLineStart());
        if (info == null) return;

        String marker = findInlineActivationMarker(info.originalText);
        if (marker != null) {
            le.setSourceLines(startMsg.getSourceLineStart(), le.getSourceLineEnd());
            le.setInlineStart(true);
            le.setStartMarker(marker);
        }
    }

    private void matchInlineEnd(SequenceLifeEvent le) {
        int endMsgIdx = le.getEndMessage();
        if (endMsgIdx < 0 || endMsgIdx >= model.messages.size()) return;

        SequenceMessage endMsg = model.messages.get(endMsgIdx);
        if (!endMsg.hasLine()) return;

        LineMapper.LineInfo info = lineMapper.getLineInfo(endMsg.getSourceLineStart());
        if (info == null) return;

        String lineText = info.originalText.trim();

        // "return" is a special case
        if (lineText.toLowerCase().startsWith("return")) {
            le.setSourceLines(le.getSourceLineStart(), endMsg.getSourceLineStart());
            le.setReturnEnd(true);
            le.setEndMarker("return");

        } else {
            String marker = findInlineDeactivationMarker(info.originalText);
            if (marker != null) {
                le.setSourceLines(le.getSourceLineStart(), endMsg.getSourceLineStart());
                le.setInlineEnd(true);
                le.setEndMarker(marker);
            }
        }
    }

    private void relocateMessages(LineFinder lineFinder) {
        lineFinder.resetSearch();

        for (SequenceMessage message : model.messages) {
            String label = message.getMessage();
            String type = message.getType();
            int line;

            switch (type) {
                case "edge:delay" -> line = lineFinder.findDelayLine(label, message);
                case "edge:divider" -> line = lineFinder.findDividerLine(label, message);

                case "edge:ref" -> {
                    String fromName = message.getFrom() != null ? message.getFrom().getName() : "";
                    line = lineFinder.findReferenceLine("ref over " + fromName, message);

                    // Multi-line references need both start and end lines
                    if (line >= 0 && label.contains("<br>")) {
                        int endLine = lineFinder.findEndReferenceLine("end ref", message);
                        message.setSourceLines(line, endLine >= 0 ? endLine : line);
                        LineMapper.LineInfo info = lineMapper.getLineInfo(line);
                        if (info != null) message.setRawSourceText(info.originalText);

                        message.clearModified();
                        relocateMessageNotes(lineFinder, message);

                        continue;
                    }
                }

                case "edge:note" -> {
                    message.setSourceLines(-1, -1);
                    message.clearModified();
                    relocateMessageNotes(lineFinder, message);

                    continue;
                }

                // Regular messages try message arrow syntax first, then "return" syntax
                default -> {
                    line = lineFinder.findMessageLine(label, message);
                    if (line < 0) line = lineFinder.findReturnLine(label, message);
                }
            }

            if (line >= 0) {
                message.setSourceLines(line, line);
                LineMapper.LineInfo info = lineMapper.getLineInfo(line);
                if (info != null) message.setRawSourceText(info.originalText);

            } else {
                message.setSourceLines(-1, -1);
            }

            message.clearModified();
            relocateMessageNotes(lineFinder, message);
        }
    }

    private void relocateMessageNotes(LineFinder lineFinder, SequenceMessage message) {
        for (SequenceNote note : message.getNotes()) {
            int startLine = lineFinder.findNoteLine(null, note);
            int endLine = startLine;

            if (startLine >= 0) {
                LineMapper.LineInfo info = lineMapper.getLineInfo(startLine);
                if (info != null && !info.originalText.contains(":")) {
                    endLine = lineFinder.findNoteEndLine(note);
                }

                note.setSourceLines(startLine, endLine >= 0 ? endLine : startLine);
                LineMapper.LineInfo startInfo = lineMapper.getLineInfo(startLine);
                if (startInfo != null) note.setRawSourceText(startInfo.originalText);

            } else {
                note.setSourceLines(-1, -1);
            }

            note.clearModified();
        }
    }

    private void relocateGroups(LineFinder lineFinder) {
        lineFinder.resetSearch();

        for (SequenceGroup group : model.groups) {
            int startLine = lineFinder.findGroupStartLine(group.getLabel(), group);

            if (startLine >= 0) {
                int endLine = lineFinder.findGroupEndLine(group);
                group.setSourceLines(startLine, endLine >= 0 ? endLine : startLine);
                LineMapper.LineInfo info = lineMapper.getLineInfo(startLine);
                if (info != null) group.setRawSourceText(info.originalText);

                // Re-resolve separator lines by scanning forward from the group start
                group.getSeparatorLineNumbers().clear();
                lineFinder.setPosition(startLine);

                for (String sepLabel : group.getSeparatorLabel()) {
                    int sepLine = lineFinder.findGroupElseLine(sepLabel != null ? sepLabel : "", group);
                    if (sepLine >= 0) group.addSeparatorLineNumber(sepLine);
                }

            } else {
                group.setSourceLines(-1, -1);
            }

            group.clearModified();
        }
    }

    private void relocateEnglobers(LineFinder lineFinder) {
        lineFinder.resetSearch();

        for (SequenceEnglober englober : model.englobers) {
            int line = lineFinder.findEngloberLine(englober.getLabel(), englober);

            if (line >= 0) {
                englober.setSourceLines(line, line);
                LineMapper.LineInfo info = lineMapper.getLineInfo(line);
                if (info != null) englober.setRawSourceText(info.originalText);
            } else {
                englober.setSourceLines(-1, -1);
            }

            englober.clearModified();
        }
    }

    private void relocateAnchors(LineFinder lineFinder) {
        lineFinder.resetSearch();

        for (SequenceAnchor anchor : model.anchors) {
            int line = lineFinder.findAnchorLine("start", anchor);

            if (line >= 0) {
                anchor.setSourceLines(line, line);
                LineMapper.LineInfo info = lineMapper.getLineInfo(line);
                if (info != null) anchor.setRawSourceText(info.originalText);

            } else {
                anchor.setSourceLines(-1, -1);
            }

            anchor.clearModified();
        }
    }

    private void relocatePageElements() {
        model.titleLineStart = findLineByType(LineMapper.LineType.TITLE);
        model.titleLineEnd = findLineByType(LineMapper.LineType.END_TITLE);
        if (model.titleLineEnd < 0) model.titleLineEnd = model.titleLineStart;

        model.headerLineStart = findLineByType(LineMapper.LineType.HEADER);
        model.headerLineEnd = findLineByType(LineMapper.LineType.END_HEADER);
        if (model.headerLineEnd < 0) model.headerLineEnd = model.headerLineStart;

        model.footerLineStart = findLineByType(LineMapper.LineType.FOOTER);
        model.footerLineEnd = findLineByType(LineMapper.LineType.END_FOOTER);
        if (model.footerLineEnd < 0) model.footerLineEnd = model.footerLineStart;

        model.mainframeLineNumber = findLineByType(LineMapper.LineType.MAINFRAME);
    }

    /**
     * Checks the arrow portion of a message line for inline activation markers.
     */
    private String findInlineActivationMarker(String lineText) {
        String arrowPart = getArrowPart(lineText);
        if (arrowPart.contains("++")) return "++";
        if (arrowPart.contains("**")) return "**";

        return null;
    }

    /**
     * Checks the arrow portion of a message line for inline deactivation markers.
     */
    private String findInlineDeactivationMarker(String lineText) {
        String arrowPart = getArrowPart(lineText);
        if (arrowPart.contains("--")) return "--";
        if (arrowPart.contains("!!")) return "!!";

        return null;
    }

    private String getArrowPart(String lineText) {
        return (lineText.indexOf(':') >= 0) ? lineText.substring(0, lineText.indexOf(':')) : lineText;
    }

    private boolean matchesParticipant(String textAfterKeyword, String participantName) {
        textAfterKeyword = textAfterKeyword.trim();

        // Plain name match
        if (textAfterKeyword.toLowerCase().startsWith(participantName.toLowerCase())) {
            return true;
        }

        // Quoted name match
        if ((textAfterKeyword.startsWith("\"") || textAfterKeyword.startsWith("'")) && textAfterKeyword.length() > 2) {
            char quote = textAfterKeyword.charAt(0);
            int endQuote = textAfterKeyword.indexOf(quote, 1);

            if (endQuote > 0) {
                return textAfterKeyword.substring(1, endQuote).equalsIgnoreCase(participantName);
            }
        }

        return false;
    }

    private int findLineByType(LineMapper.LineType type) {
        for (LineMapper.LineInfo info : lineMapper.getLineInfos()) {
            if (info.type == type) return info.lineNumber;
        }

        return -1;
    }
}
