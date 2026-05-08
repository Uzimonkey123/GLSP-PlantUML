/*
 * File: SequenceDiagramTestBase.java
 * Author: Norman Babiak
 * Description: Base testing setup file for the sequence diagram
 * Date: 29.4.2026
 */

// Test file skeleton generated with assistance from Claude Opus 4.5.

package com.diagrams.SequenceDiagram;

import com.diagrams.SequenceDiagram.model.SequenceModel;
import com.diagrams.SequenceDiagram.reconstructor.LineFinder;
import com.diagrams.SequenceDiagram.reconstructor.LineMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class SequenceDiagramTestBase {
    private static final String RESOURCE_PREFIX = "SequenceDiagrams/";

    @TempDir
    protected Path tempDir;

    protected SequenceModel model;
    protected LineMapper lineMapper;
    protected LineFinder lineFinder;
    protected Map<Object, Integer> elementToLineMap;

    @BeforeEach
    void setupBase() {
        model = new SequenceModel();
        elementToLineMap = new HashMap<>();
    }

    protected String loadResource(String resourcePath) {
        String fullPath = RESOURCE_PREFIX + resourcePath;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fullPath)) {
            Objects.requireNonNull(is, "Resource not found: " + fullPath);

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + fullPath, e);
        }
    }

    protected void createMapper(String resourcePath) {
        String source = loadResource(resourcePath);
        lineMapper = new LineMapper(source);
        model.setMapper(lineMapper);
    }

    protected void createFinder(String resourcePath) {
        createMapper(resourcePath);
        lineFinder = new LineFinder(lineMapper, elementToLineMap);
    }

    protected Path createTempPumlFile(String content) throws IOException {
        Path file = tempDir.resolve("test.puml");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        return file;
    }

    protected String puml(String... lines) {
        StringBuilder sb = new StringBuilder("@startuml\n");

        for (String line : lines) {
            sb.append(line).append("\n");
        }
        sb.append("@enduml\n");

        return sb.toString();
    }

    protected String normalizeContent(String content) {
        if (content == null) return null;

        return content
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .stripTrailing();
    }
}