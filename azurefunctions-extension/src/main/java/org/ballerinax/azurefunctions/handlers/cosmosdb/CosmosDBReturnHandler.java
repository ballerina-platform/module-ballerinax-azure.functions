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
package org.ballerinax.azurefunctions.handlers.cosmosdb;

import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.Utils;
import org.ballerinax.azurefunctions.handlers.AbstractReturnHandler;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the return handler annotation "@CosmosDBOutput".
 */
public class CosmosDBReturnHandler extends AbstractReturnHandler {

    private Map<String, String> annonMap;

    public CosmosDBReturnHandler(BType retType, BLangAnnotationAttachment annotation) {
        super(retType, annotation);
        this.annonMap = Utils.extractAnnotationKeyValues(this.annotation);
    }

    @Override
    public void postInvocationProcess(BLangExpression returnValueExpr) throws AzureFunctionsException {
        if (Utils.isJsonType(this.ctx.globalCtx, this.retType)) {
            Utils.addAzurePkgFunctionCall(this.ctx, "setCosmosDBJsonReturn", true,
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams), returnValueExpr,
                    Utils.createStringLiteral(this.ctx.globalCtx, pk()));
        } else if (Utils.isRecordType(this.ctx.globalCtx, this.retType)) {
            Utils.addAzurePkgFunctionCall(this.ctx, "setCosmosDBBallerinaValueAsJsonReturn", true,
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams), returnValueExpr,
                    Utils.createStringLiteral(this.ctx.globalCtx, pk()));
        } else if (Utils.isRecordArrayType(this.ctx.globalCtx, this.retType)) {
            Utils.addAzurePkgFunctionCall(this.ctx, "setCosmosDBBallerinaValueAsJsonReturn", true,
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams), returnValueExpr,
                    Utils.createStringLiteral(this.ctx.globalCtx, pk()));
        } else {            
            throw this.createError("Type '" + this.retType.tsymbol.name.value + "' is not supported");
        }
    }

    private String pk() {
        String pk = annonMap.get("partitionKey");
        if (pk == null) {
            pk = "";
        }
        return pk;
    }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("type", "cosmosDB");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        String createIfNotExists = annonMap.get("createIfNotExists");
        if (createIfNotExists != null) {
            binding.put("createIfNotExists", Boolean.parseBoolean(createIfNotExists));
        }
        binding.put("partitionKey", annonMap.get("partitionKey"));
        String collectionThroughput = annonMap.get("collectionThroughput");
        if (collectionThroughput != null) {
            binding.put("collectionThroughput", Integer.parseInt(collectionThroughput));
        }
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        String useMultipleWriteLocations = annonMap.get("useMultipleWriteLocations");
        if (useMultipleWriteLocations != null) {
            binding.put("useMultipleWriteLocations", Boolean.parseBoolean(useMultipleWriteLocations));
        }
        return binding;
    }
    
}
