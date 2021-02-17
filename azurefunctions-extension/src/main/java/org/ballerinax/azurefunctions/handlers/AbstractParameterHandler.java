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
package org.ballerinax.azurefunctions.handlers;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.FunctionDeploymentContext;
import org.ballerinax.azurefunctions.ParameterHandler;
import org.ballerinax.azurefunctions.STUtil;

import java.util.Map;

/**
 * Abstract class with common operations implemented for {@link ParameterHandler}.
 */
public abstract class AbstractParameterHandler implements ParameterHandler {

    protected FunctionDeploymentContext ctx;

    protected RequiredParameterNode param;

    protected String name;

    protected BindingType bindingType;

    protected ParameterSymbol variableSymbol;

    public AbstractParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param,
                                    BindingType bindingType) {
        this.variableSymbol = variableSymbol;
        this.param = param;
        this.name = this.param.paramName().get().text();
        this.bindingType = bindingType;
    }

    public void init(FunctionDeploymentContext ctx) throws AzureFunctionsException {
        this.ctx = ctx;
        this.processBinding();
    }

    private String extractBindingDirection() {
        if (BindingType.INPUT.equals(this.bindingType) || BindingType.TRIGGER.equals(this.bindingType)) {
            return "in";
        } else {
            return "out";
        }
    }

    private void processBinding() throws AzureFunctionsException {
        Map<String, Object> binding = this.generateBinding();
        if (binding == null) {
            return;
        }
        binding.put("direction", this.extractBindingDirection());
        binding.put("name", this.name);
        STUtil.addFunctionBinding(this.ctx, binding);
    }

    public BindingType getBindingType() {
        return bindingType;
    }

    public abstract Map<String, Object> generateBinding() throws AzureFunctionsException;

}
