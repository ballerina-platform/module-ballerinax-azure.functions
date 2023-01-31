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
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ReferenceType;
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
import io.ballerina.runtime.api.values.BValue;
import io.ballerina.runtime.api.values.BXmlItem;
import io.ballerina.stdlib.azure.functions.exceptions.UnsupportedTypeException;
import org.ballerinalang.langlib.array.ToBase64;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * {@code FunctionCallback} used to handle the Azure function service method invocation results.
 */
public class FunctionCallback implements Callback {

    private final Future future;
    private final Module module;
    private final MethodType methodType;

    public FunctionCallback(Future future, Module module, MethodType methodType) {
        this.future = future;
        this.module = module;
        this.methodType = methodType;
    }

    private String getOutputAnnotation() {
        List<String> returnAnnotations = new ArrayList<>();
        BMap<BString, ?> annotations =
                (BMap<BString, ?>) methodType.getAnnotation(StringUtils.fromString(Constants.RETURN_ANNOTATION));
        if (annotations != null) {
            for (BString annotation : annotations.getKeys()) {
                String[] split = annotation.getValue().split(":");
                //TODO only add azure functions annotations
                returnAnnotations.add(split[split.length - 1]);
            }
        }

        if (returnAnnotations.size() == 0) {

            //TODO see if we can check trigger type
            if (methodType instanceof ResourceMethodType) {
                return Constants.HTTP_OUTPUT;
            }
            //TODO impl compiler ext validations to make sure output annotations exists
        }
        return returnAnnotations.get(0);
    }

    public List<String> parseTupleAnnotations(BMap<BString, Object> annotations) {
        List<String> returnAnnotations = new ArrayList<>();
        for (int i = 0; i < annotations.size(); i++) {
            BMap<BString, ?> mapValue1 =
                    (BMap<BString, ?>) annotations.getMapValue(StringUtils.fromString("$field$." + i));
            for (BString annotation : mapValue1.getKeys()) {
                String[] split = annotation.getValue().split(":");
                //TODO only add azure functions annotations
                returnAnnotations.add(split[split.length - 1]);
            }
        }
        return returnAnnotations;
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
        //Refactor to readable
        if (result == null) {
            handleNilReturnType(mapValue);
            future.complete(mapValue);
            return;
        }

        try {
            if (result instanceof BValue) {
                BValue bValue = (BValue) result;
                if (bValue.getType().getTag() == TypeTags.TUPLE_TAG) {
                    BArray tupleValues = (BArray) result;
                    BMap<BString, Object> tupleAnnotations =
                            ((AnnotatableType) tupleValues.getTypedesc().getDescribingType()).getAnnotations();
                    List<String> annotations = parseTupleAnnotations(tupleAnnotations);
                    handleTuples(mapValue, tupleValues, annotations);
                    future.complete(mapValue);
                    return;
                } else if (bValue.getType().getTag() == TypeTags.TYPE_REFERENCED_TYPE_TAG) {
                    ReferenceType typeRef = (ReferenceType) bValue.getType();
                    Type referredType = typeRef.getReferredType();
                    if (referredType.getTag() == TypeTags.TUPLE_TAG) {
                        BArray tupleValues = (BArray) bValue;
                        BMap<BString, Object> annotations = ((AnnotatableType) typeRef).getAnnotations();
                        handleTuples(mapValue, tupleValues, parseTupleAnnotations(annotations));
                        future.complete(mapValue);
                        return;
                    }
                }
            }
            String outputBinding = getOutputAnnotation();
            Map.Entry<BString, Object> webWorkerResponse = handleOutputBinding(outputBinding, result, 0);
            mapValue.put(webWorkerResponse.getKey(), webWorkerResponse.getValue());
            future.complete(mapValue);
        } catch (UnsupportedTypeException e) {
            future.complete(Utils.createError(module, e.getMessage(), e.getType()));
        } catch (Exception e) {
            future.complete(
                    Utils.createError(module, Constants.UNSUPPORTED_TYPE_MESSAGE, Constants.UNSUPPORTED_TYPE_ERROR));
        }
    }

