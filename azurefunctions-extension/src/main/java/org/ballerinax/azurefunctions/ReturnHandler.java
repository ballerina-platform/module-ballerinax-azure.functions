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

import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

/**
 * Represents an Azure function return value handler.
 */
public interface ReturnHandler {

    /**
     * Initializes the {@link ReturnHandler}. This can be used for any
     * initialization operations before the invocationVariable call is made.
     * 
     * @param context The handler context
     * 
     * @throws AzureFunctionsException thrown if an error occurs
     */
    public void init(FunctionDeploymentContext context) throws AzureFunctionsException;

    /**
     * Called after the function invocation statement is done, and the return value is passed here 
     * as an expression to be use for further statement creation to populate the JSON result. 
     * 
     * @param returnValueExpr The function invocation return value expression
     * 
     * @throws AzureFunctionsException thrown if an error occurs
     */
    public void postInvocationProcess(BLangExpression returnValueExpr) throws AzureFunctionsException;

}
