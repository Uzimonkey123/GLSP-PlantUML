/*
 * File: SequenceModelParser.java
 * Author: Norman Babiak
 * Description: Parser to internal model from PlantUML public API
 * Date: 3.4.2026
 */

package com.diagrams.SequenceDiagram.parser;

import com.GLSPPlantUML.parser.PlantUMLParser;
import com.GLSPPlantUML.utils.ErrorMessage;
import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.model.SequenceParts.*;
import com.diagrams.SequenceDiagram.parser.handlers.SequenceEventHandlerContext;
import com.diagrams.SequenceDiagram.parser.handlers.SequenceEventHandlerRegistry;
import com.diagrams.SequenceDiagram.reconstructor.LineFinder;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import com.diagrams.SequenceDiagram.reconstructor.SourceElement;
import com.diagrams.SequenceDiagram.state.SequenceModelState;
import com.google.inject.Inject;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.sequencediagram.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SequenceModelParser implements PlantUMLParser<SequenceModel> {
    private SequenceDiagram sequenceDiagram;

    @Inject
    SequenceModelState modelState;

    SequenceModel model;

    private LineFinder lineFinder;
    private LineMapper lineMapper;
    private final Map<Object, Integer> eventToLineNumber = new HashMap<>();

    private SequenceEventHandlerContext handlerContext;
    private SequenceEventHandlerRegistry handlerRegistry;

    @Inject
    public SequenceModelParser() {}

    public SequenceModel parse(File file) throws IOException {
        // Read and store original
        String originalText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        this.model = new SequenceModel();

        // Prepare text for PlantUML
        String text = originalText;
        if (!text.contains("@startuml")) {
            text = "@startuml\n" + originalText + "\n@enduml";
        }

        // Create line mapper and utility
        lineMapper = new LineMapper(originalText);
        lineFinder = new LineFinder(lineMapper, eventToLineNumber);

        // Parse with PlantUML
        SourceStringReader reader = new SourceStringReader(text);
        List<BlockUml> blocks = reader.getBlocks();

        for (BlockUml block : blocks) {
            Diagram d = block.getDiagram();
            if (d instanceof PSystemError) {
                String error = String.join("<br>", ((PSystemError) d).getPureAsciiFormatted());
                modelState.setError(new ErrorMessage(error));
                return model;
            }

            if (d instanceof SequenceDiagram sd) {
                this.sequenceDiagram = sd;
                model.showFoot = sequenceDiagram.isShowFootbox();

                handlerContext = new SequenceEventHandlerContext(model, lineMapper, lineFinder, sd);
                handlerRegistry = new SequenceEventHandlerRegistry(handlerContext);

                handleHeader();
                handleFooter();
                handleTitle();
                MainframeHandler();

                lineFinder.resetSearch();
                processParticipants(sd);

                lineFinder.resetSearch();
                processEvents(sd);

                closeLifeEvents();
            }
        }

        model.setMapper(lineMapper);
        return model;
    }

    /**
     * Processes all participants from the diagram, resolving source lines and englobers.
     */
    private void processParticipants(SequenceDiagram sd) {
        for (Participant participant : sd.participants()) {
            String name = String.join("<br>", participant.getDisplay(false));

            lineFinder.resetSearch();
            int participantLine = lineFinder.findParticipantLine(name, participant);

            ParticipantHandler(participant, participantLine);
        }
    }

    /**
     * Delegates each event to the handler registry for processing.
     */
    private void processEvents(SequenceDiagram sd) {
        for (Event event : sd.events()) {
            handlerRegistry.handle(event);
        }
    }

    private void ParticipantHandler(Participant participant, int lineNum) {
        String name = String.join("<br>", participant.getDisplay(false));
        String type = participant.getType().toString();
        int order = participant.getOrder();
        HColor background = participant.getColors().getColor(ColorType.BACK);
        SequenceNode node = null;
        String id = "par-" + model.participants.size();

        if(!hasParticipant(name)) {
            node = new SequenceNode(id, name, type, order, background, false);

            // Store source info
            if (lineNum >= 0) { // If lineNum at this point is > 0, explicit declaration
                node.setSourceLines(lineNum, lineNum);
                LineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);
                if (lineMapper.getLineInfo(lineNum) != null) {
                    node.setRawSourceText(info.originalText);
                }

            } else { // Implicit participant declaration
                int createLine = lineFinder.findCreateLine(name, participant);
                addMapperInfo(node, createLine);
            }

            EngloberHandler(node, sequenceDiagram.getEnglober(participant));
            addParticipants(model.participants, node);
        }

        if (node != null && participant.getStereotype() != null) {
            node.setStereotype(true);
            node.setStereotypeChar(participant.getStereotype().getCharacter());
            if (participant.getStereotype().getHtmlColor() != null) {
                node.setCharColor(participant.getStereotype().getHtmlColor().asString());
            }
        }
    }

    private void EngloberHandler(SequenceNode node, ParticipantEnglober englober) {
        if (englober == null) return;

        for (ParticipantEnglober part : englober.getGenealogy()) {
            String title = String.join("<br>", part.getTitle());
            String id = "englober-" + title;
            String parentId = part.getParent() == null ? null : "englober-" + part.getParent().getTitle().toString().hashCode();
            String color = part.getBoxColor() != null ? part.getBoxColor().asString() : "#CCCCCC";
            int level = part.getGenealogy().size() - 1; // Level indicating depth of the box to set offset

            boolean alreadyAdded = model.englobers.stream().anyMatch(e -> e.getId().equals(id));
            if (!alreadyAdded) {
                SequenceEnglober newEnglober = new SequenceEnglober(id, title, parentId, color, level);
                int lineNum = lineFinder.findEngloberLine(title, englober);
                addMapperInfo(newEnglober, lineNum);

                model.englobers.add(newEnglober);
            }

            // Assign the englober ID to the node for further search in factory
            node.addEngloberId(id);
        }
    }

    /**
     * Closes any remaining open life events at the end of parsing.
     */
    private void closeLifeEvents() {
        int lastMsg = model.messages.isEmpty() ? 0 : model.messages.size() - 1;

        handlerContext.getAllActivationStacks().forEach((participant, starts) -> {
            Stack<HColor> colors = handlerContext.getAllActivationColorStacks().get(participant);

            SequenceNode currentNode = null;

            // Search for node in the participant list
            for (SequenceNode node : model.participants) {
                if (node.getName().equals(participant)) {
                    currentNode = node;
                    break;
                }
            }
            if (currentNode == null) return;

            while (!starts.isEmpty()) {
                int startIndex = starts.pop();
                HColor color = colors.pop();

                SequenceLifeEvent le = new SequenceLifeEvent(startIndex, lastMsg, color);
                le.setLevel(starts.size());
                currentNode.addLifeEvent(le);
            }
        });
    }

    private void addMapperInfo(SourceElement element, int lineNum) {
        if (lineNum >= 0) {
            element.setSourceLines(lineNum, lineNum);
            LineMapper.LineInfo info = lineMapper.getLineInfo(lineNum);
            if (info != null) {
                element.setRawSourceText(info.originalText);
            }
        }
    }

    private boolean hasParticipant(String name) {
        for (SequenceNode node : model.participants) {
            if (node.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void addParticipants(List<SequenceNode> participants, SequenceNode node) {
        for (int i = 0; i < participants.size(); i++) {
            int existingOrder = participants.get(i).getOrder();
            if (node.getOrder() < existingOrder) {
                participants.add(i, node);
                return;
            }
        }

        // Add to last position
        participants.add(node);
    }

    private void handleTitle() {
        model.title = "";

        if (sequenceDiagram.getTitle() != null
                && sequenceDiagram.getTitle().getDisplay() != null
                && sequenceDiagram.getTitle().getDisplay().size() > 0) {
            model.title = String.join("<br>", sequenceDiagram.getTitle().getDisplay());

            int startLine = findLineByType(LineMapper.LineType.TITLE);
            int endLine = findLineByType(LineMapper.LineType.END_TITLE);

            if (startLine >= 0) {
                model.titleLineStart = startLine;
                model.titleLineEnd = endLine >= 0 ? endLine : startLine;
            }
        }
    }

    private void handleHeader() {
        model.header = "";

        if (sequenceDiagram.getHeader() != null
                && sequenceDiagram.getHeader().getDisplay() != null
                && sequenceDiagram.getHeader().getDisplay().size() > 0) {
            model.header = String.join("<br>", sequenceDiagram.getHeader().getDisplay());

            int startLine = findLineByType(LineMapper.LineType.HEADER);
            int endLine = findLineByType(LineMapper.LineType.END_HEADER);

            if (startLine >= 0) {
                model.headerLineStart = startLine;
                model.headerLineEnd = endLine >= 0 ? endLine : startLine;
            }
        }
    }

    private void handleFooter() {
        model.footer = "";

        if (sequenceDiagram.getFooter() != null
                && sequenceDiagram.getFooter().getDisplay() != null
                && sequenceDiagram.getFooter().getDisplay().size() > 0) {
            model.footer = String.join("<br>", sequenceDiagram.getFooter().getDisplay());

            int startLine = findLineByType(LineMapper.LineType.FOOTER);
            int endLine = findLineByType(LineMapper.LineType.END_FOOTER);

            if (startLine >= 0) {
                model.footerLineStart = startLine;
                model.footerLineEnd = endLine >= 0 ? endLine : startLine;
            }
        }
    }

    private void MainframeHandler() {
        model.mainframe = "";

        if (sequenceDiagram.getMainFrame() != null) {
            model.isMainframe = true;
            model.mainframe = String.join("<br>", sequenceDiagram.getMainFrame());

            model.mainframeLineNumber = findLineByType(LineMapper.LineType.MAINFRAME);
        }
    }

    private int findLineByType(LineMapper.LineType type) {
        List<LineMapper.LineInfo> lines = lineMapper.getLineInfos();

        for (LineMapper.LineInfo info : lines) {
            if (info.type == type) return info.lineNumber;
        }

        return -1;
    }
}
