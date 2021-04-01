/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.azurefunctions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Azure functions context.
 */
public class FunctionDeploymentContext {

    private static final String VAR_PREFIX = "v";

    private List<ParameterHandler> parameterHandlers = new ArrayList<>();

    private ReturnHandler returnHandler;

    private JsonObject functionDefinition;

    private FunctionDefinitionNode sourceFunction;

    private FunctionDefinitionNode function;

    private int varCounter = 0;

    public FunctionDeploymentContext() {
        this.setFunctionDefinition(new JsonObject());
        this.getFunctionDefinition().add(Constants.FUNCTION_BINDINGS_NAME, new JsonArray());
    }

    public String getNextVarName() {
        return VAR_PREFIX + (++varCounter);
    }

    public List<ParameterHandler> getParameterHandlers() {
        return parameterHandlers;
    }

    public void setParameterHandlers(List<ParameterHandler> parameterHandlers) {
        this.parameterHandlers = parameterHandlers;
    }

    public ReturnHandler getReturnHandler() {
        return returnHandler;
    }

    public void setReturnHandler(ReturnHandler returnHandler) {
        this.returnHandler = returnHandler;
    }

    public JsonObject getFunctionDefinition() {
        return functionDefinition;
    }

    public void setFunctionDefinition(JsonObject functionDefinition) {
        this.functionDefinition = functionDefinition;
    }

    public FunctionDefinitionNode getSourceFunction() {
        return sourceFunction;
    }

    public void setSourceFunction(FunctionDefinitionNode sourceFunction) {
        this.sourceFunction = sourceFunction;
    }

    public FunctionDefinitionNode getFunction() {
        return function;
    }

    public void setFunction(FunctionDefinitionNode function) {
        this.function = function;
    }
}
