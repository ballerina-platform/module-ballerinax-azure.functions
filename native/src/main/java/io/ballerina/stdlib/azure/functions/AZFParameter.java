package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;

/**
 * Represents an parameter of resource/remote function. 
 * 
 * @since 2.0.0
 */
public abstract class AZFParameter implements Comparable<AZFParameter> {
    private int index;
    private Parameter parameter;

    public AZFParameter(int index, Parameter parameter) {
        this.index = index;
        this.parameter = parameter;
    }
    
    public int getIndex() {
        return index;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public int compareTo(AZFParameter o) {
        return this.index - o.index;
    }

    public abstract Object getValue();
}
