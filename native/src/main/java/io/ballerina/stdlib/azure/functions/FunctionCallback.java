/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * {@code FunctionCallback} used to handle the Azure function service method invocation results.
 */
public class FunctionCallback implements Callback {

    private final Future future;
    private final Module module;
    private final List<String> annotations;

    public FunctionCallback(Future future, Module module, Object[] annotations) {
        this.future = future;
        this.module = module;
        this.annotations = new ArrayList<>();
        for (Object o : annotations) {
            BString annotation = (BString) o;
            String[] split = annotation.getValue().split(":");
            this.annotations.add(split[split.length - 1]);
        }
    }

    @Override
    public void notifySuccess(Object result) {
        if (result instanceof BError) {
            BError error = (BError) result;
            if (!isModuleDefinedError(error)) {
                error.printStackTrace();
            }
            future.complete(result);
            return;
        }
        BMap<BString, Object> mapValue =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
//        if (result instanceof BArray) {
//            BArray result1 = (BArray) result;
//            Object[] values = result1.getValues();
//            for (int i = 0; i < values.length; i++) {
//                Object obj = values[i];
//                String identifier = generateUniqueIdentifier(i);
//                mapValue.put(StringUtils.fromString(identifier), obj);
//            }
//            future.complete(mapValue);
//            return;
//        }
        if (this.annotations.get(0).equals("QueueOutput") || this.annotations.get(0).equals("CosmosDBOutput")) {
            mapValue.put(StringUtils.fromString("outMsg"), result);
        } else if (this.annotations.get(0).equals("HTTPOutput")) {
            if (isHTTPResponse(result)) {
                BObject status = (BObject) (((BMap) result).get(StringUtils.fromString("status")));
                Object statusCode = Long.toString(status.getIntValue(StringUtils.fromString("code")));
                statusCode = StringUtils.fromString((String) statusCode);

                BMap<BString, Object> bodyMap =
                        ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
                bodyMap.put(StringUtils.fromString("statusCode"), statusCode);

                if (((BMap) result).containsKey(StringUtils.fromString("body"))) {
                    Object body = ((BMap) result).get(StringUtils.fromString("body"));
                    bodyMap.put(StringUtils.fromString("body"), body);
                }

                if (((BMap) result).containsKey(StringUtils.fromString("headers"))) {
                    Object headers = ((BMap) result).get(StringUtils.fromString("headers"));
                    if (!((BMap) headers).containsKey(StringUtils.fromString("content-type"))) {
                        ((BMap) headers).put(StringUtils.fromString("content-type"),
                                StringUtils.fromString("text/plain"));
                    }
                    bodyMap.put(StringUtils.fromString("headers"), headers);
                } else {
                    Object headers =
                            ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
                    ((BMap) headers).put(StringUtils.fromString("content-type"),
                            StringUtils.fromString("text/plain"));
                    bodyMap.put(StringUtils.fromString("headers"), headers);

                }

                if (((BMap) result).containsKey(StringUtils.fromString("mediaType"))) {
                    Object headers = ((BMap) result).get(StringUtils.fromString("headers"));
                    Object mediaType = ((BMap) result).get(StringUtils.fromString("mediaType"));
                    ((BMap) headers).put(StringUtils.fromString("content-type"), mediaType);
                }
                mapValue.put(StringUtils.fromString("resp"), bodyMap);

            } else {
                BMap<BString, Object> respMap =
                        ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
                respMap.put(StringUtils.fromString("body"), result);
                mapValue.put(StringUtils.fromString("resp"), respMap);
            }
        } else {
            BMap<BString, Object> respMap =
                    ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
            respMap.put(StringUtils.fromString("body"), result);
            mapValue.put(StringUtils.fromString("resp"), respMap);
        }
        future.complete(mapValue);
    }

    @Override
    public void notifyFailure(BError bError) {
        bError.printStackTrace();
        BString errorMessage = fromString("service method invocation failed: " + bError.getErrorMessage());
        BError invocationError = ErrorCreator.createError(module, "ServiceExecutionError",
                errorMessage, bError, null);
        future.complete(invocationError);
    }

    private boolean isModuleDefinedError(BError error) {
        Type errorType = error.getType();
        Module packageDetails = errorType.getPackage();
        String orgName = packageDetails.getOrg();
        String packageName = packageDetails.getName();
        return Constants.PACKAGE_ORG.equals(orgName) && Constants.PACKAGE_NAME.equals(packageName);
    }

    private boolean isHTTPResponse(Object result) {
        return (result instanceof  BMap) && (((BMap) result).containsKey(fromString("status"))) &&
                ((result.getClass()).getPackageName()).contains("ballerinax.azure_functions");
    }
}
