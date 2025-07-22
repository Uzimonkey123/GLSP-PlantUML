import {Container, injectable} from 'inversify';
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
    labelEditModule,
    EditLabelUI, SModelRegistry
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
    EngloberView
} from "./sequence-node-views";

import { PlantUmlStartup } from './plantuml-startup';
import { PlantUmlGLSPDiagramWidget } from './plantuml-diagram-widget';
import '../css/diagram.css';
import 'sprotty/css/sprotty.css';
import 'sprotty/css/edit-label.css';
import 'balloon-css/balloon.min.css';

@injectable()
export class BrEditLabelUI extends EditLabelUI {
    private isMultilineLabel(): boolean {
        const id = this.label?.id ?? '';
        return !id.startsWith('group-');  // Groups do not have multiline label/comment/separator
    }

    public override get editControl(): HTMLInputElement | HTMLTextAreaElement {
        // Check if input or text area is needed for label/multiline
        return this.isMultilineLabel() ? this.textAreaElement : this.inputElement;
    }

    protected override configureAndAdd(
        element: HTMLInputElement | HTMLTextAreaElement,
        container: HTMLElement
    ): void {
        // Make the previous handlers as they should be
        super.configureAndAdd(element, container);

        element.style.overflow = 'hidden'; // no scrollbar
        element.style.resize = 'none';

        element.addEventListener('keydown', (ev: Event) => {
            const e = ev as KeyboardEvent;
            if (e.key === 'Enter' && e.shiftKey) { // Shift enter for next line
                e.preventDefault();
                e.stopPropagation(); // Do not apply at shift + enter
                this.insertAtCursor(element, '\n');
                this.autoSizeEditor();
            }
        });

        // To change size automatically
        element.addEventListener('input', () => this.autoSizeEditor());

        // Create the first sized elements with DOM
        requestAnimationFrame(() => this.autoSizeEditor());
    }

    protected override applyTextContents(): void {
        if (!this.label) return;
        // To display multilines instead of simple <br>
        const display = (this.label.text ?? '').replace(/<br>/g, '\n');
        this.editControl.value = display;

        if (this.editControl instanceof HTMLTextAreaElement) {
            // To see the beginning of the message, not scrolled etc...
            this.editControl.selectionStart = this.editControl.selectionEnd = display.length;
            this.editControl.scrollTop = 0;
            this.editControl.scrollLeft = 0;

        } else {
            // For input text
            this.editControl.setSelectionRange(0, display.length);
        }
    }

    protected override applyFontStyling(): void {
        super.applyFontStyling();
        requestAnimationFrame(() => this.autoSizeEditor());
    }

    protected override async applyLabelEdit(): Promise<void> {
        this.editControl.value = this.editControl.value
            .replace(/\r?\n/g, '<br>');
        await super.applyLabelEdit();
    }

    private insertAtCursor(el: HTMLInputElement | HTMLTextAreaElement, text: string): void {
        const start = el.selectionStart ?? 0;
        const end   = el.selectionEnd ?? 0;
        el.value = el.value.substring(0, start) + text + el.value.substring(end);
        const pos = start + text.length;
        el.setSelectionRange(pos, pos);
    }

    private autoSizeEditor(): void {
        if (!(this.editControl instanceof HTMLTextAreaElement)) return;
        const ta = this.editControl;

        ta.style.height = 'auto';
        ta.style.height = ta.scrollHeight + 'px';

        ta.style.width = 'auto';
        ta.style.width = ta.scrollWidth + 4 + 'px';
    }
}

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
        bindOrRebind(context, TYPES.IUIExtension).to(BrEditLabelUI).inSingletonScope().whenTargetNamed(EditLabelUI.ID);

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
        configureModelElement(context, "edge:notes", GEdge, NoteEdgeView, { disable: [selectFeature]});
        configureModelElement(context, "label:html", GLabel, HtmlLabelView, { enable: [editLabelFeature, selectFeature, moveFeature] });
        configureModelElement(context, "label:participant", GLabel, ParticipantLabelView, { enable: [editLabelFeature, selectFeature, moveFeature] });
        configureModelElement(context, "label:header", GLabel, SequenceHeaderFooter);
        configureModelElement(context, "label:footer", GLabel, SequenceHeaderFooter);
        configureModelElement(context, "label:title", GLabel, SequenceTitle);

        configureModelElement(context, "anchor-arrow", GEdge, AnchorEdgeView);
        configureModelElement(context, "lifeEvent", GNode, LifeEventBar);
        configureModelElement(context, "destroy", GNode, DestroyCross)
        configureModelElement(context, "group", GEdge, GroupsView, { disable: [selectFeature]});
        configureModelElement(context, "participant-englober", GNode, EngloberView);
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