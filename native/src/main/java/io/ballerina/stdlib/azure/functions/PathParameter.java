package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.utils.StringUtils;

/**
 * Represents a path paramter in azure resource function.
 * 
 * @since 2.0.0
 */
public class PathParameter extends AZFParameter {
    private  String value;
    public PathParameter(int index, Parameter parameter, String value) {
        super(index, parameter);
        this.value = value;
    }

    @Override
    public Object getValue() {
        return StringUtils.fromString(this.value);
    }
}
