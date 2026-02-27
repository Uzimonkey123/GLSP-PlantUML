import 'reflect-metadata';
import * as vscode from 'vscode';
import {
    GlspVscodeConnector,
    SocketGlspVscodeServer,
    configureDefaultCommands,
} from '@eclipse-glsp/vscode-integration';
import PumlEditorProvider from './editor-provider';
import { RequestExportSvgAction } from 'sprotty';
import { SyntaxValidator } from "./syntaxValidation";

export async function activate(context: vscode.ExtensionContext): Promise<void> {
    let server: SocketGlspVscodeServer | undefined;
    let connector: GlspVscodeConnector | undefined;

    validateFile(context);

    // Running server initialization only once, so reconnect is possible
    const initialized = () => {
        if (server) return;

        // Connecting to the Java server that is already running
        server = new SocketGlspVscodeServer({
            clientId: "glsp.puml",
            clientName: "PlantUML",
            connectionOptions: {
                port: 5007,
                path: "plantuml",
            }
        });

        context.subscriptions.push(server);

        connector = new GlspVscodeConnector({server, logging: true});

        // Registering custom editor provider
        const provider = new PumlEditorProvider(context, connector);
        const registration = vscode.window.registerCustomEditorProvider(
            'plantuml.glspDiagram',
            provider,
            {
                webviewOptions: {retainContextWhenHidden: false},
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

        // Usage of default export command
        context.subscriptions.push(
            vscode.commands.registerCommand('plantuml.exportSvg', () =>
                connector!.dispatchAction(RequestExportSvgAction.create())
            )
        );
    }

    // Command to open .puml in editor
    context.subscriptions.push(
        vscode.commands.registerCommand('plantuml.openPreview', async (uri?: vscode.Uri) => {
            const resource = uri ?? vscode.window.activeTextEditor?.document.uri;

            // Check for server
            initialized();

            try {
                await server!.start();

            } catch (err) {
                return vscode.window.showErrorMessage('Error: Couldn\'t connect to server.');
            }

            await vscode.commands.executeCommand(
                'vscode.openWith',
                resource,
                'plantuml.glspDiagram',
                vscode.ViewColumn.Beside
            );
        })
    );
}

function validateFile(context: vscode.ExtensionContext): void {
    const diagnosticCollection = vscode.languages.createDiagnosticCollection('plantuml');
    const validator = new SyntaxValidator(diagnosticCollection);

    context.subscriptions.push(diagnosticCollection, validator);

    context.subscriptions.push(
        vscode.workspace.onDidChangeTextDocument(event =>
            validator.validate(event.document)
        ),
        vscode.workspace.onDidOpenTextDocument(document =>
            validator.validate(document)
        )
    );

    // Validate all open .puml documents
    vscode.workspace.textDocuments.forEach(document => {
        if (document.languageId === 'plantuml') {
            validator.validate(document);
        }
    });
}
