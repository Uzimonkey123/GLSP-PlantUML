/*
 * File: diagramTypeParser.ts
 * Author: Norman Babiak
 * Description: File for parsing diagram type upon the .punl file is opened
 * Date: 2.5.2026
 */

import * as vscode from "vscode";

const CLASS_DIAGRAM_PATTERN = new RegExp(
    '\\b(class|interface|enum|abstract|annotation|dataclass|entity|exception'
    + '|metaclass|protocol|record|stereotype|struct)\\b'
    + '|\\b(package|namespace|rectangle|node|cloud|database'
    + '|frame|storage|component|folder)\\b'
    + '|\\{\\s*([+\\-#~])'
    + '|--|>|\\.\\.\\|>'
    + '|--\\*|--o'
    + '|<\\|--|<\\|\\.\\.'
    + '|\\.\\.[>]|--[>]'
    + '|o--|\\*--'
    + '|\\bdiamond\\b|\\bcircle\\b|<>'
    + '|<<\\w+>>'
    + '|\\{field}|\\{method}'
    + '|\\{static}|\\{abstract}'
    + '|\\b[A-Za-z_]\\w*\\s*:\\s*\\S'
    + '|^\\s*-{2,}\\s*$|^\\s*[.]{2,}\\s*$|^\\s*={2,}\\s*$|^\\s*_{2,}\\s*$'
    + '|\\b(hide|show)\\s+(empty|members|fields|methods|circle|stereotype)'
    + '|\\bleft\\s+to\\s+right\\s+direction\\b',
    'm'
);

/**
 * Function to parser diagram type regarding diagram keywords, if no pattern matches, sequence diagram is selected as fallback
 */
export default async function parseDiagramType(document: { uri: vscode.Uri }) {
    try {
        const fileContent = await vscode.workspace.fs.readFile(document.uri);
        const content = new TextDecoder().decode(fileContent);

        if (CLASS_DIAGRAM_PATTERN.test(content)) {
            return 'class-diagram';
        }

        return 'sequence-diagram'; // fallback
    } catch (error) {
        console.error('Error parsing diagram type:', error);
        return 'sequence-diagram'; // fallback
    }
}
