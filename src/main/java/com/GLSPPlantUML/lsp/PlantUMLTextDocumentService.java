/*
 * File: PlantUMLTextDocumentService.java
 * Author: Norman Babiak
 * Description: Handles text document events, publishes diagnostics and provides completions
 * Date: 10.4.2026
 */

package com.GLSPPlantUML.lsp;

import com.GLSPPlantUML.launcher.RuleLoader;
import com.GLSPPlantUML.utils.ErrorRecord;
import com.GLSPPlantUML.validators.CompositeValidator;
import com.GLSPPlantUML.validators.ErrorValidator;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantUMLTextDocumentService implements TextDocumentService {
    private final PlantUMLLanguageServer server;
    private final Map<String, String> documents = new ConcurrentHashMap<>();

    private CompositeValidator compositeValidator;
    private boolean initialized = false;

    private static final Pattern DIAGRAM_TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|abstract|annotation|dataclass|entity|exception"
                    + "|metaclass|protocol|record|stereotype|struct)\\b"
                    + "|\\b(package|namespace|rectangle|node|cloud|database"
                    + "|frame|storage|component|folder)\\b"
                    + "|\\{\\s*([+\\-#~])"
                    + "|--|>|\\.\\.\\|>"
                    + "|--\\*|--o"
                    + "|<\\|--|<\\|\\.\\."
                    + "|\\.\\.[>]|--[>]"
                    + "|o--|\\*--"
                    + "|\\bdiamond\\b|\\bcircle\\b|<>"
                    + "|<<\\w+>>"
                    + "|\\{field}|\\{method}"
                    + "|\\{static}|\\{abstract}"
                    + "|\\b[A-Za-z_]\\w*\\s*:\\s*\\S"
                    + "|^\\s*-{2,}\\s*$|^\\s*[.]{2,}\\s*$|^\\s*={2,}\\s*$|^\\s*_{2,}\\s*$"
                    + "|\\b(hide|show)\\s+(empty|members|fields|methods|circle|stereotype)"
                    + "|\\bleft\\s+to\\s+right\\s+direction\\b"
    );

    public PlantUMLTextDocumentService(PlantUMLLanguageServer server) {
        this.server = server;
    }

    private synchronized CompositeValidator getValidator() {
        if (!initialized) {
            initialized = true;
            ErrorValidator errorValidator = new ErrorValidator();
            RuleLoader ruleLoader = new RuleLoader();
            try {
                ruleLoader.loadRules();

            } catch (Exception e) {
                System.err.println("Failed to load validation rules: " + e.getMessage());
            }

            compositeValidator = new CompositeValidator(errorValidator, ruleLoader);
        }

        return compositeValidator;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        documents.put(uri, text);
        validateAndPublish(uri, text);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getContentChanges().getFirst().getText();
        documents.put(uri, text);
        validateAndPublish(uri, text);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        server.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getText();

        if (text != null) {
            documents.put(uri, text);
            validateAndPublish(uri, text);
        }
    }

    /**
     * Method to run composite validator before transforming ErrorRecord type into LSP Diagnostic
     */
    private void validateAndPublish(String uri, String text) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        String diagramType = detectDiagramType(text);
        ErrorRecord result = getValidator().validate(text, diagramType);

        if (result.hasError() && result.errorMsg() != null) {
            int line = Math.max(0, result.lineNumber());
            int startCol = Math.max(0, result.columnStart());
            int endCol = result.columnEnd();

            if (endCol <= startCol) {
                String[] lines = text.split("\n");
                if (line < lines.length) {
                    endCol = lines[line].length();

                } else {
                    endCol = startCol + 1;
                }
            }

            Diagnostic diagnostic = new Diagnostic(
                    new Range(new Position(line, startCol), new Position(line, endCol)),
                    cleanErrorMessage(result.errorMsg()),
                    DiagnosticSeverity.Error,
                    "PlantUML"
            );

            diagnostics.add(diagnostic);
        }

        server.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    private String detectDiagramType(String text) {
        if (DIAGRAM_TYPE_PATTERN.matcher(text).find()) {
            return "class-diagram";
        }

        return "sequence-diagram";
    }

    private String cleanErrorMessage(String msg) {
        return msg.replaceAll("<br>", "\n").replaceAll("<[^>]*>", "");
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {

        String uri = params.getTextDocument().getUri();
        String text = documents.getOrDefault(uri, "");
        String diagramType = detectDiagramType(text);
        int line = params.getPosition().getLine();

        List<CompletionItem> items = new ArrayList<>();

        String[] lines = text.split("\n", -1);
        String currentLine = line < lines.length ? lines[line] : "";
        String trimmed = currentLine.trim();

        if (diagramType.equals("class-diagram")) {
            addClassDiagramCompletions(items, trimmed, text);

        } else {
            addSequenceDiagramCompletions(items, trimmed, text);
        }

        addCommonCompletions(items, trimmed);

        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    private void addClassDiagramCompletions(List<CompletionItem> items, String currentLine, String fullText) {
        // TODO: ADD MORE
        addItem(items, "--|>", "Inheritance (extends)", CompletionItemKind.Operator);
        addItem(items, "..|>", "Implementation (implements)", CompletionItemKind.Operator);
        addItem(items, "-->", "Association", CompletionItemKind.Operator);
        addItem(items, "..>", "Dependency", CompletionItemKind.Operator);
        addItem(items, "--*", "Composition", CompletionItemKind.Operator);
        addItem(items, "--o", "Aggregation", CompletionItemKind.Operator);

        addItem(items, "class ", "Class declaration", CompletionItemKind.Keyword);
        addItem(items, "interface ", "Interface declaration", CompletionItemKind.Keyword);
        addItem(items, "enum ", "Enum declaration", CompletionItemKind.Keyword);
        addItem(items, "abstract class ", "Abstract class declaration", CompletionItemKind.Keyword);
        addItem(items, "package ", "Package grouping", CompletionItemKind.Keyword);
        addItem(items, "namespace ", "Namespace grouping", CompletionItemKind.Keyword);

        addItem(items, "+", "Public", CompletionItemKind.Keyword);
        addItem(items, "-", "Private", CompletionItemKind.Keyword);
        addItem(items, "#", "Protected", CompletionItemKind.Keyword);
        addItem(items, "~", "Package-private", CompletionItemKind.Keyword);

        addExistingEntityNames(items, fullText,
                Pattern.compile("(?:class|interface|enum|abstract\\s+class)\\s+(\\w+)"));
    }

    private void addSequenceDiagramCompletions(List<CompletionItem> items, String currentLine, String fullText) {
        // TODO: ADD MORE
        addItem(items, "->", "Synchronous message", CompletionItemKind.Operator);
        addItem(items, "-->", "Return message", CompletionItemKind.Operator);
        addItem(items, "->>", "Async message", CompletionItemKind.Operator);
        addItem(items, "-\\\\", "Lost message", CompletionItemKind.Operator);

        addItem(items, "participant ", "Participant declaration", CompletionItemKind.Keyword);
        addItem(items, "actor ", "Actor declaration", CompletionItemKind.Keyword);
        addItem(items, "boundary ", "Boundary declaration", CompletionItemKind.Keyword);
        addItem(items, "control ", "Control declaration", CompletionItemKind.Keyword);
        addItem(items, "entity ", "Entity declaration", CompletionItemKind.Keyword);
        addItem(items, "database ", "Database declaration", CompletionItemKind.Keyword);
        addItem(items, "collections ", "Collections declaration", CompletionItemKind.Keyword);
        addItem(items, "queue ", "Queue declaration", CompletionItemKind.Keyword);

        addItem(items, "alt ", "Alternative (if)", CompletionItemKind.Snippet);
        addItem(items, "else ", "Else branch", CompletionItemKind.Snippet);
        addItem(items, "loop ", "Loop block", CompletionItemKind.Snippet);
        addItem(items, "opt ", "Optional block", CompletionItemKind.Snippet);
        addItem(items, "par ", "Parallel block", CompletionItemKind.Snippet);
        addItem(items, "break ", "Break block", CompletionItemKind.Snippet);
        addItem(items, "critical ", "Critical block", CompletionItemKind.Snippet);
        addItem(items, "group ", "Generic group", CompletionItemKind.Snippet);
        addItem(items, "end", "End block", CompletionItemKind.Keyword);

        addItem(items, "activate ", "Activate lifeline", CompletionItemKind.Keyword);
        addItem(items, "deactivate ", "Deactivate lifeline", CompletionItemKind.Keyword);
        addItem(items, "destroy ", "Destroy lifeline", CompletionItemKind.Keyword);
        addItem(items, "newpage", "New page", CompletionItemKind.Keyword);

        addItem(items, "note left: ", "Note left", CompletionItemKind.Snippet);
        addItem(items, "note right: ", "Note right", CompletionItemKind.Snippet);
        addItem(items, "note over ", "Note over participant", CompletionItemKind.Snippet);

        addExistingEntityNames(items, fullText,
                Pattern.compile("(?:participant|actor|boundary|control|entity|database|collections|queue)\\s+(\\w+)"));
    }

    private void addCommonCompletions(List<CompletionItem> items, String currentLine) {
        addItem(items, "@startuml", "Start UML block", CompletionItemKind.Keyword);
        addItem(items, "@enduml", "End UML block", CompletionItemKind.Keyword);
        addItem(items, "title ", "Diagram title", CompletionItemKind.Keyword);
        addItem(items, "header ", "Header text", CompletionItemKind.Keyword);
        addItem(items, "footer ", "Footer text", CompletionItemKind.Keyword);
        addItem(items, "hide ", "Hide element", CompletionItemKind.Keyword);
        addItem(items, "show ", "Show element", CompletionItemKind.Keyword);
    }

    private void addExistingEntityNames(List<CompletionItem> items, String text, Pattern pattern) {
        Set<String> seen = new HashSet<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String name = matcher.group(1);
            if (seen.add(name)) {
                addItem(items, name, "Existing entity: " + name, CompletionItemKind.Reference);
            }
        }
    }

    private void addItem(List<CompletionItem> items, String label, String detail, CompletionItemKind kind) {
        CompletionItem item = new CompletionItem(label);
        item.setDetail(detail);
        item.setKind(kind);
        items.add(item);
    }
}