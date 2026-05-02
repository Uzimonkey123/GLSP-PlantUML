/*
 * File: editor-provider.ts
 * Author: Norman Babiak
 * Description: Custom editor provider for .puml files in webview
 * Date: 2.5.2026
 */

import 'reflect-metadata';

import * as vscode from 'vscode';
import {
    GlspEditorProvider,
    GlspVscodeConnector,
} from '@eclipse-glsp/vscode-integration';
import parseDiagramType from "./diagramTypeParser";

/**
 * Custom editor provider that opens .puml files in a GLSP webview. Detects the diagram type from the file content and
 * injects it into the webview so the correct DI container is initialized on the client side.
 */
export default class PumlEditorProvider extends GlspEditorProvider {
    diagramType = 'sequence-diagram';
    // Tracks open webview panels by normalized file path for refresh support
    private readonly panels = new Map<string, vscode.WebviewPanel>();

    constructor(
        protected readonly extensionContext: vscode.ExtensionContext,
        protected override readonly glspVscodeConnector: GlspVscodeConnector
    ) {
        super(glspVscodeConnector);
    }

    // Normalizes a URI to a lowercase file path for case-insensitive panel lookup
    private normalizeUri(uri: vscode.Uri): string {
        return uri.fsPath.toLowerCase();
    }

    /**
     * Parses the diagram type from the document before delegating to the base GLSP editor setup. Registers the panel for
     * later refresh lookups
     */
    async resolveCustomEditor(
        document: vscode.CustomDocument,
        webviewPanel: vscode.WebviewPanel,
        token: vscode.CancellationToken
    ): Promise<void> {
        this.diagramType = await parseDiagramType(document);

        const key = this.normalizeUri(document.uri);
        this.panels.set(key, webviewPanel);

        // Clean up the panel reference when the editor tab is closed
        webviewPanel.onDidDispose(() => {
            this.panels.delete(key);
        });

        return super.resolveCustomEditor(document, webviewPanel, token);
    }

    /**
     * Reveals an already-open diagram panel for the given URI. Returns true if the panel was found, false if no editor
     * is open for that file
     */
    public refreshDiagram(uri: vscode.Uri): boolean {
        const key = this.normalizeUri(uri);
        const panel = this.panels.get(key);

        if (panel) {
            panel.reveal(undefined, false);

            return true;
        }

        return false;
    }

    /**
     * Configures the webview HTML content. Injects the diagram type as a global variable, sets up CSS, and loads the
     * bundled webview script
     */
    setUpWebview(
        _document: vscode.CustomDocument,
        webviewPanel: vscode.WebviewPanel,
        _token: vscode.CancellationToken,
        clientId: string
    ): void {
        const webview = webviewPanel.webview;
        const extensionUri = this.extensionContext.extensionUri;
        const webviewScriptSourceUri = webview.asWebviewUri(vscode.Uri.joinPath(extensionUri, 'extension/dist', 'webview.js'));

        webviewPanel.webview.options = {
            enableScripts: true
        };

        webviewPanel.webview.html = `
            <!DOCTYPE html>
            <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta 
                        name="viewport" 
                        content="width=device-width, height=device-height, initial-scale=1.0"
                    />
                    <style>
                        html, body {
                            width: 100vw;
                            height: 100vh;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                        }
                        body {
                            display: flex;
                            flex-direction: column;
                        }
                        #${clientId}_container {
                            flex: 1;
                            position: relative;
                            }
                        #${clientId}_container > div {
                            position: absolute;
                            top: 0; left: 0; right: 0; bottom: 0;
                            width: 100%;
                            height: 100%;
                        }
                        #${clientId}_container svg {
                            width: 100%;
                            height: 100%;
                        }
                    </style>
                </head>
                <script>
                    window.DIAGRAM_TYPE = '${this.diagramType}';
                </script>
                <body>
                    <div id="${clientId}_container"></div>
                    <script src="${webviewScriptSourceUri}"></script>
                </body>
            </html>`;
    }
}
