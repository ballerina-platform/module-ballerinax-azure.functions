package org.ballerinax.azurefunctions;

import org.ballerinax.azurefunctions.service.Binding;

import java.util.List;

/**
 * Represents a the function.json structure.
 * 
 * @since 2.0.0
 */
public class FunctionContext {
    private String functionName;
    private List<Binding> bindingList;

    public FunctionContext(String functionName, List<Binding> bindingList) {
        this.functionName = functionName;
        this.bindingList = bindingList;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Binding> getBindingList() {
        return bindingList;
    }
}
