/*
 * File: app.tsx
 * Author: Norman Babiak
 * Description: Message type views and other views for sequence diagram
 * Date: 1.5.2026
 */

import { GLSPStarter } from '@eclipse-glsp/vscode-integration-webview';
import { ContainerConfiguration } from '@eclipse-glsp/client';
import { Container } from 'inversify';
import { initializeSequenceContainer } from '@plantuml-client/glsp-client';
import { initializeClassContainer } from '@plantuml-client/glsp-client'

/**
 * GLSP starter that selects the correct DI container (class or sequence) based on the diagram type injected into the
 * webview window by the extension host
 */
class PlantUmlGLSPStarter extends GLSPStarter {
    private readonly diagramType: string;

    constructor() {
        super();

        this.diagramType = (window as any).DIAGRAM_TYPE;
    }

    /** Creates and returns the appropriate inversify container with all modules for the active diagram type */
    createContainer(...containerConfiguration: ContainerConfiguration): Container {
        if (this.diagramType == 'class-diagram') {
            return initializeClassContainer(new Container(), ...containerConfiguration)
        }

        return initializeSequenceContainer(new Container(), ...containerConfiguration);
    }
}

/**
 * Entry point called from the webview HTML script tag
 */
export function launch(): void {
    console.log('Starting webview app launch...');
    new PlantUmlGLSPStarter();
}