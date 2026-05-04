/*
 * File: diagramTypeParser.ts
 * Author: Norman Babiak
 * Description: File for parsing diagram type upon the .puml file is opened
 * Date: 4.5.2026
 */

import * as vscode from "vscode";

const CLASS_SIGNALS = new RegExp(
    '\\b(class|interface|enum|abstract|annotation|dataclass|entity|exception'
    + '|metaclass|protocol|record|stereotype|struct)\\b'
    + '|\\b(package|namespace|rectangle|node|cloud|database'
    + '|frame|storage|component|folder)\\b'
    + '|\\{\\s*([+\\-#~])'
    + '|<\\|--|<\\|\\.\\.'
    + '|--\\|>|\\.\\.\\|>'
    + '|--\\*|--o|o--|\\*--'
    + '|\\.\\.>|<\\.\\.'
    + '|\\bdiamond\\b|\\bcircle\\b'
    + '|<<\\w+>>'
    + '|\\{field}|\\{method}'
    + '|\\{static}|\\{abstract}'
    + '|\\b(hide|show)\\s+(empty|members|fields|methods|circle|stereotype)',
    'gm'
);

const SEQUENCE_SIGNALS = new RegExp(
    '\\b(participant|actor|boundary|control|collections|queue)\\s'
    + '|\\b(activate|deactivate|destroy)\\s'
    + '|\\b(alt|else|opt|loop|par|break|critical|group|ref)\\b'
    + '|\\b(autonumber|newpage|rnote|hnote)\\b'
    + '|\\breturn\\b'
    + '|\\S+\\s*-+>+\\s*\\S+\\s*:'
    + '|\\S+\\s*<-+\\s*\\S+\\s*:',
    'gm'
);

/**
 * Determines the diagram type by counting class and sequence signal matches.
 * If no class signals dominate, sequence diagram is selected as fallback
 */
export default async function parseDiagramType(document: { uri: vscode.Uri }) {
    try {
        const fileContent = await vscode.workspace.fs.readFile(document.uri);
        const content = new TextDecoder().decode(fileContent);

        const classMatches = content.match(CLASS_SIGNALS) || [];
        const seqMatches = content.match(SEQUENCE_SIGNALS) || [];

        return classMatches.length > seqMatches.length ? 'class-diagram' : 'sequence-diagram';

    } catch (error) {
        console.error('Error parsing diagram type:', error);
        return 'sequence-diagram'; // fallback
    }
}
