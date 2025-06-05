import { Container } from 'inversify';
import {
    helperLineModule,
    gridModule,
    debugModule,
    ContainerConfiguration,
    initializeDiagramContainer,
    FeatureModule,
    TYPES,
    configureDefaultModelElements,
    RevealNamedElementActionProvider,
    DeleteElementContextMenuItemProvider,
    defaultModule as clientDefaultModule,
    bindAsService,
    overrideModelElement,
    DefaultTypes,
    resizeModule,
    bindOrRebind,
    GGraph,
    GLSPProjectionView,
    GNode,
    GEdge,
    EditorContextService,
    EditMode,
    SetEditModeAction
} from '@eclipse-glsp/client';
import {
    VSCODE_DEFAULT_MODULES, 
    GLSPDiagramWidget,      
    GLSPDiagramWidgetFactory
} from '@eclipse-glsp/vscode-integration-webview';
import { SequenceMessageEdgeView, RectangularNodeView } from './sequence-views';

import { PlantUmlStartup } from './plantuml-startup';
import { PlantUmlGLSPDiagramWidget } from './plantuml-diagram-widget'; 
import '../css/diagram.css';
import 'sprotty/css/sprotty.css';
import 'sprotty/css/edit-label.css';
import 'balloon-css/balloon.min.css';

// Set every document to read only, since no need for editor
class ReadOnlyEditorContextService extends EditorContextService {
    protected initialize(): void {
        super.initialize();
        this._editMode = EditMode.READONLY;
    }

    // To ignore SetEditModeAction in case it is called
    protected handleSetEditModeAction(action: SetEditModeAction): void {
    }
}

export const PlantUmlDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context = { bind, unbind, isBound, rebind };

        bindOrRebind(context, EditorContextService).to(ReadOnlyEditorContextService).inSingletonScope();
        bindOrRebind(context, GLSPDiagramWidget).to(PlantUmlGLSPDiagramWidget).inSingletonScope();
        bindOrRebind(context, GLSPDiagramWidgetFactory).toFactory(context => () => context.container.get<PlantUmlGLSPDiagramWidget>(GLSPDiagramWidget));
        bindAsService(context, TYPES.ICommandPaletteActionProvider, RevealNamedElementActionProvider);
        bindAsService(context, TYPES.IContextMenuItemProvider, DeleteElementContextMenuItemProvider);
        bindAsService(context, TYPES.IDiagramStartup, PlantUmlStartup);

        configureDefaultModelElements(context);
        overrideModelElement(context, DefaultTypes.GRAPH, GGraph, GLSPProjectionView);
        overrideModelElement(context, DefaultTypes.NODE_RECTANGLE, GNode, RectangularNodeView);
        overrideModelElement(context, DefaultTypes.EDGE, GEdge, SequenceMessageEdgeView);
    }
);

export function initializePlantUmlDiagramContainer(container: Container, ...containerConfiguration: ContainerConfiguration): Container {
    return initializeDiagramContainer(
        container,
        helperLineModule,
        gridModule,
        debugModule,
        resizeModule,
        clientDefaultModule,
        ...VSCODE_DEFAULT_MODULES,
        PlantUmlDiagramModule,
        ...containerConfiguration
    );
}