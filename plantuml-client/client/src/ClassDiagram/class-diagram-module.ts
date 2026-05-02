/*
 * File: class-diagram-module.ts
 * Author: Norman Babiak
 * Description: Module file for class diagram, registering and initializing DI container
 * Date: 29.4.2026
 */

import {
    bindAsService,
    bindOrRebind,
    configureDefaultModelElements, configureModelElement,
    ContainerConfiguration,
    debugModule,
    DeleteElementContextMenuItemProvider, editLabelFeature,
    EditLabelUI,
    FeatureModule, GCompartment, GEdge, GLabel, GNode,
    gridModule,
    helperLineModule,
    initializeDiagramContainer,
    labelEditModule, moveFeature,
    resizeModule,
    RevealNamedElementActionProvider, selectFeature,
    TYPES
} from "@eclipse-glsp/client";

import {
    VSCODE_DEFAULT_MODULES
} from "@eclipse-glsp/vscode-integration-webview";

import {PlantUmlToolPalette} from "../plantuml-tool-palette";
import {BrEditLabelUI, HtmlLabelView} from "../utils-common";

import {Container} from "inversify";
import {defaultModule as clientDefaultModule} from "@eclipse-glsp/client/lib/base/default.module";
import {
    CircleEntityView,
    DiamondEntityView,
    EntityView,
    AssociationPointView,
    NoteEntityView, LollipopEntityView, InvisibleEntityView
} from "./class-entity-views";
import {
    EntityLabelView,
    HiddenLabelView, SimpleNoteEdgeView
} from "./class-views";

import {ClassLinkView} from "./ClassEdge/link-views";

import {
    PackageCloudView,
    PackageDatabaseView,
    PackageFolderView,
    PackageFrameView,
    PackageNodeView,
    PackageRectangleView,
    PackageHeaderView
} from "./class-package-view";
import '../../css/diagram.css';

const EDITABLE_FIXED_LABEL = { enable: [editLabelFeature, selectFeature], disable: [moveFeature] };
const EDITABLE_MOVABLE_LABEL = { enable: [editLabelFeature, selectFeature, moveFeature] };
const SELECTABLE_EDGE = { enable: [selectFeature] };

/**
 * Module function for class diagram, registering necessary components, node and edge views.
 */
export const ClassDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context = { bind, unbind, isBound, rebind };

        bindAsService(context, TYPES.ICommandPaletteActionProvider, RevealNamedElementActionProvider);
        bindAsService(context, TYPES.IContextMenuItemProvider, DeleteElementContextMenuItemProvider);
        bindOrRebind(context, TYPES.IUIExtension).to(BrEditLabelUI).inSingletonScope().whenTargetNamed(EditLabelUI.ID);

        bind(PlantUmlToolPalette).toSelf().inSingletonScope();
        bind(TYPES.IDiagramStartup).toService(PlantUmlToolPalette);

        configureDefaultModelElements(context);
        configureModelElement(context, "entity", GNode, EntityView);
        configureModelElement(context, "entity:circle", GNode, CircleEntityView);
        configureModelElement(context, "entity:diamond", GNode, DiamondEntityView);
        configureModelElement(context, 'entity:association-point', GNode, AssociationPointView);
        configureModelElement(context, 'entity:note', GNode, NoteEntityView);
        configureModelElement(context, "entity:lollipop", GNode, LollipopEntityView);
        configureModelElement(context, "entity:invis", GNode, InvisibleEntityView);

        configureModelElement(context, "link", GEdge, ClassLinkView, SELECTABLE_EDGE);
        configureModelElement(context, "link:note", GEdge, SimpleNoteEdgeView, SELECTABLE_EDGE);

        configureModelElement(context, "label:entityName", GLabel, EntityLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:stereotype", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:method", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:field", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:body", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:generic", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:note", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
        configureModelElement(context, "label:invis", GLabel, HiddenLabelView);
        configureModelElement(context, "label:link", GLabel, HtmlLabelView, EDITABLE_MOVABLE_LABEL);
        configureModelElement(context, "label:html", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);

        configureModelElement(context, 'package-folder', GCompartment, PackageFolderView);
        configureModelElement(context, 'package-rectangle', GCompartment, PackageRectangleView);
        configureModelElement(context, 'package-frame', GCompartment, PackageFrameView);
        configureModelElement(context, 'package-node', GCompartment, PackageNodeView);
        configureModelElement(context, 'package-database', GCompartment, PackageDatabaseView);
        configureModelElement(context, 'package-cloud', GCompartment, PackageCloudView);

        configureModelElement(context, 'comp:header', GCompartment, PackageHeaderView);
        configureModelElement(context, "label:heading", GLabel, HtmlLabelView, EDITABLE_FIXED_LABEL);
    }
);

/**
 * Creates and initializes the DI container for the class diagram with all required GLSP modules and the class diagram feature module.
 */
export function initializeClassContainer(container: Container, ...containerConfiguration: ContainerConfiguration): Container {
    return initializeDiagramContainer(
        container,
        helperLineModule,
        gridModule,
        debugModule,
        resizeModule,
        labelEditModule,
        clientDefaultModule,
        ...VSCODE_DEFAULT_MODULES,
        ClassDiagramModule,
        ...containerConfiguration
    );
}
