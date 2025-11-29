import {
    bindAsService,
    bindOrRebind,
    configureDefaultModelElements, configureModelElement,
    ContainerConfiguration,
    debugModule,
    DeleteElementContextMenuItemProvider, editLabelFeature,
    EditLabelUI,
    FeatureModule, GLabel, GNode,
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
import {BrEditLabelUI} from "../utils";

import {Container} from "inversify";
import {defaultModule as clientDefaultModule} from "@eclipse-glsp/client/lib/base/default.module";
import {EntityView} from "./class-entity-views";
import {HtmlLabelView} from "../../lib/sequence-views";
import {EntityLabelView} from "./class-views";

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
        configureModelElement(context, "label:entityName", GLabel, EntityLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature]});
        configureModelElement(context, "label:method", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:field", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });
        configureModelElement(context, "label:body", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature], disable: [moveFeature] });

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