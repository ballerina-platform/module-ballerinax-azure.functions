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
import org.ballerinax.azurefunctions.Utils;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the return handler annotation "@HTTPOutput".
 */
public class HTTPReturnHandler extends AbstractReturnHandler {

    public HTTPReturnHandler(BType retType, BLangAnnotationAttachment annotation) {
        super(retType, annotation);
    }

    @Override
    public void postInvocationProcess(BLangExpression returnValueExpr) throws AzureFunctionsException {
        if (Utils.isStringType(this.ctx.globalCtx, this.retType)) {
            if (Utils.isSingleOutputBinding(this.ctx)) {
                Utils.addAzurePkgFunctionCall(this.ctx, "setStringReturn", true,
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams), returnValueExpr);
            } else {
                Utils.addAzurePkgFunctionCall(this.ctx, "setPureStringOutput", true,
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams), returnValueExpr);
            }
        } else {
            throw this.createError("Type '" + this.retType.tsymbol.name.value + "' is not supported");
        }
    }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("type", "http");
        return binding;
    }
    
}
