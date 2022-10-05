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
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.exceptions.BadRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            BString functionName = ((BMap) resourceMethod
                    .getAnnotation(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_COMPLETE)))
                    .getStringValue(StringUtils.fromString("name"));
            functionNameList.add(functionName);
        }
        return ValueCreator.createArrayValue(functionNameList.toArray(BString[]::new));
    }

    public static Object callNativeMethod(Environment env, BObject adaptor, BMap body, BString functionName) {
        BObject bHubService = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeResourceFunction(env, bHubService,
                "callNativeMethod", body, functionName);
    }

    private static Object invokeResourceFunction(Environment env, BObject bHubService, String parentFunctionName,
                                                 BMap body, BString functionName) {
        Future balFuture = env.markAsync();
        Module module = ModuleUtils.getModule();
        StrandMetadata metadata = new StrandMetadata(module.getOrg(), module.getName(), module.getVersion(),
                parentFunctionName);
        ServiceType serviceType = (ServiceType) bHubService.getType();

        ResourceMethodType[] resourceMethods = serviceType.getResourceMethods();
        //TODO restrict "httpPayload" from the param names.
        Optional<ResourceMethodType> resourceMethodType = getResourceMethodType(resourceMethods, functionName);
        if (resourceMethodType.isEmpty()) {
            balFuture.complete(Utils.createError(module, "function " + functionName.getValue() + " not found in the " +
                    "code", Constants.FUNCTION_NOT_FOUND_ERROR));
            return null;
        }
        ResourceMethodType resourceMethod = resourceMethodType.get();
        try {
            BMap serviceAnnotations = serviceType.getAnnotations();
            HttpResource httpResource = new HttpResource(resourceMethod, body, serviceAnnotations);
            Object[] args = httpResource.getArgList();
            if (serviceType.isIsolated() && resourceMethod.isIsolated()) {
                env.getRuntime().invokeMethodAsyncConcurrently(
                        bHubService, resourceMethod.getName(), null, metadata,
                        new FunctionCallback(balFuture, module, resourceMethod), null, PredefinedTypes.TYPE_NULL,
                        args);
            } else {
                env.getRuntime().invokeMethodAsyncSequentially(
                        bHubService, resourceMethod.getName(), null, metadata,
                        new FunctionCallback(balFuture, module, resourceMethod), null, PredefinedTypes.TYPE_NULL,
                        args);
            }
        } catch (BadRequestException e) {
            balFuture.complete(Utils.createError(module, e.getMessage(), e.getType()));
        }
        return null;
    }

    private static Optional<ResourceMethodType> getResourceMethodType(ResourceMethodType[] types,
                                                                      BString enteredFunctionName) {
        for (ResourceMethodType type : types) {
            BString functionName =
                    ((BMap) type.getAnnotation(StringUtils.fromString(Constants.FUNCTION_ANNOTATION_COMPLETE)))
                            .getStringValue(StringUtils.fromString("name"));

            if (functionName.toString().equals(enteredFunctionName.toString())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
