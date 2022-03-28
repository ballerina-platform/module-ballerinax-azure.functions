package org.ballerinax.azurefunctions.service;

import org.ballerinax.azurefunctions.Constants;

/**
 * Represents an Input Binding in Azure Functions.
 * 
 * @since 2.0.0
 */
public abstract class InputBinding extends Binding {

    public InputBinding(String triggerType) {
        super(triggerType, Constants.DIRECTION_IN);
    }
}
