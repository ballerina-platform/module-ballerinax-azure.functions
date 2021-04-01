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

import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.FunctionDeploymentContext;
import org.ballerinax.azurefunctions.ReturnHandler;
import org.ballerinax.azurefunctions.STUtil;

import java.util.Map;

/**
 * Abstract class with common operations implemented for {@link ReturnHandler}.
 */
public abstract class AbstractReturnHandler implements ReturnHandler {
    
    protected FunctionDeploymentContext ctx;

    protected TypeSymbol retType;

    protected AnnotationNode annotation;
    
    public AbstractReturnHandler(TypeSymbol retType, AnnotationNode annotation) {
        this.retType = retType;
        this.annotation = annotation;
    }

    public void init(FunctionDeploymentContext ctx) throws AzureFunctionsException {
        this.ctx = ctx;
        this.processBinding();
    }

    private void processBinding() throws AzureFunctionsException {
        Map<String, Object> binding = this.generateBinding();
        if (binding == null) {
            return;
        }
        binding.put("direction", "out");
        binding.put("name", "$return");
        STUtil.addFunctionBinding(this.ctx, binding);
    }

    public abstract Map<String, Object> generateBinding() throws AzureFunctionsException;

    public AnnotationNode getAnnotation() {
        return annotation;
    }

}
