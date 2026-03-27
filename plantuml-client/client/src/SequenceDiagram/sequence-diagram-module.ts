import {Container} from 'inversify';
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
    configureModelElement,
    GLabel,
    editLabelFeature,
    selectFeature,
    moveFeature,
    labelEditModule,
    EditLabelUI
} from '@eclipse-glsp/client';
import {
    VSCODE_DEFAULT_MODULES,
    GLSPDiagramWidget,
    GLSPDiagramWidgetFactory
} from '@eclipse-glsp/vscode-integration-webview';
import {
    SequenceMessageEdgeView,
    SequenceMessageDelay,
    //HtmlLabelView,
    SequenceMessageDivider,
    AnchorEdgeView,
    ParticipantLabelView,
    ReferenceEdgeView,
    GroupsView,
    NoteEdgeView
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
    DestroyCross,
    EngloberView,
    MainframeView
} from "./sequence-node-views";

import { PlantUmlGLSPDiagramWidget } from '../plantuml-diagram-widget';
import { PlantUmlToolPalette } from '../plantuml-tool-palette';
import '../../css/diagram.css';
import 'sprotty/css/sprotty.css';
import 'sprotty/css/edit-label.css';
import 'balloon-css/balloon.min.css';
import {BrEditLabelUI, HtmlLabelView} from "../utils-common";

export const SequenceDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context = { bind, unbind, isBound, rebind };

        bindOrRebind(context, GLSPDiagramWidget).to(PlantUmlGLSPDiagramWidget).inSingletonScope();
        bindOrRebind(context, GLSPDiagramWidgetFactory).toFactory(context => () => context.container.get<PlantUmlGLSPDiagramWidget>(GLSPDiagramWidget));
        bindAsService(context, TYPES.ICommandPaletteActionProvider, RevealNamedElementActionProvider);
        bindAsService(context, TYPES.IContextMenuItemProvider, DeleteElementContextMenuItemProvider);
        bindOrRebind(context, TYPES.IUIExtension).to(BrEditLabelUI).inSingletonScope().whenTargetNamed(EditLabelUI.ID);

        bind(PlantUmlToolPalette).toSelf().inSingletonScope();
        bind(TYPES.IDiagramStartup).toService(PlantUmlToolPalette);

        configureDefaultModelElements(context);
        overrideModelElement(context, DefaultTypes.GRAPH, GGraph, GLSPProjectionView);
        overrideModelElement(context, DefaultTypes.EDGE, GEdge, SequenceMessageEdgeView, { enable: [selectFeature], disable: [moveFeature] });
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
        configureModelElement(context, "edge:notes", GEdge, NoteEdgeView);
        configureModelElement(context, "label:html", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:participant", GLabel, ParticipantLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature]});

        configureModelElement(context, "anchor-arrow", GEdge, AnchorEdgeView);
        configureModelElement(context, "lifeEvent", GNode, LifeEventBar);
        configureModelElement(context, "destroy", GNode, DestroyCross)
        configureModelElement(context, "group", GEdge, GroupsView, { disable: [selectFeature]});
        configureModelElement(context, "participant-englober", GNode, EngloberView);
        configureModelElement(context, "mainframe", GNode, MainframeView);
    }
);

export function initializeSequenceContainer(container: Container, ...containerConfiguration: ContainerConfiguration): Container {
    return initializeDiagramContainer(
        container,
        helperLineModule,
        gridModule,
        debugModule,
        resizeModule,
        labelEditModule,
        clientDefaultModule,
        ...VSCODE_DEFAULT_MODULES,
        SequenceDiagramModule,
        ...containerConfiguration
    );
}