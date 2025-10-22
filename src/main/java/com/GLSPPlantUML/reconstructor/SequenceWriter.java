package com.GLSPPlantUML.reconstructor;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.*;
import com.GLSPPlantUML.utils.IndentatHelper;
import com.GLSPPlantUML.utils.NewLine;
import com.GLSPPlantUML.utils.ReconstructorHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class SequenceWriter {
    private final SequenceModel model;
    private final File source;
    private final List<String> sourceLines;
    private final Map<Integer, NewLine> newLines;
    private final LineMapper lineMap;

    public SequenceWriter(SequenceModel model, String sourceUri) throws IOException {
        this.model = model;
        this.source = new File(URI.create(sourceUri));
        this.sourceLines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);
        this.newLines = new HashMap<>();
        this.lineMap = model.getLineMapper();
    }

    public void write() throws IOException {
        newLines.clear();

        writeParticipant();
        writeMessage();
        writeAnchor();
        writeGroup();
        writePageDetails();
        writeEnglobers();

        applyReplacements();
        saveAtomic();
    }

    private void changeLine(int start, int end, List<String> lines) {
        if (!newLines.containsKey(start)) {
            newLines.put(start, new NewLine(start, end, lines));
        }
    }

    private void writeParticipant() {
        for (SequenceNode node : model.participants) {
            System.err.println("Writing participant " + node.isModified());
            System.err.println("Has line " + node.hasLine());
            if (!node.isModified()) continue;

            if (node.hasLine()) {
                changeLine(node.getSourceLineStart(), node.getSourceLineEnd(), List.of(replaceParticipant(node)));
            }

            for (SequenceMessage message : model.messages) {
                if (message.hasLine() && ((message.getFrom() != null && message.getFrom().equals(node)) ||
                     (message.getTo() != null && message.getTo().equals(node))))
                {
                    changeLine(message.getSourceLineStart(), message.getSourceLineEnd(),
                            List.of(replaceMessage(message)));
                }

                for (SequenceNote note : message.getNotes()) {
                    writeNote(message, note);
                }
            }

            for (SequenceLifeEvent event : node.getLifeEvents()) {
                if (!event.hasLine()) continue;

                int startLine = event.getSourceLineStart();
                int endLine = event.getSourceLineEnd();

                LineMapper.LineInfo startLineInfo = lineMap.getLineInfo(startLine);
                if (startLineInfo == null) continue;

                if (startLineInfo.type == LineMapper.LineType.DESTROY) {
                    changeLine(startLine, endLine, List.of(replaceDestroy(node, event)));
                } else {
                    changeLine(startLine, startLine, List.of(replaceActivate(node, event)));
                }

                if (startLine != endLine) {
                    changeLine(endLine, endLine, List.of(replaceDeactivate(node, event)));
                }
            }
        }
    }

    private String replaceParticipant(SequenceNode node) {
        StringBuilder sb = new StringBuilder();
        System.err.println("Replace participant " + node.isModified());

        String source = node.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);
        boolean isCreate = lineMap.getLineInfo(node.getSourceLineStart()).type == LineMapper.LineType.CREATE;

        if (isCreate) {
            sb.append("create ");
            if (!node.getType().equals("PARTICIPANT")) {
                sb.append(node.getType().toLowerCase()).append(" ");
            }

        } else {
            sb.append(node.getType().toLowerCase()).append(" ");
        }

        String alias = ReconstructorHelper.extractAlias(source);

        if (alias != null && !alias.isEmpty()) {
            String name = node.getName();
            if (!name.matches("[a-zA-Z0-9_]+")) {
                sb.append("\"").append(name).append("\"");

            } else {
                sb.append(name);
            }
            sb.append(" as ").append(alias);

        } else {
            sb.append(ReconstructorHelper.getParticipant(node));
        }

        if (node.getOrder() != 0) {
            sb.append(" order ").append(node.getOrder());
        }

        if (!node.getBackground().equals("#5d4949")) {
            sb.append(" ").append(node.getBackground());
        }

        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private String replaceDestroy(SequenceNode node, SequenceLifeEvent event) {
        StringBuilder sb = new StringBuilder("destroy ");

        String source = event.rawSourceText;
        String indent = IndentatHelper.extractIndentation(source);

        sb.append(ReconstructorHelper.getParticipant(node));
        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private String replaceActivate(SequenceNode node, SequenceLifeEvent event) {
        StringBuilder sb = new StringBuilder("activate ");

        String source = event.rawSourceText;
        String indent = IndentatHelper.extractIndentation(source);

        sb.append(ReconstructorHelper.getParticipant(node));
        sb.append(" ").append(event.getBackground());
        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private String replaceDeactivate(SequenceNode node, SequenceLifeEvent event) {
        StringBuilder sb = new StringBuilder("deactivate ");

        String source = event.rawSourceText;
        String indent = IndentatHelper.extractIndentation(source);

        sb.append(ReconstructorHelper.getParticipant(node));
        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private void writeMessage() {
        for (SequenceMessage message : model.messages) {
            if (message.hasLine() && message.isModified()) {
                changeLine(message.getSourceLineStart(), message.getSourceLineEnd(), List.of(replaceMessage(message)));
            }

            for (SequenceNote note : message.getNotes()) {
                if (!note.isModified()) continue;

                writeNote(message, note);
            }
        }
    }

    private String replaceMessage(SequenceMessage message) {
        String sourceText = message.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(sourceText);
        String type = message.getType();

        switch (type) {
            case "edge:delay" -> {
                return IndentatHelper.applyIndentation("..." + message.getMessage() + "...", indent);
            }
            case "edge:divider" -> {
                return IndentatHelper.applyIndentation("==" + message.getMessage() + "==", indent);
            }
            case "edge:ref" -> {
                return IndentatHelper.applyIndentation(referenceMessage(message), indent);
            }
        }

        if (lineMap.getLineInfo(message.getSourceLineStart()).type == LineMapper.LineType.RETURN) {
            return IndentatHelper.applyIndentation("return " + message.getMessage(), indent);
        }

        return IndentatHelper.applyIndentation(regularMessage(message), indent);
    }

    private String regularMessage(SequenceMessage message) {
        StringBuilder sb = new StringBuilder();

        if (message.isParallel()) sb.append("& ");
        if (message.isAnchorStart()) sb.append("{start} ");
        if (message.isAnchorEnd()) sb.append("{end} ");

        if (message.getFrom() != null) {
            sb.append(ReconstructorHelper.getParticipant(message.getFrom())).append(" ");
        } else if (message.getFromId().equals("[")) {
            sb.append(message.isShort() ? "?" : "[");
        }

        sb.append(messageArrow(message));

        if (message.getTo() != null) {
            sb.append(" ");
            sb.append(ReconstructorHelper.getParticipant(message.getTo())).append(" ");
        } else if (message.getToId().equals("]")) {
            sb.append(message.isShort() ? "?" : "]").append(" ");
        }

        sb.append(ReconstructorHelper.extractLifeEventSymbol(message.getRawSourceText()));
        sb.append(": ");
        sb.append(message.getMessage());

        return sb.toString();
    }

    private String referenceMessage(SequenceMessage message) {
        StringBuilder sb = new StringBuilder("ref over ");

        if (message.getFrom() != null) {
            sb.append(ReconstructorHelper.getParticipant(message.getFrom()));
        }

        if (message.getTo() != null && !message.getFrom().equals(message.getTo())) {
            sb.append(", ");
            sb.append(ReconstructorHelper.getParticipant(message.getTo()));
        }

        String msgText = message.getMessage();
        // Check if reference is single line or multiline type
        if (msgText != null && msgText.contains("<br>")) {
            sb.append("\n");
            for (String line : msgText.split("<br>")) {
                sb.append("  ").append(line).append("\n");
            }

            sb.append("end ref");

        } else {
            sb.append(" : ").append(msgText);
        }

        return sb.toString();
    }

    private String messageArrow(SequenceMessage message) {
        StringBuilder sb = new StringBuilder();

        String startPart = message.getStartPart();
        String endPart = message.getEndPart();

        if (message.getStartDecor().equals("circle")) sb.append("o");

        String startHead = message.getStartHead();
        switch (startPart) {
            case "full" -> {
                switch (startHead) {
                    case "block" -> sb.append("<");
                    case "open" -> sb.append("<<");
                    case "cross" -> sb.append("x");
                }
            }
            case "bottom" -> {
                if (startHead.equals("open")) {
                    sb.append("//");
                } else if (startHead.equals("block")) {
                    sb.append("/");
                }
            }
            case "top" -> {
                if (startHead.equals("open")) {
                    sb.append("\\\\");
                } else if (startHead.equals("block")) {
                    sb.append("\\");
                }
            }
        }

        sb.append(message.isDotted() ? "--" : "-");
        String color = message.getColor();
        if (color != null && !color.equals("black")) {
            sb.append("[").append(color).append("]");
        }

        String endHead = message.getEndHead();
        switch (endPart) {
            case "full" -> {
                switch (endHead) {
                    case "block" -> sb.append(">");
                    case "open" -> sb.append(">>");
                    case "cross" -> sb.append("x");
                }
            }
            case "bottom" -> {
                if (endHead.equals("open")) {
                    sb.append("//");
                } else if (endHead.equals("block")) {
                    sb.append("/");
                }
            }
            case "top" -> {
                if (endHead.equals("open")) {
                    sb.append("\\\\");
                } else if (endHead.equals("block")) {
                    sb.append("\\");
                }
            }
        }

        if (message.getEndDecor().equals("circle")) sb.append("o");

        return sb.toString();
    }

    private void writeAnchor() {
        for (SequenceAnchor anchor : model.anchors) {
            if (anchor.hasLine() && anchor.isModified()) {
                changeLine(anchor.getSourceLineStart(), anchor.getSourceLineEnd(), List.of(replaceAnchor(anchor)));
            }
        }
    }

    private String replaceAnchor(SequenceAnchor anchor) {
        String source = anchor.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);

        if (source != null && !source.isEmpty()) {
            int firstBrace = source.indexOf('{');
            int secondBrace = source.indexOf('}', firstBrace);
            int thirdBrace = source.indexOf('{', secondBrace);
            int fourthBrace = source.indexOf('}', thirdBrace);

            if (firstBrace >= 0 && fourthBrace >= 0) {
                String startMarker = source.substring(firstBrace, secondBrace + 1);
                String endMarker = source.substring(thirdBrace, fourthBrace + 1);
                String content = startMarker + " <-> " + endMarker + " : " + anchor.getLabel();
                return IndentatHelper.applyIndentation(content, indent);
            }
        }

        return IndentatHelper.applyIndentation("{start} <-> {end} : " + anchor.getLabel(), indent);
    }

    private void writeGroup() {
        for (SequenceGroup group : model.groups) {
            if (!group.isModified()) continue;

            // Group start
            changeLine(group.getSourceLineStart(), group.getSourceLineStart(), List.of(replaceGroupStart(group)));

            // Group else
            List<Integer> separatorLines = group.getSeparatorLineNumbers();
            List<String> separatorLabels = group.getSeparatorLabel();

            for (int i = 0; i < separatorLines.size(); i++) {
                int line = separatorLines.get(i);
                String label = separatorLabels.get(i);
                String sourceLine = sourceLines.get(line);

                changeLine(line, line, List.of(replaceGroupElse(label, sourceLine)));
            }
        }
    }

    private String replaceGroupStart(SequenceGroup group) {
        StringBuilder sb = new StringBuilder();

        String source = group.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);

        if (group.isGroup()) {
            sb.append("group");
            if (!group.getElementColor().equals("grey")) sb.append(group.getElementColor());
            if (!group.getBackColor().equals("none")) sb.append(" ").append(group.getBackColor());
            sb.append(" ").append(group.getLabel());

            if (group.getComment() != null && !group.getComment().isEmpty()) {
                sb.append(" [").append(group.getComment()).append("]");
            }

        } else {
            sb.append(group.getLabel());
            if (!group.getElementColor().equals("grey")) sb.append(group.getElementColor());
            if (!group.getBackColor().equals("none")) sb.append(" ").append(group.getBackColor());

            if (group.getComment() != null && !group.getComment().isEmpty()) {
                sb.append(" ").append(group.getComment());
            }
        }

        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private String replaceGroupElse(String label, String source) {
        StringBuilder sb = new StringBuilder("else");
        String indent = IndentatHelper.extractIndentation(source);

        if (label != null && !label.isEmpty()) {
            sb.append(" ").append(label);
        }

        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private void writePageDetails() {
        if (model.titleModified) {
            changeLine(model.titleLineStart, model.titleLineEnd,
                    replacePageDetails("title", model.title, model.titleLineStart, model.titleLineEnd));
        }

        if (model.headerModified) {
            changeLine(model.headerLineStart, model.headerLineEnd,
                    replacePageDetails("header", model.header, model.headerLineStart, model.headerLineEnd));
        }

        if (model.footerModified) {
            changeLine(model.footerLineStart, model.footerLineEnd,
                    replacePageDetails("footer", model.footer, model.footerLineStart, model.footerLineEnd));
        }

        if (model.mainframeModified) {
            String text = model.mainframe.replace("<br>", "\\n");
            changeLine(model.mainframeLineNumber, model.mainframeLineNumber,
                    List.of("mainframe " + text));
        }
    }

    private List<String> replacePageDetails(String keyword, String content, int startLine, int endLine) {
        List<String> lines = new ArrayList<>();
        String indent = IndentatHelper.extractIndentation(sourceLines.get(startLine));

        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            lines.add(IndentatHelper.applyIndentation(keyword, indent));

            String[] contentLines = content.split("<br>");
            for (String line : contentLines) {
                lines.add(IndentatHelper.applyIndentation(line, indent));
            }

            lines.add(IndentatHelper.applyIndentation("end " + keyword, indent));

        } else {
            String text = content.replace("<br>", "\\n");
            lines.add(IndentatHelper.applyIndentation(keyword + " " + text, indent));
        }

        return lines;
    }

    private void writeEnglobers() {
        for (SequenceEnglober englober : model.englobers) {
            if (!englober.isModified()) continue;

            changeLine(englober.getSourceLineStart(), englober.getSourceLineStart(), List.of(replaceEnglober(englober)));
        }
    }

    private String replaceEnglober(SequenceEnglober englober) {
        StringBuilder sb = new StringBuilder("box ");

        String source = englober.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);

        sb.append("\"").append(englober.getLabel()).append("\"");
        if (!englober.getColor().equals("#CCCCCC")) {
            sb.append(" ").append(englober.getColor());
        }

        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private void writeNote(SequenceMessage message, SequenceNote note) {
        int startLine = note.getSourceLineStart();
        int endLine = note.getSourceLineEnd();

        boolean isMultiline = startLine != endLine;

        if (isMultiline) {
            changeLine(startLine, endLine - 1, List.of(replaceMultiLineNote(message, note)));
        } else {
            changeLine(startLine, endLine, List.of(replaceSingleLineNote(message, note)));
        }
    }

    private String replaceMultiLineNote(SequenceMessage message, SequenceNote note) {
        StringBuilder sb = new StringBuilder();

        String source = note.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);

        String shape = note.getShape();
        switch (shape) {
            case "HEXAGONAL" -> sb.append("hnote ");
            case "BOX" -> sb.append("rnote ");
            case "NORMAL" -> sb.append("note ");
        }

        if (source.contains("across")) {
            sb.append("across");
        } else {
            String position = note.getPosition();
            boolean overSeveral = false;
            switch (position) {
                case "LEFT" -> sb.append("left ");
                case "RIGHT" -> sb.append("right ");
                case "OVER" -> sb.append("over ");
                case "OVER_SEVERAL" -> {
                    overSeveral = true;
                    sb.append("over ");
                }
            }

            if (!message.getType().equals("edge:note")) {
                sb.append(note.getLabel().replace("<br>", "\n"));
                return IndentatHelper.applyIndentation(sb.toString(), indent);
            }

            if (position.equals("LEFT") || position.equals("RIGHT")) {
                sb.append("of ");
            }

            sb.append(ReconstructorHelper.getParticipant(message.getFrom()));
            if (overSeveral) {
                sb.append(", ").append(ReconstructorHelper.getParticipant(message.getTo()));
            }

            if (!note.getBackground().equals("#FFFFE0")) {
                sb.append(" ").append(note.getBackground());
            }

            sb.append("\n");
        }

        sb.append(note.getLabel().replace("<br>", "\n"));
        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private String replaceSingleLineNote(SequenceMessage message, SequenceNote note) {
        StringBuilder sb = new StringBuilder();

        String source = note.getRawSourceText();
        String indent = IndentatHelper.extractIndentation(source);

        String shape = note.getShape();
        switch (shape) {
            case "HEXAGONAL" -> sb.append("hnote ");
            case "BOX" -> sb.append("rnote ");
            case "NORMAL" -> sb.append("note ");
        }

        if (source.contains("across")) {
            sb.append("across: ");
        } else {
            String position = note.getPosition();
            boolean overSeveral = false;
            switch (position) {
                case "LEFT" -> sb.append("left ");
                case "RIGHT" -> sb.append("right ");
                case "OVER" -> sb.append("over ");
                case "OVER_SEVERAL" -> {
                    overSeveral = true;
                    sb.append("over ");
                }
            }

            if (!message.getType().equals("edge:note")) {
                sb.append(": ").append(note.getLabel());
                return IndentatHelper.applyIndentation(sb.toString(), indent);
            }

            if (position.equals("LEFT") || position.equals("RIGHT")) {
                sb.append("of ");
            }

            sb.append(ReconstructorHelper.getParticipant(message.getFrom()));
            if (overSeveral) {
                sb.append(", ").append(ReconstructorHelper.getParticipant(message.getTo()));
            }

            if (!note.getBackground().equals("#FFFFE0")) {
                sb.append(" ").append(note.getBackground());
            }

            sb.append(": ");
        }

        sb.append(note.getLabel().replace("<br>", "\\n"));
        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private void applyReplacements() {
        List<Integer> sortedLines = new ArrayList<>(newLines.keySet());
        sortedLines.sort(Collections.reverseOrder());

        for (int startLine : sortedLines) {
            NewLine replacement = newLines.get(startLine);
            System.err.println("Applying replacement: " + replacement);

            // Remove old lines
            for (int i = replacement.endLine(); i >= replacement.startLine(); i--) {
                if (i < sourceLines.size()) {
                    sourceLines.remove(i);
                }
            }

            // Insert new lines
            sourceLines.addAll(replacement.startLine(), replacement.newLines());
        }
    }

    private void saveAtomic() throws IOException {
        File tempFile = new File(source.getParent(), source.getName() + ".tmp");

        Files.write(tempFile.toPath(), sourceLines, StandardCharsets.UTF_8);
        Files.move(
                tempFile.toPath(),
                source.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }
}
