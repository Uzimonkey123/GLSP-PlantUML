import { IDiagramStartup, IGridManager } from '@eclipse-glsp/client';
import { MaybePromise, TYPES } from '@eclipse-glsp/sprotty';
import { inject, injectable, optional } from 'inversify';

@injectable()
export class PlantUmlStartup implements IDiagramStartup {
    rank = -1;

    @inject(TYPES.IGridManager) @optional() protected gridManager?: IGridManager;

    preRequestModel(): MaybePromise<void> {
        this.gridManager?.setGridVisible(true);
    }
}