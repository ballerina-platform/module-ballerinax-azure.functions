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
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Utils;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.Map;

/**
 * Implementation for the input parameter handler annotation "@HTTPTrigger".
 */
public class HTTPTriggerParameterHandler extends AbstractParameterHandler {

    public HTTPTriggerParameterHandler(BLangSimpleVariable param, BLangAnnotationAttachment annotation) {
        super(param, annotation, BindingType.TRIGGER);
    }
    
    @Override
    public BLangExpression invocationProcess() throws AzureFunctionsException {
        boolean httpRequestType = Utils.isHTTPRequestType(this.param.type);
        if (Utils.isSingleInputBinding(this.ctx)) {
            if (httpRequestType) {
                return Utils.createAzurePkgInvocationNode(this.ctx, "getHTTPRequestFromParams",
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams));
            } else if (Utils.isStringType(this.ctx.globalCtx, this.param.type)) {
                return Utils.createAzurePkgInvocationNode(this.ctx, "getStringFromHTTPReq",
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams));
            } else if (Utils.isJsonType(this.ctx.globalCtx, this.param.type)) {
                return Utils.createAzurePkgInvocationNode(this.ctx, "getJsonFromHTTPReq",
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams));
            } else if (Utils.isByteArray(this.ctx.globalCtx, this.param.type)) {
                return Utils.createAzurePkgInvocationNode(this.ctx, "getBinaryFromHTTPReq",
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams));
            } else {
                throw this.createError("Type '" + this.param.type.tsymbol.name.value + "' is not supported");
            }
        } else {
            if (httpRequestType) {
                throw this.createError(
                        "In a multiple input binding scenario, the parameter type cannot be '" 
                                + Constants.BALLERINA_ORG + "/" + Constants.HTTP_MODULE_NAME 
                                + ":" + Constants.HTTP_REQUEST_NAME);
            } else if (Utils.isStringType(this.ctx.globalCtx, this.param.type)) {
                return Utils.createAzurePkgInvocationNode(this.ctx, "getStringFromInputData",
                        Utils.createStringLiteral(ctx.globalCtx, this.name),
                        Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams));
            } else {
                throw this.createError("Type '" + this.param.type.tsymbol.name.value + "' is not supported");
            }
        }
    }

    @Override
    public void postInvocationProcess() { }

    @Override
    public Map<String, Object> generateBinding() {
        return null;
    }
    
}
