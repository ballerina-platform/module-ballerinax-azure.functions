package org.ballerinax.azurefunctions.service;

import org.ballerinax.azurefunctions.Constants;

/**
 * Represents an Output Binding in Azure Functions.
 *
 * @since 2.0.0
 */
public abstract class OutputBinding extends Binding {
    public OutputBinding(String triggerType) {
        super(triggerType, Constants.DIRECTION_OUT);
    }
}
