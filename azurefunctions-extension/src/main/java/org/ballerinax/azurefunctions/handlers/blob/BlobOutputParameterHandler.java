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
package org.ballerinax.azurefunctions.handlers.blob;

import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Utils;
import org.ballerinax.azurefunctions.handlers.AbstractParameterHandler;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the output parameter handler annotation "@BlobOutput".
 */
public class BlobOutputParameterHandler extends AbstractParameterHandler {

    private BVarSymbol var;

    public BlobOutputParameterHandler(BLangSimpleVariable param, BLangAnnotationAttachment annotation) {
        super(param, annotation, BindingType.OUTPUT);
    }

    @Override
    public BLangExpression invocationProcess() throws AzureFunctionsException {
        if (Utils.isAzurePkgType(ctx, "BytesOutputBinding", this.param.type)) {
             this.var = Utils.addAzurePkgRecordVarDef(this.ctx, "BytesOutputBinding", this.ctx.getNextVarName());
        } else if (Utils.isAzurePkgType(ctx, "StringOutputBinding", this.param.type)) {
            this.var = Utils.addAzurePkgRecordVarDef(this.ctx, "StringOutputBinding", this.ctx.getNextVarName());
        } else {
            throw this.createError("Type must be 'BytesOutputBinding' or 'StringOutputBinding'");
        }
        return Utils.createVariableRef(this.ctx.globalCtx, this.var);
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException {
        Utils.addAzurePkgFunctionCall(this.ctx, "setBlobOutput", true,
                Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                Utils.createStringLiteral(this.ctx.globalCtx, this.name),
                Utils.createVariableRef(this.ctx.globalCtx, this.var));
    }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        Map<String, Object> annonMap = Utils.extractAnnotationKeyValues(this.annotation);
        binding.put("type", "blob");
        binding.put("path", annonMap.get("path"));
        // According to: https://github.com/Azure/azure-functions-host/issues/6091
        binding.put("dataType", "string");
        String connection = (String) annonMap.get("connection");
        if (connection == null) {
            connection = Constants.DEFAULT_STORAGE_CONNECTION_NAME;
        }
        binding.put("connection", connection);
        return binding;

    }
    
}
