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
    GLSPDiagramWidget,
    GLSPDiagramWidgetFactory,
    VSCODE_DEFAULT_MODULES
} from "@eclipse-glsp/vscode-integration-webview";

import {PlantUmlGLSPDiagramWidget} from "../plantuml-diagram-widget";
import {PlantUmlStartup} from "../plantuml-startup";
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

export const ClassDiagramModule = new FeatureModule(
    (bind, unbind, isBound, rebind) => {
        const context = { bind, unbind, isBound, rebind };
        console.log("CLASS DIAGRAM MODULE FRONTEND");

        bindOrRebind(context, GLSPDiagramWidget).to(PlantUmlGLSPDiagramWidget).inSingletonScope();
        bindOrRebind(context, GLSPDiagramWidgetFactory).toFactory(context => () => context.container.get<PlantUmlGLSPDiagramWidget>(GLSPDiagramWidget));
        bindAsService(context, TYPES.ICommandPaletteActionProvider, RevealNamedElementActionProvider);
        bindAsService(context, TYPES.IContextMenuItemProvider, DeleteElementContextMenuItemProvider);
        bindAsService(context, TYPES.IDiagramStartup, PlantUmlStartup);
        bindOrRebind(context, TYPES.IUIExtension).to(BrEditLabelUI).inSingletonScope().whenTargetNamed(EditLabelUI.ID);

        configureDefaultModelElements(context);
        configureModelElement(context, "entity", GNode, EntityView);
        configureModelElement(context, "entity:circle", GNode, CircleEntityView);
        configureModelElement(context, "entity:diamond", GNode, DiamondEntityView);
        configureModelElement(context, 'entity:association-point', GNode, AssociationPointView);
        configureModelElement(context, 'entity:note', GNode, NoteEntityView);
        configureModelElement(context, "entity:lollipop", GNode, LollipopEntityView);
        configureModelElement(context, "entity:invis", GNode, InvisibleEntityView);

        configureModelElement(context, "link", GEdge, ClassLinkView);
        configureModelElement(context, "link:note", GEdge, SimpleNoteEdgeView);

        configureModelElement(context, "label:entityName", GLabel, EntityLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature]});
        configureModelElement(context, "label:stereotype", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature, moveFeature]});
        configureModelElement(context, "label:method", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:field", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:body", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:generic", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:note", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:invis", GLabel, HiddenLabelView);
        configureModelElement(context, "label:link", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature, moveFeature]});

        configureModelElement(context, 'package-folder', GCompartment, PackageFolderView);
        configureModelElement(context, 'package-rectangle', GCompartment, PackageRectangleView);
        configureModelElement(context, 'package-frame', GCompartment, PackageFrameView);
        configureModelElement(context, 'package-node', GCompartment, PackageNodeView);
        configureModelElement(context, 'package-database', GCompartment, PackageDatabaseView);
        configureModelElement(context, 'package-cloud', GCompartment, PackageCloudView);

        configureModelElement(context, 'comp:header', GCompartment, PackageHeaderView);
        configureModelElement(context, "label:heading", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
    }
);

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