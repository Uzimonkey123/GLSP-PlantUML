import 'reflect-metadata';
import * as vscode from 'vscode';
import * as net from 'net';
import * as path from 'path';
import { ChildProcess, spawn } from 'child_process';
import {
    GlspVscodeConnector,
    SocketGlspVscodeServer,
    configureDefaultCommands,
} from '@eclipse-glsp/vscode-integration';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind,
} from 'vscode-languageclient/node';
import PumlEditorProvider from './editor-provider';
import { RequestExportSvgAction } from 'sprotty';
import {RequestModelAction} from "@eclipse-glsp/client";

const GLSP_PORT = 5007;
const JAR_NAME = 'GLSPPlantUML-1.0-SNAPSHOT.jar';
const SERVER_READY_TIMEOUT = 15000;
const RETRY_INTERVAL = 500;

let serverProcess: ChildProcess | undefined;
let languageClient: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
    let server: SocketGlspVscodeServer | undefined;
    let connector: GlspVscodeConnector | undefined;
    let provider: PumlEditorProvider | undefined;

    startLanguageClient(context).catch(err => {
        console.error('[PlantUML] LSP client failed to start:', err);
    });

    // Running server initialization only once, so reconnect is possible
    const initialized = () => {
        if (server) return;

        // Connecting to the Java server that is already running
        server = new SocketGlspVscodeServer({
            clientId: "glsp.puml",
            clientName: "PlantUML",
            connectionOptions: {
                port: GLSP_PORT,
                path: "plantuml",
            }
        });

        context.subscriptions.push(server);

        connector = new GlspVscodeConnector({server, logging: true});

        // Registering custom editor provider
        provider = new PumlEditorProvider(context, connector);
        const registration = vscode.window.registerCustomEditorProvider(
            'plantuml.glspDiagram',
            provider,
            {
                webviewOptions: {retainContextWhenHidden: true},
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
    };

    context.subscriptions.push({
        dispose: () => {
            if (serverProcess) {
                serverProcess.kill();
                serverProcess = undefined;
            }
        }
    });

    // Command to open .puml in editor
    context.subscriptions.push(
        vscode.commands.registerCommand('plantuml.openPreview', async (uri?: vscode.Uri) => {
            const resource = uri ?? vscode.window.activeTextEditor?.document.uri;

            // Check for server
            initialized();

            try {
                await launchServerProcess(context);
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

    context.subscriptions.push(
        vscode.workspace.onDidSaveTextDocument(async (document) => {
            vscode.window.showInformationMessage(`File saved: ${document.languageId}`);

            if (document.languageId === 'plantuml' && connector && provider) {
                const hasPanel = provider.refreshDiagram(document.uri);

                if (hasPanel) {
                    setTimeout(() => {
                        connector!.dispatchAction(RequestModelAction.create({
                            requestId: '',
                            options: {
                                sourceUri: document.uri.toString(),
                                needsClientLayout: true
                            }
                        }));
                    }, 500);
                }
            }
        })
    );
}

async function startLanguageClient(context: vscode.ExtensionContext): Promise<void> {
    const jarPath = path.join(context.extensionPath, 'server', JAR_NAME);

    const serverOptions: ServerOptions = {
        run: {
            command: 'java',
            args: ['-cp', jarPath, 'com.GLSPPlantUML.lsp.LSPServerLauncher'],
            transport: TransportKind.stdio,
        },
        debug: {
            command: 'java',
            args: ['-cp', jarPath, 'com.GLSPPlantUML.lsp.LSPServerLauncher'],
            transport: TransportKind.stdio,
        }
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ language: 'plantuml' }],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.puml')
        }
    };

    languageClient = new LanguageClient(
        'plantumlLSP',
        'PlantUML Language Server',
        serverOptions,
        clientOptions
    );

    context.subscriptions.push(languageClient);
    await languageClient.start();
}

async function launchServerProcess(context: vscode.ExtensionContext): Promise<void> {
    if (serverProcess) {
        return;
    }

    const jarPath = path.join(context.extensionPath, 'server', JAR_NAME);
    console.log('[PlantUML] JAR path:', jarPath);

    serverProcess = spawn('java', [
        '-jar', jarPath,
        '--websocket',
        '--port', String(GLSP_PORT),
        '--host', 'localhost'
    ], {
        cwd: context.extensionPath
    });

    serverProcess.stdout?.on('data', (data: Buffer) => {
        console.log(`[PlantUML:server] ${data.toString().trim()}`);
    });

    serverProcess.stderr?.on('data', (data: Buffer) => {
        console.error(`[PlantUML:server:err] ${data.toString().trim()}`);
    });

    serverProcess.on('error', (err) => {
        console.error('[PlantUML] Failed to spawn server process:', err);
    });

    serverProcess.on('exit', (code) => {
        serverProcess = undefined;
    });

    await waitForPort(GLSP_PORT, SERVER_READY_TIMEOUT);
}

function waitForPort(port: number, timeout: number): Promise<void> {
    return new Promise((resolve, reject) => {
        const start = Date.now();

        const tryConnect = () => {
            const socket = net.createConnection({port, host: 'localhost'}, () => {
                socket.destroy();
                resolve();
            });

            socket.on('error', () => {
                socket.destroy();

                if (Date.now() - start > timeout) {
                    reject(new Error(`Server did not start`));

                } else {
                    setTimeout(tryConnect, RETRY_INTERVAL);
                }
            });
        };

        tryConnect();
    });
}

export async function deactivate(): Promise<void> {
    if (languageClient) {
        await languageClient.stop();
    }
}