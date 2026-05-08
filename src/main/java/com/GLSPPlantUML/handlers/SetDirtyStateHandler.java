/*
 * File: SetDirtyStateHandler.java
 * Author: Norman Babiak
 * Description: Setting dirty state after change of file, using the SetDirtyStateAction
 * Date: 5.5.2026
 */

package com.GLSPPlantUML.handlers;

import org.eclipse.glsp.server.actions.AbstractActionHandler;
import org.eclipse.glsp.server.actions.Action;
import org.eclipse.glsp.server.actions.SetDirtyStateAction;

import java.util.List;

public class SetDirtyStateHandler extends AbstractActionHandler<SetDirtyStateAction> {

    @Override
    protected List<Action> executeAction(SetDirtyStateAction setDirtyStateAction) {
        return List.of();
    }

    public SetDirtyStateHandler() {
        super();
    }
}
