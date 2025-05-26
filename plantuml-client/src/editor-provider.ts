import { GlspEditorProvider, GlspVscodeConnector, SetEditModeAction, RequestModelAction } from '@eclipse-glsp/vscode-integration';
import * as vscode from 'vscode';

export default class PumlEditorProvider extends GlspEditorProvider {
    diagramType = "sequence-diagram";
    extensionContext: vscode.ExtensionContext;

  }