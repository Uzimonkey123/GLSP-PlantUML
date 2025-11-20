import { GLSPStarter } from '@eclipse-glsp/vscode-integration-webview';
import { ContainerConfiguration } from '@eclipse-glsp/client';
import { Container } from 'inversify';
import { initializePlantUmlDiagramContainer } from '@plantuml-client/glsp-client';

class PlantUmlGLSPStarter extends GLSPStarter {
    private diagramType: string;

    constructor() {
        super();

        this.diagramType = (window as any).DIAGRAM_TYPE;
    }

    createContainer(...containerConfiguration: ContainerConfiguration): Container {
        return initializePlantUmlDiagramContainer(new Container(), ...containerConfiguration);
    }
}

export function launch(): void {
    console.log('Starting webview app launch...');
    new PlantUmlGLSPStarter();
}