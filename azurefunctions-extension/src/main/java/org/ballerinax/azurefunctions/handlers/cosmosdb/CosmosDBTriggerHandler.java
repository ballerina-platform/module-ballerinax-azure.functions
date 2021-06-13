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
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Utils;
import org.ballerinax.azurefunctions.handlers.AbstractParameterHandler;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the input parameter handler annotation "@CosmosDBTrigger".
 */
public class CosmosDBTriggerHandler extends AbstractParameterHandler {

    public CosmosDBTriggerHandler(BLangSimpleVariable param, BLangAnnotationAttachment annotation) {
        super(param, annotation, BindingType.TRIGGER);
    }

    @Override
    public BLangExpression invocationProcess() throws AzureFunctionsException {
        if (Utils.isJsonType(this.ctx.globalCtx, this.param.getBType())) {
            return Utils.createAzurePkgInvocationNode(this.ctx, "getJsonFromInputData",
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                    Utils.createStringLiteral(ctx.globalCtx, this.name));
        } else if (Utils.isRecordArrayType(this.ctx.globalCtx, this.param.getBType())) {
            return Utils.createAzurePkgInvocationNode(this.ctx, "getBallerinaValueFromInputData",
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                    Utils.createStringLiteral(ctx.globalCtx, this.name),
                    Utils.createTypeDescExpr(ctx.globalCtx, this.param.getBType()));
        } else {
            throw this.createError("Type must be 'json' or a record array type");
        }
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException { }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        Map<String, Object> annonMap = Utils.extractAnnotationKeyValues(this.annotation);
        binding.put("type", "cosmosDBTrigger");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        binding.put("leaseConnectionStringSetting", annonMap.get("leaseConnectionStringSetting"));
        binding.put("leaseDatabaseName", annonMap.get("leaseDatabaseName"));
        binding.put("leaseCollectionName", annonMap.get("leaseCollectionName"));
        Boolean createLeaseCollectionIfNotExists = (Boolean) annonMap.get("createLeaseCollectionIfNotExists");
        if (createLeaseCollectionIfNotExists == null) {
            createLeaseCollectionIfNotExists = Constants.DEFAULT_COSMOS_DB_CREATELEASECOLLECTIONIFNOTEXISTS;
        }
        binding.put("createLeaseCollectionIfNotExists", createLeaseCollectionIfNotExists);
        binding.put("leasesCollectionThroughput", annonMap.get("leasesCollectionThroughput"));
        binding.put("leaseCollectionPrefix", annonMap.get("leaseCollectionPrefix"));
        binding.put("feedPollDelay", annonMap.get("feedPollDelay"));
        binding.put("leaseAcquireInterval", annonMap.get("leaseAcquireInterval"));
        binding.put("leaseExpirationInterval", annonMap.get("leaseExpirationInterval"));
        binding.put("leaseRenewInterval", annonMap.get("leaseRenewInterval"));
        binding.put("checkpointFrequency", annonMap.get("checkpointFrequency"));
        binding.put("maxItemsPerInvocation", annonMap.get("maxItemsPerInvocation"));
        binding.put("startFromBeginning", annonMap.get("startFromBeginning"));
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        return binding;
    }
    
}
