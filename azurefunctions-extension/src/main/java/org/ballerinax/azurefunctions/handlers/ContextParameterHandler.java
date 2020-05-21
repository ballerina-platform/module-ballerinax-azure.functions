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
import org.ballerinax.azurefunctions.Utils;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.Map;

/**
 * Implementation for the input parameter handler for the Context object.
 */
public class ContextParameterHandler extends AbstractParameterHandler {

    public ContextParameterHandler(BLangSimpleVariable param) {
        super(param, null, BindingType.CONTEXT);
    }
    
    @Override
    public BLangExpression invocationProcess() throws AzureFunctionsException {
        return Utils.createAzurePkgInvocationNode(this.ctx, "createContext",
                Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                Utils.createBooleanLiteral(this.ctx.globalCtx, !Utils.isPureHTTPBinding(this.ctx)));
    }

    @Override
    public void postInvocationProcess() { }

    @Override
    public Map<String, Object> generateBinding() {
        return null;
    }
    
}
