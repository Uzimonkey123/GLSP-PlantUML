package com.GLSPPlantUML.reconstructor;

import com.GLSPPlantUML.model.SequenceModel;
import com.GLSPPlantUML.model.SequenceParts.SequenceLifeEvent;
import com.GLSPPlantUML.model.SequenceParts.SequenceMessage;
import com.GLSPPlantUML.model.SequenceParts.SequenceNode;
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
            }

            for (SequenceLifeEvent event : node.getLifeEvents()) {
                if (event.hasLine()) {
                    // TODO: Life events of this node
                }
            }
        }
    }

    private String replaceParticipant(SequenceNode node) {
        StringBuilder sb = new StringBuilder();

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

        sb.append(ReconstructorHelper.getParticipant(node));

        return IndentatHelper.applyIndentation(sb.toString(), indent);
    }

    private void writeMessage() {
        for (SequenceMessage message : model.messages) {
            if (message.hasLine() && message.isModified()) {
                changeLine(message.getSourceLineStart(), message.getSourceLineEnd(), List.of(replaceMessage(message)));
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
        }

        if (lineMap.getLineInfo(message.getSourceLineStart()).type == LineMapper.LineType.RETURN) {
            return IndentatHelper.applyIndentation("return " + message.getMessage(), indent);
        }

        return IndentatHelper.applyIndentation(regularMessage(message), indent);
    }

    private String regularMessage(SequenceMessage message) {
        StringBuilder sb = new StringBuilder();

        if (message.isParallel()) sb.append("& ");

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

    private void applyReplacements() {
        List<Integer> sortedLines = new ArrayList<>(newLines.keySet());
        sortedLines.sort(Collections.reverseOrder());

        for (int startLine : sortedLines) {
            NewLine replacement = newLines.get(startLine);

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
