import 'reflect-metadata';

import * as vscode from 'vscode';
import {
  GlspEditorProvider,
  GlspVscodeConnector,
} from '@eclipse-glsp/vscode-integration';
import parseDiagramType from "./diagramTypeParser";

export default class PumlEditorProvider extends GlspEditorProvider {
  diagramType = 'sequence-diagram';

  constructor(
    protected readonly extensionContext: vscode.ExtensionContext,
    protected override readonly glspVscodeConnector: GlspVscodeConnector
  ) {
    super(glspVscodeConnector);
  }

  async resolveCustomEditor(
      document: vscode.CustomDocument,
      webviewPanel: vscode.WebviewPanel,
      token: vscode.CancellationToken
  ): Promise<void> {
      this.diagramType = await parseDiagramType(document);

      return super.resolveCustomEditor(document, webviewPanel, token);
  }

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