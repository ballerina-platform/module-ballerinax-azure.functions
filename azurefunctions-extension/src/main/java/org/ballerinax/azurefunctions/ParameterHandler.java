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

import org.ballerinalang.core.model.expressions.Expression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

/**
 * Represents an Azure function parameter handler.
 */
public interface ParameterHandler {

    /**
     * Initializes the {@link ParameterHandler}. This can be used for any initialization operations before
     * the preInvocationProcess call is made.
     *
     * @param context The handler context
     * @throws AzureFunctionsException thrown if an error occurs
     */
    void init(FunctionDeploymentContext context) throws AzureFunctionsException;

    /**
     * Called when generating the azure function invocation statement. This will be used for scenarios
     * such as generating the parameter values for the function invocation by consuming the incoming HTTP
     * data, which can be accessed using the context. After any required statement are generated, this must
     * return an {@link Expression} which is used for the parameter value of the azure function call.
     *
     * @return The expression which represents the parameter value
     * @throws AzureFunctionsException thrown if an error occurs
     */
    BLangExpression invocationProcess() throws AzureFunctionsException;

    /**
     * Called after the function call statement is generated. This can be used for scenarios like processing
     * any output bindings, where we need to extra data from the parameter and populate the output JSON value
     * that is referenced using the context instance.
     */
    void postInvocationProcess() throws AzureFunctionsException;

    /**
     * Retreives the binding type.
     *
     * @return The binding type
     */
    BindingType getBindingType();

}
