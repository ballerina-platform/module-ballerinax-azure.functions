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
        if (Utils.isJsonType(this.ctx.globalCtx, this.param.type)) {
            return Utils.createAzurePkgInvocationNode(this.ctx, "getJsonFromInputData",
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                    Utils.createStringLiteral(ctx.globalCtx, this.name));
        } else if (Utils.isRecordArrayType(this.ctx.globalCtx, this.param.type)) {
            return Utils.createAzurePkgInvocationNode(this.ctx, "getBallerinaValueFromInputData",
                    Utils.createVariableRef(ctx.globalCtx, ctx.handlerParams),
                    Utils.createStringLiteral(ctx.globalCtx, this.name),
                    Utils.createTypeDescExpr(ctx.globalCtx, this.param.type));
        } else {
            throw this.createError("Type must be 'json' or a record array type");
        }
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException { }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        Map<String, String> annonMap = Utils.extractAnnotationKeyValues(this.annotation);
        binding.put("type", "cosmosDBTrigger");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        binding.put("leaseConnectionStringSetting", annonMap.get("leaseConnectionStringSetting"));
        binding.put("leaseDatabaseName", annonMap.get("leaseDatabaseName"));
        binding.put("leaseCollectionName", annonMap.get("leaseCollectionName"));
        String createLeaseCollectionIfNotExistsStr = annonMap.get("createLeaseCollectionIfNotExists");
        Boolean createLeaseCollectionIfNotExists;
        if (createLeaseCollectionIfNotExistsStr != null) {
            createLeaseCollectionIfNotExists = Boolean.valueOf(createLeaseCollectionIfNotExistsStr);
        } else {
            createLeaseCollectionIfNotExists = Constants.DEFAULT_COSMOS_DB_CREATELEASECOLLECTIONIFNOTEXISTS;
        }
        binding.put("createLeaseCollectionIfNotExists", createLeaseCollectionIfNotExists);
        String leasesCollectionThroughput = annonMap.get("leasesCollectionThroughput");
        if (leasesCollectionThroughput != null) {
            binding.put("leasesCollectionThroughput", Integer.parseInt(leasesCollectionThroughput));
        }
        binding.put("leaseCollectionPrefix", annonMap.get("leaseCollectionPrefix"));
        String feedPollDelay = annonMap.get("feedPollDelay");
        if (feedPollDelay != null) {
            binding.put("feedPollDelay", Integer.parseInt(feedPollDelay));
        }
        String leaseAcquireInterval = annonMap.get("leaseAcquireInterval");
        if (leaseAcquireInterval != null) {
            binding.put("leaseAcquireInterval", Integer.parseInt(leaseAcquireInterval));
        }
        String leaseExpirationInterval = annonMap.get("leaseExpirationInterval");
        if (leaseExpirationInterval != null) {
            binding.put("leaseExpirationInterval", Integer.parseInt(leaseExpirationInterval));
        }
        String leaseRenewInterval = annonMap.get("leaseRenewInterval");
        if (leaseRenewInterval != null) {
            binding.put("leaseRenewInterval", Integer.parseInt(leaseRenewInterval));
        }
        String checkpointFrequency = annonMap.get("checkpointFrequency");
        if (checkpointFrequency != null) {
            binding.put("checkpointFrequency", Integer.parseInt(checkpointFrequency));
        }
        String maxItemsPerInvocation = annonMap.get("maxItemsPerInvocation");
        if (maxItemsPerInvocation != null) {
            binding.put("maxItemsPerInvocation", Integer.parseInt(maxItemsPerInvocation));
        }
        String startFromBeginning = annonMap.get("startFromBeginning");
        if (startFromBeginning != null) {
            binding.put("startFromBeginning", Boolean.parseBoolean(startFromBeginning));
        }
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        return binding;
    }
    
}
