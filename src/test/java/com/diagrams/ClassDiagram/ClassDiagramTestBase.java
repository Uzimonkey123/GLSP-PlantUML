package com.diagrams.ClassDiagram;

import com.diagrams.ClassDiagram.model.ClassModel;
import com.diagrams.ClassDiagram.reconstructor.ClassLineFinder;
import com.diagrams.ClassDiagram.reconstructor.ClassLineMapper;
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

public abstract class ClassDiagramTestBase {
    private static final String RESOURCE_PREFIX = "ClassDiagrams/";

    @TempDir
    protected Path tempDir;

    protected ClassModel model;
    protected ClassLineMapper lineMapper;
    protected ClassLineFinder lineFinder;
    protected Map<Object, Integer> elementToLineMap;

    @BeforeEach
    void setupBase() {
        model = new ClassModel();
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
        lineMapper = new ClassLineMapper(source, model);

    }

    protected void createFinder(String resourcePath) {
        createMapper(resourcePath);
        lineFinder = new ClassLineFinder(lineMapper, elementToLineMap);

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

    protected void assertLineType(int lineIndex, ClassLineMapper.LineType expectedType) {
        ClassLineMapper.LineInfo info = lineMapper.getLineInfo(lineIndex);
        if (info == null) {
            throw new AssertionError("No line info at index " + lineIndex);
        }

        if (info.type != expectedType) {
            throw new AssertionError(String.format(
                    "Line %d: expected type %s but was %s (text: '%s')",
                    lineIndex, expectedType, info.type, info.trimmedText
            ));
        }
    }
}