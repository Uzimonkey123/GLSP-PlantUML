/*
 * File: IgnoreComputeBoundsHandler.java
 * Author: Norman Babiak
 * Description: ComputedBoundsAction placeholder to keep the errors from moving entities in class diagram
 * Date: 5.5.2026
 */

package com.GLSPPlantUML.handlers;

import com.google.inject.Singleton;
import org.eclipse.glsp.server.actions.AbstractActionHandler;
import org.eclipse.glsp.server.actions.Action;
import org.eclipse.glsp.server.features.core.model.ComputedBoundsAction;

import java.util.List;

@Singleton
public class IgnoreComputeBoundsHandler extends AbstractActionHandler<ComputedBoundsAction> {

    @Override
    protected List<Action> executeAction(ComputedBoundsAction computedBoundsAction) {
        return List.of();
    }

    @Override
    public List<Class<? extends Action>> getHandledActionTypes() {
        return List.of(ComputedBoundsAction.class);
    }
}
