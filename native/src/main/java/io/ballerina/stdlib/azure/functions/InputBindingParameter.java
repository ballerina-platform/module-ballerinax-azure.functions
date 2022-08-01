package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;

/**
 * Represents an Input Binding Parameter.
 * 
 * @since 2.0.0
 */
public class InputBindingParameter extends AZFParameter {
    private Object value;

    public InputBindingParameter(int index, Parameter parameter, Object value) {
        super(index, parameter);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }
}