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
                if ((message.getFrom().equals(node) || message.getTo().equals(node)) &&
                        message.hasLine()) {
                    // TODO: Message that has this node
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

        ReconstructorHelper.appendQuotedName(sb, node.getName());
        String alias = ReconstructorHelper.extractAlias(source);
        if (alias != null && !alias.isEmpty()) {
            sb.append(" as ").append(alias);
        }

        return IndentatHelper.applyIndentation(sb.toString(), indent);
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
