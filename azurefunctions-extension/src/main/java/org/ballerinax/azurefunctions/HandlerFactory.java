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

import org.ballerinax.azurefunctions.handlers.blob.BlobInputParameterHandler;
import org.ballerinax.azurefunctions.handlers.blob.BlobOutputParameterHandler;
import org.ballerinax.azurefunctions.handlers.blob.BlobTriggerParameterHandler;
import org.ballerinax.azurefunctions.handlers.context.ContextParameterHandler;
import org.ballerinax.azurefunctions.handlers.cosmosdb.CosmosDBInputParameterHandler;
import org.ballerinax.azurefunctions.handlers.cosmosdb.CosmosDBReturnHandler;
import org.ballerinax.azurefunctions.handlers.cosmosdb.CosmosDBTriggerHandler;
import org.ballerinax.azurefunctions.handlers.http.HTTPOutputParameterHandler;
import org.ballerinax.azurefunctions.handlers.http.HTTPReturnHandler;
import org.ballerinax.azurefunctions.handlers.http.HTTPTriggerParameterHandler;
import org.ballerinax.azurefunctions.handlers.metadata.MetadataBindingParameterHandler;
import org.ballerinax.azurefunctions.handlers.queue.QueueOutputParameterHandler;
import org.ballerinax.azurefunctions.handlers.queue.QueueTriggerHandler;
import org.ballerinax.azurefunctions.handlers.timer.TimerTriggerHandler;
import org.ballerinax.azurefunctions.handlers.twilio.TwilioSmsOutputParameterHandler;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

import java.util.List;

/**
 * Factory class to create parameter and return handlers.
 */
public class HandlerFactory {
    
    public static ParameterHandler createParameterHandler(FunctionDeploymentContext ctx, BLangSimpleVariable param)
            throws AzureFunctionsException {
        if (Utils.isContextType(param.getBType())) {
            return new ContextParameterHandler(param);
        }
        BLangAnnotationAttachment ann = Utils.extractAzureFunctionAnnotation(param.getAnnotationAttachments());
        if (ann == null) {
            throw createParamError(ctx, param, "Invalid annotation or type");
        }
        String name = ann.getAnnotationName().getValue();
        if ("HTTPOutput".equals(name)) {
            return new HTTPOutputParameterHandler(param, ann);
        } else if ("HTTPTrigger".equals(name)) {
            return new HTTPTriggerParameterHandler(param, ann);
        } else if ("QueueOutput".equals(name)) {
            return new QueueOutputParameterHandler(param, ann);
        } else if ("QueueTrigger".equals(name)) {
            return new QueueTriggerHandler(param, ann);
        } else if ("TimerTrigger".equals(name)) {
            return new TimerTriggerHandler(param, ann);
        } else if ("BlobTrigger".equals(name)) {
            return new BlobTriggerParameterHandler(param, ann);
        } else if ("BlobInput".equals(name)) {
            return new BlobInputParameterHandler(param, ann);
        } else if ("BlobOutput".equals(name)) {
            return new BlobOutputParameterHandler(param, ann);
        } else if ("TwilioSmsOutput".equals(name)) {
            return new TwilioSmsOutputParameterHandler(param, ann);
        } else if ("BindingName".equals(name)) {
            return new MetadataBindingParameterHandler(param, ann);
        } else if ("CosmosDBTrigger".equals(name)) {
            return new CosmosDBTriggerHandler(param, ann);
        } else if ("CosmosDBInput".equals(name)) {
            return new CosmosDBInputParameterHandler(param, ann);
        } else {
            throw createParamError(ctx, param, "Parameter handler not found");
        }
    }

    public static ReturnHandler createReturnHandler(FunctionDeploymentContext ctx, BType retType,
            List<BLangAnnotationAttachment> annons) throws AzureFunctionsException {
        SymbolTable symTable = ctx.globalCtx.symTable;
        if (symTable.nilType.equals(retType) || symTable.noType.equals(retType)) {
            return null;
        }
        BLangAnnotationAttachment ann = Utils.extractAzureFunctionAnnotation(annons);
        if (ann == null) {
            throw createReturnError(ctx, "Invalid annotation");
        }
        String name = ann.getAnnotationName().getValue();
        if ("HTTPOutput".equals(name)) {
            return new HTTPReturnHandler(retType, ann);
        } else if ("CosmosDBOutput".equals(name)) {
            return new CosmosDBReturnHandler(retType, ann);
        }  else {
            throw createReturnError(ctx, "Return handler not found for the type: " + retType);
        }
    }

    private static AzureFunctionsException createParamError(FunctionDeploymentContext ctx, BLangSimpleVariable param,
            String msg) {
        return new AzureFunctionsException("Error at function: '" + ctx.sourceFunction.name.value + "' parameter: '"
                + param.name.value + "' - " + msg);
    }

    private static AzureFunctionsException createReturnError(FunctionDeploymentContext ctx, String msg) {
        return new AzureFunctionsException("Error at function: '" 
                + ctx.sourceFunction.name.value + " return - " + msg);
    }

}
