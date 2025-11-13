import * as vscode from "vscode";

export default async function parseDiagramType(document: vscode.CustomDocument) {
    try {
        const fileContent = await vscode.workspace.fs.readFile(document.uri);
        const content = new TextDecoder().decode(fileContent);

        if (/\bclass\b|\binterface\b|\benum\b|\babstract\b/.test(content)) {
            return 'class-diagram';
        }

        return 'sequence-diagram'; // fallback
    } catch (error) {
        console.error('Error parsing diagram type:', error);
        return 'sequence-diagram'; // fallback
    }
}