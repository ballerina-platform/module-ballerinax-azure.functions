package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;

/**
 * Represents a query parameter in a resource function.
 *
 * @since 2.0.0
 */
public class QueryParameter extends AZFParameter {

    private Object value;

    public QueryParameter(int index, Parameter parameter, Object value) {
        super(index, parameter);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return this.value;
    }
}
