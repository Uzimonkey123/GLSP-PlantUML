import {
    IDiagramStartup,
    EditorContextService,
    DeleteElementOperation,
    SelectionService,
    GModelRoot,
    TYPES,
    IActionDispatcher, IGridManager
} from '@eclipse-glsp/client';
import { MaybePromise } from '@eclipse-glsp/sprotty';
import { RequestExportSvgAction } from 'sprotty';
import {inject, injectable, optional} from 'inversify';
import '../css/tool-palette.css';

const ICON_TRASH = `<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <path d="M11 1.75V3h2.5a.75.75 0 0 1 0 1.5H2.5a.75.75 0 0 1 0-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75ZM6.5 1.75V3h3V1.75a.25.25 0 0 0-.25-.25h-2.5a.25.25 0 0 0-.25.25ZM3.613 5.5l.737 8.834A1.75 1.75 0 0 0 6.096 16h3.808a1.75 1.75 0 0 0 1.746-1.666L12.387 5.5H3.613Z"/>
</svg>`;

const ICON_EXPORT = `<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <path d="M8 1a.75.75 0 0 1 .75.75v6.69l1.72-1.72a.75.75 0 1 1 1.06 1.06l-3 3a.75.75 0 0 1-1.06 0l-3-3a.75.75 0 0 1 1.06-1.06l1.72 1.72V1.75A.75.75 0 0 1 8 1ZM2.75 11a.75.75 0 0 1 .75.75v1.5c0 .138.112.25.25.25h8.5a.25.25 0 0 0 .25-.25v-1.5a.75.75 0 0 1 1.5 0v1.5A1.75 1.75 0 0 1 12.25 15h-8.5A1.75 1.75 0 0 1 2 13.25v-1.5a.75.75 0 0 1 .75-.75Z"/>
</svg>`;

@injectable()
export class PlantUmlToolPalette implements IDiagramStartup {
    static readonly ID = 'plantuml-tool-palette';

    @inject(TYPES.IActionDispatcher)
    protected readonly actionDispatcher!: IActionDispatcher;

    @inject(TYPES.IGridManager) @optional()
    protected gridManager?: IGridManager;

    @inject(EditorContextService)
    protected editorContext!: EditorContextService;

    @inject(SelectionService)
    protected selectionService!: SelectionService;

    private deleteMode = false;
    private deleteModeBtn?: HTMLDivElement;

    rank = 10;

    preRequestModel(): MaybePromise<void> {
        this.gridManager?.setGridVisible(true);
    }

    postModelInitialization(): MaybePromise<void> {
        this.createPalette();
    }

    private createPalette(): void {
        document.getElementById(PlantUmlToolPalette.ID)?.remove();

        const container = document.createElement('div');
        container.id = PlantUmlToolPalette.ID;
        container.classList.add('plantuml-tool-palette');

        this.deleteModeBtn = this.createButton(
            'btn-delete-element', 'Delete Element', ICON_TRASH,
            () => this.handleDeleteClick()
        );
        container.appendChild(this.deleteModeBtn);

        container.appendChild(
            this.createButton('btn-export-svg', 'Export SVG', ICON_EXPORT,
            () => this.handleExportSvg()
            )
        );

        document.body.appendChild(container);
        this.selectionService.onSelectionChanged(event => this.onSelectionChanged(event));
    }

    private createButton(
        id: string,
        label: string,
        iconSvg: string,
        handler: () => void
    ): HTMLDivElement {
        const btn = document.createElement('div');
        btn.id = id;
        btn.classList.add('palette-button');
        btn.title = label;

        const iconContainer = document.createElement('span');
        iconContainer.classList.add('palette-icon');
        iconContainer.innerHTML = iconSvg;
        btn.appendChild(iconContainer);

        const text = document.createElement('span');
        text.textContent = label;
        btn.appendChild(text);

        btn.addEventListener('click', (e) => { e.stopPropagation(); handler(); });
        return btn;
    }

    private handleDeleteClick(): void {
        const selectedIds = this.editorContext.selectedElements.map(e => e.id);
        if (selectedIds.length > 0 && !this.deleteMode) {
            this.actionDispatcher.dispatch(DeleteElementOperation.create(selectedIds));

            return;
        }

        this.setDeleteMode(!this.deleteMode);
    }

    private onSelectionChanged(
        event: {
            root: Readonly<GModelRoot>;
            selectedElements: string[]
        }
    ): void {
        if (!this.deleteMode || event.selectedElements.length === 0) return;

        this.actionDispatcher.dispatch(DeleteElementOperation.create([...event.selectedElements]));
        this.setDeleteMode(false);
    }

    private setDeleteMode(active: boolean): void {
        this.deleteMode = active;
        if (!this.deleteModeBtn) return;

        if (active) {
            this.deleteModeBtn.classList.add('active');
            this.deleteModeBtn.title =
                'Delete Mode ON — click an element to remove it (or click here again to cancel)';
            document.body.style.cursor = 'crosshair';

        } else {
            this.deleteModeBtn.classList.remove('active');
            this.deleteModeBtn.title = 'Delete Element';
            document.body.style.cursor = '';
        }
    }

    private handleExportSvg(): void {
        this.actionDispatcher.dispatch(RequestExportSvgAction.create());
    }
}