/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.exceptions.BadRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.stdlib.azure.functions.Constants.SERVICE_OBJECT;

/**
 * {@code NativeHttpToAzureAdaptor} is a wrapper object used for service method execution.
 */
public class NativeHttpToAzureAdaptor {

    public static void externInit(BObject adaptor, BObject serviceObj) {
        adaptor.addNativeData(SERVICE_OBJECT, serviceObj);
    }

    public static BArray getAzureFunctionNames(Environment env, BObject adaptor) {
        BObject bHubService = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        ServiceType svcType = (ServiceType) bHubService.getType();
        List<BString> functionNameList = new ArrayList<>();
        for (ResourceMethodType resourceMethod : svcType.getResourceMethods()) {
            BString functionName = ((BMap<?, ?>) resourceMethod
                    .getAnnotation(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_COMPLETE)))
                    .getStringValue(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_NAME_FIELD));
            functionNameList.add(functionName);
        }
        return ValueCreator.createArrayValue(functionNameList.toArray(BString[]::new));
    }

    public static Object callNativeMethod(Environment env, BObject adaptor, BMap<?, ?> body, BString functionName) {
        BObject bHubService = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeResourceFunction(env, bHubService,
                "callNativeMethod", body, functionName);
    }
    
    //Todo See if we can call parent bal method directly and check deprecated usages
    private static Object invokeResourceFunction(Environment env, BObject bHubService, String parentFunctionName,
                                                 BMap<?, ?> body, BString functionName) {
        return env.yieldAndRun(() -> {
            ServiceType serviceType = (ServiceType) bHubService.getType();
            ResourceMethodType[] resourceMethods = serviceType.getResourceMethods();
            Optional<ResourceMethodType> resourceMethodType = getResourceMethodType(resourceMethods, functionName);
            if (resourceMethodType.isEmpty()) {
                return Utils.createError(ModuleUtils.getModule(), "function " + functionName.getValue() +
                        " not found in the " + "code", Constants.FUNCTION_NOT_FOUND_ERROR);
            }
            ResourceMethodType resourceMethod = resourceMethodType.get();
            try {
                BMap<?, ?> serviceAnnotations = serviceType.getAnnotations();
                HttpResource httpResource = new HttpResource(resourceMethod, body, serviceAnnotations);
                Object[] args = httpResource.getArgList();
                CompletableFuture<Object> balFuture = new CompletableFuture<>();
                FunctionCallback functionCallback = new FunctionCallback(balFuture,
                        ModuleUtils.getModule(), resourceMethod);
                boolean isIsolated = serviceType.isIsolated() && resourceMethod.isIsolated();
                try {
                    Object result = env.getRuntime().callMethod(bHubService, resourceMethod.getName(),
                            new StrandMetadata(isIsolated, null), args);
                    functionCallback.notifySuccess(result);
                    return ModuleUtils.getResult(balFuture);
                } catch (BError bError) {
                    functionCallback.notifyFailure(bError);
                    return ModuleUtils.getResult(balFuture);
                }
            } catch (BadRequestException e) {
                return Utils.createError(ModuleUtils.getModule(), e.getMessage(), e.getType());
            }
        });
    }

    private static Optional<ResourceMethodType> getResourceMethodType(ResourceMethodType[] types,
                                                                      BString enteredFunctionName) {
        for (ResourceMethodType type : types) {
            BString functionName =
                    ((BMap<?, ?>) type.getAnnotation(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_COMPLETE)))
                            .getStringValue(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_NAME_FIELD));

            if (functionName.toString().equals(enteredFunctionName.toString())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
