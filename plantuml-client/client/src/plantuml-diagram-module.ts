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
    SetEditModeAction,
    configureModelElement,
    GLabel,
    editLabelFeature,
    selectFeature,
    moveFeature,
    labelEditModule
} from '@eclipse-glsp/client';
import {
    VSCODE_DEFAULT_MODULES, 
    GLSPDiagramWidget,      
    GLSPDiagramWidgetFactory
} from '@eclipse-glsp/vscode-integration-webview';
import {
    SequenceMessageEdgeView,
    SequenceMessageDelay,
    HtmlLabelView,
    SequenceMessageDivider,
    SequenceHeaderFooter,
    SequenceTitle,
    AnchorEdgeView,
    ParticipantLabelView,
    ReferenceEdgeView, GroupsView
} from './sequence-views';

import {
    RectangularNodeView,
    ActorNodeView,
    BoundaryNodeView,
    ControlNodeView,
    EntityNodeView,
    DatabaseNodeView,
    CollectionNodeView,
    QueueNodeView,
    LifeEventBar,
    DestroyCross
} from "./sequence-node-views";

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
        this._editMode = EditMode.EDITABLE;
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
        overrideModelElement(context, DefaultTypes.EDGE, GEdge, SequenceMessageEdgeView);
        configureModelElement(context, "PARTICIPANT", GNode, RectangularNodeView);
        configureModelElement(context, "ACTOR", GNode, ActorNodeView);
        configureModelElement(context, "BOUNDARY", GNode, BoundaryNodeView);
        configureModelElement(context, "CONTROL", GNode, ControlNodeView);
        configureModelElement(context, "ENTITY", GNode, EntityNodeView);
        configureModelElement(context, "DATABASE", GNode, DatabaseNodeView);
        configureModelElement(context, "COLLECTIONS", GNode, CollectionNodeView);
        configureModelElement(context, "QUEUE", GNode, QueueNodeView);

        configureModelElement(context, "edge:delay", GEdge, SequenceMessageDelay);
        configureModelElement(context, "edge:divider", GEdge, SequenceMessageDivider);
        configureModelElement(context, "edge:ref", GEdge, ReferenceEdgeView);
        configureModelElement(context, "label:html", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature, moveFeature] });
        configureModelElement(context, "label:participant", GLabel, ParticipantLabelView, { enable: [editLabelFeature, selectFeature, moveFeature] });
        configureModelElement(context, "label:header", GLabel, SequenceHeaderFooter);
        configureModelElement(context, "label:footer", GLabel, SequenceHeaderFooter);
        configureModelElement(context, "label:title", GLabel, SequenceTitle);

        configureModelElement(context, "anchor-arrow", GEdge, AnchorEdgeView);
        configureModelElement(context, "lifeEvent", GNode, LifeEventBar);
        configureModelElement(context, "destroy", GNode, DestroyCross)
        configureModelElement(context, "group", GEdge, GroupsView);
    }
);

export function initializePlantUmlDiagramContainer(container: Container, ...containerConfiguration: ContainerConfiguration): Container {
    return initializeDiagramContainer(
        container,
        helperLineModule,
        gridModule,
        debugModule,
        resizeModule,
        labelEditModule,
        clientDefaultModule,
        ...VSCODE_DEFAULT_MODULES,
        PlantUmlDiagramModule,
        ...containerConfiguration
    );
}