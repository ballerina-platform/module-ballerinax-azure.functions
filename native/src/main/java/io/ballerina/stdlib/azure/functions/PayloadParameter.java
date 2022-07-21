package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;

/**
 * Represents the payload parameter in azure functions.
 * 
 * @since 2.0.0
 */
public class PayloadParameter extends AZFParameter {

    private Object value;

    public PayloadParameter(int index, Parameter parameter, Object value) {
        super(index, parameter);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
