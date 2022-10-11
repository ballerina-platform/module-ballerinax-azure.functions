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

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.TableType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTable;
import io.ballerina.runtime.api.values.BXmlItem;
import org.ballerinalang.langlib.array.ToBase64;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * {@code FunctionCallback} used to handle the Azure function service method invocation results.
 */
public class FunctionCallback implements Callback {

    private final Future future;
    private final Module module;
    private final List<String> annotations;
    private final MethodType methodType;

    public FunctionCallback(Future future, Module module, MethodType methodType) {
        this.future = future;
        this.module = module;
        this.methodType = methodType;
        this.annotations = new ArrayList<>();
        BMap<BString, ?> annotations =
                (BMap<BString, ?>) methodType.getAnnotation(StringUtils.fromString(Constants.RETURN_ANNOTATION));
        if (annotations != null) {
            for (BString annotation : annotations.getKeys()) {
                String[] split = annotation.getValue().split(":");
                this.annotations.add(split[split.length - 1]);
            }
        }
    }

    private String getOutputAnnotation() {
        if (this.annotations.size() == 0) {
            if (methodType instanceof ResourceMethodType) {
                return Constants.HTTP_OUTPUT;
            }
            //TODO impl compiler ext validations to make sure output annotations exists
        }
        return this.annotations.get(0);
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
        if (result == null) {
            BString statusCode = StringUtils.fromString(Constants.ACCEPTED);
            BMap<BString, Object> respMap =
                    ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
            respMap.put(StringUtils.fromString(Constants.STATUS_CODE), statusCode);
            mapValue.put(StringUtils.fromString(Constants.RESPONSE_FIELD), respMap);
            future.complete(mapValue);
            return;
        }

        String outputBinding = getOutputAnnotation();

        if (Constants.QUEUE_OUTPUT.equals(outputBinding) || Constants.COSMOS_DBOUTPUT.equals(outputBinding)) {
            mapValue.put(StringUtils.fromString(Constants.OUT_MSG), result);

        } else if (Constants.BLOB_OUTPUT.equals(outputBinding)) {
            if (result instanceof BArray) {
                BArray arrayValue = (BArray) result;
                BString encodedString = ToBase64.toBase64(arrayValue);
                mapValue.put(StringUtils.fromString(Constants.OUT_MSG), encodedString);
            }

        } else if (outputBinding == null || Constants.HTTP_OUTPUT.equals(outputBinding)) {
            if (isHTTPStatusCodeResponse(result)) {
                handleStatusCodeResponse((BMap<?, ?>) result, mapValue);
            } else {
                handleNonStatusCodeResponse(result, mapValue);
            }
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

    private boolean isHTTPStatusCodeResponse(Object result) {
//        Module resultPkg = TypeUtils.getType(result).getPackage();
        return (result instanceof BMap) && (((BMap) result).containsKey(fromString(Constants.STATUS)));
        //TODO : Check inheritance
        //(https://github.com/ballerina-platform/module-ballerinax-azure.functions/issues/490)
    }

    private boolean isContentTypeExist(BMap<BString, ?> headersMap) {
        for (BString headerKey : headersMap.getKeys()) {
            if (headerKey.getValue().toLowerCase(Locale.ROOT).equals(Constants.CONTENT_TYPE.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void addStatusCodeImplicitly(BMap<BString, Object> respMap) {
        String accessor = ((ResourceMethodType) this.methodType).getAccessor();
        Object statusCode = "";
        if (Constants.POST.equals(accessor)) {
            statusCode = Constants.CREATED_201;
        } else if (Constants.GET.equals(accessor) || Constants.PUT.equals(accessor) ||
                Constants.PATCH.equals(accessor) || Constants.DELETE.equals(accessor) ||
                Constants.HEAD.equals(accessor) || Constants.OPTIONS.equals(accessor) ||
                Constants.DEFAULT.equals(accessor)) {
            statusCode = Constants.OK_200;
        }
        statusCode = StringUtils.fromString((String) statusCode);
        respMap.put(StringUtils.fromString(Constants.STATUS_CODE), statusCode);
    }

    private void addContentTypeImplicitly(Object result, BMap<BString, Object> headers) {
        if (result instanceof BString) {
            headers.put(StringUtils.fromString(Constants.CONTENT_TYPE), StringUtils.fromString(Constants.TEXT_PLAIN));

        } else if (result instanceof BXmlItem) {
            headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                    StringUtils.fromString(Constants.APPLICATION_XML));

        } else if (result instanceof BArray) {
            BArray arrayResult = (BArray) result;
            if (Constants.BYTE_TYPE.equals(arrayResult.getElementType().getName())) {
                headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                        StringUtils.fromString(Constants.APPLICATION_OCTET_STREAM));

            } else if (Constants.MAP_TYPE.equals(arrayResult.getElementType().getName())) {
                MapType mapContent = (MapType) arrayResult.getElementType();
                if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                    headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                            StringUtils.fromString(Constants.APPLICATION_JSON));
                }

            } else if (Constants.TABLE_TYPE.equals(arrayResult.getElementType().getName())) {
                TableType tableContent = (TableType) arrayResult.getElementType();
                if (Constants.MAP_TYPE.equals(tableContent.getConstrainedType().getName())) {
                    MapType mapContent = (MapType) tableContent.getConstrainedType();
                    if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                        headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                                StringUtils.fromString(Constants.APPLICATION_JSON));
                    }

                }
            }

        } else if (result instanceof BTable) {
            BTable tableResult = (BTable) result;
            TableType tableContent = (TableType) tableResult.getType();
            if (Constants.MAP_TYPE.equals(tableContent.getConstrainedType().getName())) {
                MapType mapContent = (MapType) tableContent.getConstrainedType();
                if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                    headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                            StringUtils.fromString(Constants.APPLICATION_JSON));
                }

            }
        } else if (result instanceof BDecimal || result instanceof Long || result instanceof Double ||
                result instanceof Boolean) {
            headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                    StringUtils.fromString(Constants.APPLICATION_JSON));
        } else if (result instanceof BMap) {
            headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                    StringUtils.fromString(Constants.APPLICATION_JSON));
        }
    }

    private void handleNonStatusCodeResponse(Object result, BMap<BString, Object> mapValue) {
        BMap<BString, Object> respMap =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
        BMap<BString, Object> headers =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));

        addStatusCodeImplicitly(respMap);
        addContentTypeImplicitly(result, headers);

        respMap.put(StringUtils.fromString(Constants.HEADERS), headers);
        if (result instanceof BArray) {
            BArray arrayResult = (BArray) result;
            if (Constants.BYTE_TYPE.equals(arrayResult.getElementType().getName())) {
                respMap.put(StringUtils.fromString(Constants.BODY), ToBase64.toBase64(arrayResult));
            } else {
                respMap.put(StringUtils.fromString(Constants.BODY), result);
            }
        } else {
            respMap.put(StringUtils.fromString(Constants.BODY), result);
        }
        mapValue.put(StringUtils.fromString(Constants.RESPONSE_FIELD), respMap);
    }

    private void handleStatusCodeResponse(BMap<?, ?> result, BMap<BString, Object> mapValue) {
        BMap<?, ?> resultMap = result;

        // Extract status code
        BObject status = (BObject) (resultMap.get(StringUtils.fromString(Constants.STATUS)));
        Object statusCode = Long.toString(status.getIntValue(StringUtils.fromString(Constants.CODE)));
        statusCode = StringUtils.fromString((String) statusCode);

        // Create a BMap for response field
        BMap<BString, Object> respMap =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
        respMap.put(StringUtils.fromString(Constants.STATUS_CODE), statusCode);

        // Create body field in the response Map
        if (resultMap.containsKey(StringUtils.fromString(Constants.BODY))) {
            Object body = resultMap.get(StringUtils.fromString(Constants.BODY));
            respMap.put(StringUtils.fromString(Constants.BODY), body);
        }

        // Create header field in the response Map
        if (resultMap.containsKey(StringUtils.fromString(Constants.HEADERS))) {
            Object headers = resultMap.get(StringUtils.fromString(Constants.HEADERS));
            BMap headersMap = (BMap) headers;
            // Add Content-type field in headers if there is not
            if (!isContentTypeExist(headersMap)) {
                headersMap.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                        StringUtils.fromString(Constants.APPLICATION_JSON));
            }
            respMap.put(StringUtils.fromString(Constants.HEADERS), headers);
        } else {
            // If there is no headers add one with default content-type
            Object headers =
                    ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
            ((BMap) headers).put(StringUtils.fromString(Constants.CONTENT_TYPE),
                    StringUtils.fromString(Constants.APPLICATION_JSON));
            respMap.put(StringUtils.fromString(Constants.HEADERS), headers);

        }

        // If there is mediaType replace content-type in headers
        if (resultMap.containsKey(StringUtils.fromString(Constants.MEDIA_TYPE))) {
            Object headers = resultMap.get(StringUtils.fromString(Constants.HEADERS));
            if (headers != null) {
                Object mediaType = resultMap.get(StringUtils.fromString(Constants.MEDIA_TYPE));
                ((BMap) headers).put(StringUtils.fromString(Constants.CONTENT_TYPE), mediaType);
            }
        }
        mapValue.put(StringUtils.fromString(Constants.RESPONSE_FIELD), respMap);
    }
}


