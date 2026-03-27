import * as vscode from 'vscode';
import parseDiagramType from "./diagramTypeParser";

export interface ValidationResult {
    hasError: boolean;
    errorMsg?: string;
    lineNumber?: number;
    columnStart?: number;
    columnEnd?: number;
}

export class SyntaxValidator {
    private diagnosticCollection: vscode.DiagnosticCollection;
    private timeouts: Map<string, NodeJS.Timeout> = new Map(); // Map timeout/file
    private serverUrl: string = 'http://localhost:5008/validate';

    constructor(diagnosticCollection: vscode.DiagnosticCollection) {
        this.diagnosticCollection = diagnosticCollection;
    }

    public validate(document: vscode.TextDocument): void {
        if (document.languageId !== 'plantuml') return;

        const key = document.uri.toString(); // Key for file
        clearTimeout(this.timeouts.get(key));

        // Wait 500ms after last typing to handle error
        this.timeouts.set(key, setTimeout(async () => {
            try {
                const requestBody = {
                    context: document.getText(),
                    diagramType: await parseDiagramType(document)
                };

                const response = await fetch(this.serverUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestBody)
                });

                if (!response.ok) {
                    console.error('Server returned error status:', response.status);
                    this.diagnosticCollection.set(document.uri, []);
                    return;
                }

                const result: ValidationResult = await response.json();
                this.updateDiagnostics(document, result);

            } catch (error) {
                console.error('Validation failed:', error);
                this.diagnosticCollection.set(document.uri, []);
            }

            // Remove timer if done with parsing and showing error
            this.timeouts.delete(key);
        }, 500));
    }

    private updateDiagnostics(document: vscode.TextDocument, result: ValidationResult): void {
        const diagnostics: vscode.Diagnostic[] = [];

        if (result.hasError && result.errorMsg) {
            const line = Math.max(0, (result.lineNumber ?? 0));
            const startCol = result.columnStart ?? 0;
            const endCol = result.columnEnd ?? startCol + 1;

            try {
                const lineText = document.lineAt(line).text;

                // Setup where the highlight is
                const range = new vscode.Range(
                    line,
                    startCol,
                    line,
                    endCol > startCol ? endCol : lineText.length
                );

                // Severity setting for error (red underline)
                const diagnostic = new vscode.Diagnostic(
                    range,
                    result.errorMsg,
                    vscode.DiagnosticSeverity.Error
                );
                diagnostic.source = 'PlantUML';
                diagnostics.push(diagnostic);

            } catch (error) {
                console.error('Error creating diagnostic:', error);
            }
        }

        // Show errors
        this.diagnosticCollection.set(document.uri, diagnostics);
    }

    public dispose(): void {
        this.timeouts.forEach(timeout => clearTimeout(timeout));
        this.timeouts.clear();
        this.diagnosticCollection.clear();
    }
}