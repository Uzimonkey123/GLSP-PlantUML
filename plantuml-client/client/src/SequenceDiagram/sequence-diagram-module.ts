/*
 * File: sequence-diagram-module.ts
 * Author: Norman Babiak
 * Description: Module file for sequence diagram, registering and initializing DI container
 * Date: 30.4.2026
 */

import {Container} from 'inversify';
import {
    bindAsService,
    bindOrRebind,
    configureDefaultModelElements,
    configureModelElement,
    ContainerConfiguration,
    debugModule,
    defaultModule as clientDefaultModule,
    DefaultTypes,
    DeleteElementContextMenuItemProvider,
    editLabelFeature,
    EditLabelUI,
    FeatureModule,
    GEdge,
    GGraph,
    GLabel,
    GLSPProjectionView,
    GNode,
    gridModule,
    helperLineModule,
    initializeDiagramContainer,
    labelEditModule,
    moveFeature,
    overrideModelElement,
    resizeModule,
    RevealNamedElementActionProvider,
    selectFeature,
    TYPES
} from '@eclipse-glsp/client';
import {VSCODE_DEFAULT_MODULES} from '@eclipse-glsp/vscode-integration-webview';
import {
    AnchorEdgeView,
    GroupsView,
    NoteEdgeView,
    ParticipantLabelView,
    ReferenceEdgeView,
    SequenceMessageDelay,
    SequenceMessageDivider,
    SequenceMessageEdgeView
} from './sequence-views';

import {
    ActorNodeView,
    BoundaryNodeView,
    CollectionNodeView,
    ControlNodeView,
    DatabaseNodeView,
    DestroyCross,
    EngloberView,
    EntityNodeView,
    LifeEventBar,
    MainframeView,
    QueueNodeView,
    RectangularNodeView
} from "./sequence-node-views";

import {PlantUmlToolPalette} from '../plantuml-tool-palette';
import '../../css/diagram.css';
import 'sprotty/css/sprotty.css';
import 'sprotty/css/edit-label.css';
import 'balloon-css/balloon.min.css';
import {BrEditLabelUI, HtmlLabelView} from "../utils-common";

const SELECTABLE_NOT_MOVABLE = { enable: [selectFeature], disable: [moveFeature] };

/**
 * Module function for sequence diagram, registering necessary components, node and edge views.
 */
export const SequenceDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context = { bind, unbind, isBound, rebind };

        bindAsService(context, TYPES.ICommandPaletteActionProvider, RevealNamedElementActionProvider);
        bindAsService(context, TYPES.IContextMenuItemProvider, DeleteElementContextMenuItemProvider);
        bindOrRebind(context, TYPES.IUIExtension).to(BrEditLabelUI).inSingletonScope().whenTargetNamed(EditLabelUI.ID);

        bind(PlantUmlToolPalette).toSelf().inSingletonScope();
        bind(TYPES.IDiagramStartup).toService(PlantUmlToolPalette);

        configureDefaultModelElements(context);
        overrideModelElement(context, DefaultTypes.GRAPH, GGraph, GLSPProjectionView);
        overrideModelElement(context, DefaultTypes.EDGE, GEdge, SequenceMessageEdgeView, { disable: [moveFeature, moveFeature] });
        configureModelElement(context, "PARTICIPANT", GNode, RectangularNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "ACTOR", GNode, ActorNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "BOUNDARY", GNode, BoundaryNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "CONTROL", GNode, ControlNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "ENTITY", GNode, EntityNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "DATABASE", GNode, DatabaseNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "COLLECTIONS", GNode, CollectionNodeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "QUEUE", GNode, QueueNodeView, SELECTABLE_NOT_MOVABLE);

        configureModelElement(context, "edge:delay", GEdge, SequenceMessageDelay, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "edge:divider", GEdge, SequenceMessageDivider, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "edge:ref", GEdge, ReferenceEdgeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "edge:notes", GEdge, NoteEdgeView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "label:html", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:participant", GLabel, ParticipantLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature]});

        configureModelElement(context, "anchor-arrow", GEdge, AnchorEdgeView, { disable: [moveFeature, moveFeature] });
        configureModelElement(context, "lifeEvent", GNode, LifeEventBar, { disable: [moveFeature, moveFeature] });
        configureModelElement(context, "destroy", GNode, DestroyCross, { disable: [moveFeature, moveFeature] })
        configureModelElement(context, "group", GEdge, GroupsView, { disable: [selectFeature]});
        configureModelElement(context, "participant-englober", GNode, EngloberView, SELECTABLE_NOT_MOVABLE);
        configureModelElement(context, "mainframe", GNode, MainframeView, { disable: [moveFeature, moveFeature] });
    }
);

/**
 * Creates and initializes the DI container for the sequence diagram with all required GLSP modules and the class diagram feature module.
 */
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
