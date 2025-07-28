import 'reflect-metadata';
import * as vscode from 'vscode';
import {
  GlspVscodeConnector,
  SocketGlspVscodeServer,
  configureDefaultCommands,
} from '@eclipse-glsp/vscode-integration';
import PumlEditorProvider from './editor-provider';
import { RequestExportSvgAction } from 'sprotty';
import {WebSocketGlspVscodeServer} from "@eclipse-glsp/vscode-integration/browser";

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  // Connecting to the Java server that is already running
  const server = new SocketGlspVscodeServer({
    clientId: "glsp.puml",
    clientName: "PlantUML",
    connectionOptions: {
      port: 5007,
      path: "plantuml",
    }
  });

  context.subscriptions.push(server);
  server.start();

  const connector = new GlspVscodeConnector({ server, logging: true });

  // Registering custom editor provider
  const provider = new PumlEditorProvider(context, connector);
  const registration = vscode.window.registerCustomEditorProvider(
    'plantuml.glspDiagram',
    provider,
    {
      webviewOptions: { retainContextWhenHidden: false },
      supportsMultipleEditorsPerDocument: false
    }
  );
  context.subscriptions.push(connector, registration);

  // Setup for default commands from glsp
  configureDefaultCommands({
    extensionContext: context,
    connector,
    diagramPrefix: 'puml'
  });

  // Command to open .puml in editor
  context.subscriptions.push(
    vscode.commands.registerCommand('plantuml.glspDiagram', (uri: vscode.Uri) =>
      vscode.commands.executeCommand('vscode.openWith', uri, 'plantuml.glspDiagram')
    )
  );

  // Usage of default export command
  context.subscriptions.push(
    vscode.commands.registerCommand('plantuml.exportSvg', () =>
      connector.dispatchAction(RequestExportSvgAction.create())
    )
  );

  context.subscriptions.push(
      vscode.commands.registerCommand('plantuml.reopenGlspEditor', () => {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
          vscode.commands.executeCommand(
              'vscode.openWith',
              editor.document.uri,
              'plantuml.glspDiagram'
          );
        }
      })
  );
}
