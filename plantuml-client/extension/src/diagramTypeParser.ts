/*
 * File: diagramTypeParser.ts
 * Author: Norman Babiak
 * Description: File for parsing diagram type upon the .puml file is opened
 * Date: 4.5.2026
 */

import * as vscode from "vscode";

const CLASS_DECLARATIONS = new RegExp(
    '(?:^|\\n)\\s*(?:'
    + 'class|interface|enum|abstract(?:\\s+class)?|annotation|dataclass'
    + '|exception|metaclass|protocol|record|stereotype|struct'
    + '|circle|diamond'
    + ')\\s',
    'm'
);

const SEQUENCE_DECLARATIONS = new RegExp(
    '(?:^|\\n)\\s*(?:'
    + 'participant|actor|boundary|control|collections|queue'
    + ')\\s',
    'm'
);

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
 * Determines the diagram type by first checking for unambiguous declaration
 * keywords. If none are found, falls back to counting signal matches.
 * Sequence diagram is the final fallback if nothing dominates.
 */
export default async function parseDiagramType(document: { uri: vscode.Uri }) {
    try {
        const fileContent = await vscode.workspace.fs.readFile(document.uri);
        const content = new TextDecoder().decode(fileContent);

        if (CLASS_DECLARATIONS.test(content)) {
            return 'class-diagram';
        }

        if (SEQUENCE_DECLARATIONS.test(content)) {
            return 'sequence-diagram';
        }

        const classMatches = content.match(CLASS_SIGNALS) || [];
        const seqMatches = content.match(SEQUENCE_SIGNALS) || [];

        return classMatches.length > seqMatches.length ? 'class-diagram' : 'sequence-diagram';

    } catch (error) {
        console.error('Error parsing diagram type:', error);
        return 'sequence-diagram'; // fallback
    }
}
