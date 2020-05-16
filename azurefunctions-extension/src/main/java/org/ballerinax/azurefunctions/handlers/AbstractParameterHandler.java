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

import org.ballerinalang.model.AnnotationAttachment;
import org.ballerinalang.model.VariableDef;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.FunctionDeploymentContext;
import org.ballerinax.azurefunctions.ParameterHandler;

/**
 * Abstract class with common operations implemented for {@link ParameterHandler}.
 */
public abstract class AbstractParameterHandler implements ParameterHandler {
    
    protected FunctionDeploymentContext ctx;

    protected AnnotationAttachment annotations;

    protected VariableDef param;

    protected String name;

    public void init(FunctionDeploymentContext ctx, AnnotationAttachment annotations, VariableDef param)
            throws AzureFunctionsException {
        this.ctx = ctx;
        this.annotations = annotations;
        this.param = param;
        //this.name = param.getName();
        this.name = "x";
    }

}
