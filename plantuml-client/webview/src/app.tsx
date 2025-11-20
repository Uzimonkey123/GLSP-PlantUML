import { GLSPStarter } from '@eclipse-glsp/vscode-integration-webview';
import { ContainerConfiguration } from '@eclipse-glsp/client';
import { Container } from 'inversify';
import { initializeSequenceContainer } from '@plantuml-client/glsp-client';
import { initializeClassContainer } from '@plantuml-client/glsp-client'

class PlantUmlGLSPStarter extends GLSPStarter {
    private readonly diagramType: string;

    constructor() {
        super();

        this.diagramType = (window as any).DIAGRAM_TYPE;
    }

    createContainer(...containerConfiguration: ContainerConfiguration): Container {
        if (this.diagramType == 'class-diagram') {
            return initializeClassContainer(new Container(), ...containerConfiguration)
        }

        return initializeSequenceContainer(new Container(), ...containerConfiguration);
    }
}

export function launch(): void {
    console.log('Starting webview app launch...');
    new PlantUmlGLSPStarter();
}