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

import org.ballerinax.azurefunctions.FunctionDeploymentContext;
import org.ballerinax.azurefunctions.ParameterHandler;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

/**
 * Abstract class with common operations implemented for {@link ParameterHandler}.
 */
public abstract class AbstractParameterHandler implements ParameterHandler {
    
    protected FunctionDeploymentContext ctx;

    protected BLangSimpleVariable param;

    protected BLangAnnotationAttachment annotation;

    protected String name;

    public AbstractParameterHandler(BLangSimpleVariable param, BLangAnnotationAttachment annotation) {
        this.param = param;
        this.annotation = annotation;
        this.name = this.param.name.value;
    }

    public void init(FunctionDeploymentContext ctx) {
        this.ctx = ctx;        
    }

}
