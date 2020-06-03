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

import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.FunctionDeploymentContext;
import org.ballerinax.azurefunctions.ParameterHandler;
import org.ballerinax.azurefunctions.Utils;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

import java.util.Map;

/**
 * Abstract class with common operations implemented for {@link ParameterHandler}.
 */
public abstract class AbstractParameterHandler implements ParameterHandler {
    
    protected FunctionDeploymentContext ctx;

    protected BLangSimpleVariable param;

    protected BLangAnnotationAttachment annotation;

    protected String name;

    protected BindingType bindingType;

    public AbstractParameterHandler(BLangSimpleVariable param, BLangAnnotationAttachment annotation,
            BindingType bindingType) {
        this.param = param;
        this.annotation = annotation;
        this.name = this.param.name.value;
        this.bindingType = bindingType;
    }

    public void init(FunctionDeploymentContext ctx) {
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

    private void processBinding() {
        Map<String, Object> binding = this.generateBinding();
        if (binding == null) {
            return;
        }
        binding.put("direction", this.extractBindingDirection());
        binding.put("name", this.name);
        Utils.addFunctionBinding(this.ctx, binding);
    }

    public BindingType getBindingType() {
        return bindingType;
    }

    public AzureFunctionsException createError(String msg) {
        return new AzureFunctionsException("Error at function: '" + ctx.sourceFunction.name.value + "' parameter: '"
                + param.name.value + "' - " + msg);
    }

    public abstract Map<String, Object> generateBinding();

}