    private void handleTuples(BMap<BString, Object> mapValue, BArray tupleValues, List<String> annotations) {
        Object[] values = tupleValues.getValues();
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            Object value = values[i];

            Map.Entry<BString, Object> webWorkerResponse =
                    handleOutputBinding(annotations.get(i), value, i);
            mapValue.put(webWorkerResponse.getKey(), webWorkerResponse.getValue());
        }
    }

    private Map.Entry<BString, Object> handleOutputBinding(String outputBinding, Object value, int index) {
        if (Constants.QUEUE_OUTPUT.equals(outputBinding) || Constants.COSMOS_DBOUTPUT.equals(outputBinding)) {
            return Map.entry(StringUtils.fromString(getBindingIdentifier(index)), value);

        } else if (Constants.BLOB_OUTPUT.equals(outputBinding)) {
            if (value instanceof BArray) {
                BArray arrayValue = (BArray) value;
                BString encodedString = ToBase64.toBase64(arrayValue);
                return Map.entry(StringUtils.fromString(getBindingIdentifier(index)), encodedString);
            }

        } else if (outputBinding == null || Constants.HTTP_OUTPUT.equals(outputBinding)) {
            if (isHTTPStatusCodeResponse(value)) {
                return handleStatusCodeResponse((BMap<?, ?>) value, index);
            } else {
                return handleNonStatusCodeResponse(value, index);
            }
        }

        throw new UnsupportedTypeException();
    }

    private void handleNilReturnType(BMap<BString, Object> mapValue) {
        BMap<BString, Object> respMap =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
        respMap.put(StringUtils.fromString(Constants.STATUS_CODE), Constants.ACCEPTED);
        mapValue.put(StringUtils.fromString(getBindingIdentifier(0)), respMap);
    }

    @Override
    public void notifyFailure(BError bError) {
        bError.printStackTrace();
        future.complete(Utils.createError(module, "internal server error", Constants.INTERNAL_SERVER_ERROR));
    }

    private boolean isModuleDefinedError(BError error) {
        Type errorType = error.getType();
        Module packageDetails = errorType.getPackage();
        String orgName = packageDetails.getOrg();
        String packageName = packageDetails.getName();
        return Constants.PACKAGE_ORG.equals(orgName) && Constants.PACKAGE_NAME.equals(packageName);
    }

    private boolean isHTTPStatusCodeResponse(Object result) {
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
        int statusCode;
        if (Constants.POST.equals(accessor)) {
            statusCode = Constants.CREATED_201;
        } else {
            statusCode = Constants.OK_200;
        }
        respMap.put(StringUtils.fromString(Constants.STATUS_CODE), statusCode);
    }

    private Optional<String> addContentTypeImplicitly(Object value) {
        if (value instanceof BString) {
            return Optional.of(Constants.TEXT_PLAIN);

        } else if (value instanceof BXmlItem) {
            return Optional.of(Constants.APPLICATION_XML);

        } else if (value instanceof BArray) {
            BArray arrayResult = (BArray) value;
            if (Constants.BYTE_TYPE.equals(arrayResult.getElementType().getName())) {
                return Optional.of(Constants.APPLICATION_OCTET_STREAM);

            } else if (Constants.MAP_TYPE.equals(arrayResult.getElementType().getName())) {
                MapType mapContent = (MapType) arrayResult.getElementType();
                if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                    return Optional.of(Constants.APPLICATION_JSON);
                }

            } else if (Constants.TABLE_TYPE.equals(arrayResult.getElementType().getName())) {
                TableType tableContent = (TableType) arrayResult.getElementType();
                if (Constants.MAP_TYPE.equals(tableContent.getConstrainedType().getName())) {
                    MapType mapContent = (MapType) tableContent.getConstrainedType();
                    if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                        return Optional.of(Constants.APPLICATION_JSON);
                    }
                }
            } else {
                return Optional.of(Constants.APPLICATION_JSON);
            }

        } else if (value instanceof BTable) {
            BTable tableResult = (BTable) value;
            TableType tableContent = (TableType) tableResult.getType();
            if (Constants.MAP_TYPE.equals(tableContent.getConstrainedType().getName())) {
                MapType mapContent = (MapType) tableContent.getConstrainedType();
                if (Constants.JSON_TYPE.equals(mapContent.getConstrainedType().getName())) {
                    return Optional.of(Constants.APPLICATION_JSON);
                }

            }
        } else if (value instanceof BDecimal || value instanceof Long || value instanceof Double ||
                value instanceof Boolean) {
            return Optional.of(Constants.APPLICATION_JSON);
        } else if (value instanceof BMap) {
            return Optional.of(Constants.APPLICATION_JSON);
        }
        return Optional.empty();
    }

    private Map.Entry<BString, Object> handleNonStatusCodeResponse(Object result, int index) {
        BMap<BString, Object> respMap =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
        BMap<BString, Object> headers =
                ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));

        addStatusCodeImplicitly(respMap);
        Optional<String> contentType = addContentTypeImplicitly(result);
        contentType
                .ifPresent(s -> headers.put(StringUtils.fromString(Constants.CONTENT_TYPE), StringUtils.fromString(s)));

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
        return Map.entry(StringUtils.fromString(getBindingIdentifier(index)), respMap);
    }

    private Map.Entry<BString, Object> handleStatusCodeResponse(BMap<?, ?> result, int index) {
        BMap<?, ?> resultMap = result;

        // Extract status code
        BObject status = (BObject) (resultMap.get(StringUtils.fromString(Constants.STATUS)));
        long statusCode = status.getIntValue(StringUtils.fromString(Constants.CODE));

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
            Object headers = resultMap.getMapValue(StringUtils.fromString(Constants.HEADERS));
            BMap<BString, Object> headersMap = (BMap) headers;
            // Add Content-type field in headers if there is not
            if (!isContentTypeExist(headersMap)) {
                Optional<String> contentType = getContentType(resultMap);
                contentType.ifPresent(s -> headersMap.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                        StringUtils.fromString(s)));
            }
            respMap.put(StringUtils.fromString(Constants.HEADERS), headers);
        } else {
            // If there is no headers add one with default content-type
            BMap<BString, Object> headers =
                    ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_ANYDATA));
            Optional<String> contentType = getContentType(resultMap);
            contentType.ifPresent(s -> headers.put(StringUtils.fromString(Constants.CONTENT_TYPE),
                    StringUtils.fromString(s)));
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
        return Map.entry(StringUtils.fromString(getBindingIdentifier(index)), respMap);
    }

    private Optional<String> getContentType(BMap<?, ?> resultMap) {
        if (resultMap.containsKey(StringUtils.fromString(Constants.BODY))) {
            Object body = resultMap.get(StringUtils.fromString(Constants.BODY));
            return addContentTypeImplicitly(body);
        } else {
            return Optional.empty();
        }
    }

    private String getBindingIdentifier(int index) {
        if (index == 0) {
            return Constants.RETURN_VAR_NAME;
        }
        return Constants.RETURN_VAR_NAME + index;
    }
}


