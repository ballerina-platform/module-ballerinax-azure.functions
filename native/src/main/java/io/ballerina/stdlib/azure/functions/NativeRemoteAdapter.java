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
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.RemoteMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeId;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.builder.BinaryPayloadBuilder;
import io.ballerina.stdlib.azure.functions.builder.JsonPayloadBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.azure.functions.Constants.SERVICE_OBJECT;

/**
 * {@code NativeRemoteAdapter} is a wrapper object used for service method execution.
 */
public class NativeRemoteAdapter {

    public static void externRemoteInit(BObject adaptor, BObject serviceObj) {
        adaptor.addNativeData(SERVICE_OBJECT, serviceObj);
    }

    public static Object callRemoteFunction(Environment env, BObject adaptor, BMap<?, ?> body, BString remoteFuncName) {
        BObject bHubService = (BObject) adaptor.getNativeData(SERVICE_OBJECT);
        return invokeRemoteFunction(env, bHubService, "callRemoteFunction", body, remoteFuncName);
    }

    private static Object invokeRemoteFunction(Environment env, BObject bHubService, String parentFunctionName,
                                               BMap<?, ?> body, BString remoteFuncName) {
        BMap<?, ?> data = body.getMapValue(StringUtils.fromString("Data"));
        Future balFuture = env.markAsync();
        Module module = ModuleUtils.getModule();
        StrandMetadata metadata = new StrandMetadata(module.getOrg(), module.getName(), module.getVersion(),
                parentFunctionName);
        ServiceType serviceType = (ServiceType) bHubService.getType();
        List<Object> argList = new ArrayList<>();
        RemoteMethodType methodType = getRemoteMethod(serviceType, remoteFuncName).orElseThrow(); //TODO handle error
        Parameter[] parameters = methodType.getParameters();
        for (Parameter parameter : parameters) {
            String name = parameter.name;
            Object annotation = methodType.getAnnotation(StringUtils.fromString("$param$." + name));
            //TODO check and process Payload variable
            if (ParamHandler.isPayloadAnnotationParam(annotation)) {
                Object bValue = getDataboundValue(data, parameter, serviceType);
                argList.add(bValue);
                argList.add(true);
                continue;
            }
            
            if (ParamHandler.isBindingNameParam(annotation)) {
                BString nameParam =
                        body.getMapValue(StringUtils.fromString("Metadata")).getStringValue(StringUtils.fromString(
                                "name"));
                Type type = parameter.type;
                JsonPayloadBuilder jsonPayloadBuilder = new JsonPayloadBuilder(type);
                Object bValue = jsonPayloadBuilder.getValue(nameParam, false);
                argList.add(bValue);
                argList.add(true);
                continue;
            }
            

            //TODO check and process input binding variable
            if (ParamHandler.isInputAnnotationParam(annotation)) {
                BString bodyValue = data.getStringValue(StringUtils.fromString(name));
                Type type = parameter.type;
                JsonPayloadBuilder jsonPayloadBuilder = new JsonPayloadBuilder(type);
                Object bValue = jsonPayloadBuilder.getValue(bodyValue, false);
                argList.add(bValue);
                argList.add(true);
                continue;
            }
        }
        Object[] args = argList.toArray();
        BMap<?, ?> annotation = (BMap<?, ?>) methodType.getAnnotation(StringUtils.fromString("$returns$"));
        if (serviceType.isIsolated()) {
            env.getRuntime().invokeMethodAsyncConcurrently(
                    bHubService, remoteFuncName.getValue(), null, metadata,
                    new FunctionCallback(balFuture, module, annotation.getKeys()), null, PredefinedTypes.TYPE_NULL,
                    args);
        } else {
            env.getRuntime().invokeMethodAsyncSequentially(
                    bHubService, remoteFuncName.getValue(), null, metadata,
                    new FunctionCallback(balFuture, module, annotation.getKeys()), null, PredefinedTypes.TYPE_NULL,
                    args);
        }
        return null;
    }

    private static Object getDataboundValue(BMap<?, ?> body, Parameter parameter, ServiceType serviceType) {
        List<TypeId> ids = serviceType.getTypeIdSet().getIds();
        BString paramBString = StringUtils.fromString(parameter.name);
        if (ids.size() >= 1) {
            TypeId typeId = ids.get(0);
            String name = typeId.getName();
            if (name.equals("TimerService")) {
                return body.getMapValue(paramBString);
            } else if (name.equals("BlobService")) {
                BString bStr = body.getStringValue(paramBString);
                BinaryPayloadBuilder binaryPayloadBuilder = new BinaryPayloadBuilder(parameter.type);
                return binaryPayloadBuilder.getValue(bStr, false);
            }
        }
        BString bStr = body.getStringValue(paramBString);
        JsonPayloadBuilder jsonPayloadBuilder = new JsonPayloadBuilder(parameter.type);
        return jsonPayloadBuilder.getValue(bStr, false);
    }

    private static Optional<RemoteMethodType> getRemoteMethod(ServiceType serviceType, BString remoteFuncName) {
        RemoteMethodType[] remoteMethods = serviceType.getRemoteMethods();
        for (RemoteMethodType methodType : remoteMethods) {
            if (methodType.getName().equals(remoteFuncName.getValue())) {
                return Optional.of(methodType);
            }
        }
        return Optional.empty();
    }
}
