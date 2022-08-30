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

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.bindings.input.InputBinding;
import io.ballerina.stdlib.azure.functions.builder.AbstractPayloadBuilder;
import io.ballerina.stdlib.azure.functions.exceptions.InvalidPayloadException;
import io.ballerina.stdlib.azure.functions.exceptions.PayloadNotFoundException;
import org.ballerinalang.langlib.bool.FromString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.ballerina.runtime.api.TypeTags.BOOLEAN_TAG;
import static io.ballerina.runtime.api.TypeTags.DECIMAL_TAG;
import static io.ballerina.runtime.api.TypeTags.FLOAT_TAG;
import static io.ballerina.runtime.api.TypeTags.INT_TAG;

/**
 * Represents a Azure Resource function property.
 *
 * @since 2.0.0
 */
public class HttpResource {

    private static final ArrayType INT_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_INT);
    private static final ArrayType FLOAT_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_FLOAT);
    private static final ArrayType BOOLEAN_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_BOOLEAN);
    private static final ArrayType DECIMAL_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_DECIMAL);

    private PathParameter[] pathParams;
    private QueryParameter[] queryParameter;
    private PayloadParameter payloadParameter;
    private InputBindingParameter[] inputBindingParameters;

    public HttpResource(ResourceMethodType resourceMethodType, BMap<?, ?> body) {
        this.pathParams = getPathParams(resourceMethodType, body);
        this.payloadParameter = processPayloadParam(resourceMethodType, body).orElse(null);
        this.queryParameter = getQueryParams(resourceMethodType, body);
        this.inputBindingParameters = getInputBindingParams(resourceMethodType, body);
    }

    private InputBindingParameter[] getInputBindingParams(ResourceMethodType resourceMethod, BMap<?, ?> body)
            throws InvalidPayloadException {
        Parameter[] parameters = resourceMethod.getParameters();
        List<InputBindingParameter> inputBindingParameters = new ArrayList<>();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation = resourceMethod.getAnnotation(StringUtils.fromString("$param$." + name));
            Optional<InputBinding> inputBindingHandler = ParamHandler.getInputBindingHandler(annotation);
            if (inputBindingHandler.isEmpty()) {
                continue;
            }
            BString bodyValue = body.getStringValue(StringUtils.fromString(name));
            InputBinding inputBinding = inputBindingHandler.get();
            Type type = parameter.type;
            try {
                AbstractPayloadBuilder payloadBuilder = inputBinding.getPayloadBuilder(type);
                Object bValue = payloadBuilder.getValue(bodyValue, false);
                inputBindingParameters.add(new InputBindingParameter(i, parameter, bValue));
            } catch (BError error) {
                throw new InvalidPayloadException(error.getMessage());
            }
        }

        return inputBindingParameters.toArray(InputBindingParameter[]::new);
    }

    private QueryParameter[] getQueryParams(ResourceMethodType resourceMethod, BMap<?, ?> body) {
        BMap<?, ?> queryParams = body.getMapValue(StringUtils.fromString("httpPayload"))
                .getMapValue(StringUtils.fromString("Query"));
        Parameter[] parameters = resourceMethod.getParameters();
        List<QueryParameter> queryParameters = new ArrayList<>();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation = resourceMethod.getAnnotation(StringUtils.fromString("$param$." + name));
            //TODO Add other annotations as well
            if (isAzAnnotationExist(annotation)) {
                continue;
            }
            BString queryValue = queryParams.getStringValue(StringUtils.fromString(name));
            try {
                Object bValue = createValue(parameter.type, queryValue);
                queryParameters.add(new QueryParameter(i, parameter, bValue));
            } catch (BError bError) {
                throw new InvalidPayloadException(bError.getMessage());
            }
        }
        return queryParameters.toArray(QueryParameter[]::new);
    }
    
    private Object createValue(Type type, BString queryValue) {
        switch (type.getTag()) {
            case TypeTags.STRING_TAG:
                return queryValue;
            case TypeTags.BOOLEAN_TAG:
                return FromString.fromString(queryValue);
            case TypeTags.INT_TAG:
                return org.ballerinalang.langlib.integer.FromString.fromString(queryValue);
            case TypeTags.FLOAT_TAG:
                return org.ballerinalang.langlib.floatingpoint.FromString.fromString(queryValue);
            case TypeTags.DECIMAL_TAG:
                return org.ballerinalang.langlib.decimal.FromString.fromString(queryValue);
            case TypeTags.UNION_TAG:
                List<Type> memberTypes = ((UnionType) type).getMemberTypes();
                for (Type memberType : memberTypes) {
                    try {
                        return createValue(memberType, queryValue);
                    } catch (BError ignored) {
                        // thrown errors are ignored until all the types are iterated
                    }
                }
                return null;
            case TypeTags.ARRAY_TAG:
                ArrayType arrayType = (ArrayType) type;
                Type elementType = arrayType.getElementType();
                if (queryValue == null) {
                    return null;
                }
                String[] values = queryValue.getValue().split(",");
                return castParamArray(elementType.getTag(), values);
            default:
                throw new InvalidPayloadException("unsupported parameter type " + type.getName());
        }
    }

    public static BArray castParamArray(int targetElementTypeTag, String[] argValueArr) {
        switch (targetElementTypeTag) {
            case INT_TAG:
                return getBArray(argValueArr, INT_ARR, targetElementTypeTag);
            case FLOAT_TAG:
                return getBArray(argValueArr, FLOAT_ARR, targetElementTypeTag);
            case BOOLEAN_TAG:
                return getBArray(argValueArr, BOOLEAN_ARR, targetElementTypeTag);
            case DECIMAL_TAG:
                return getBArray(argValueArr, DECIMAL_ARR, targetElementTypeTag);
            default:
                return StringUtils.fromStringArray(argValueArr);
        }
    }

    private static BArray getBArray(String[] valueArray, ArrayType arrayType, int elementTypeTag) {
        BArray arrayValue = ValueCreator.createArrayValue(arrayType);
        int index = 0;
        for (String element : valueArray) {
            switch (elementTypeTag) {
                case INT_TAG:
                    arrayValue.add(index++, Long.parseLong(element));
                    break;
                case FLOAT_TAG:
                    arrayValue.add(index++, Double.parseDouble(element));
                    break;
                case BOOLEAN_TAG:
                    arrayValue.add(index++, Boolean.parseBoolean(element));
                    break;
                case DECIMAL_TAG:
                    arrayValue.add(index++, ValueCreator.createDecimalValue(element));
                    break;
                default:
                    throw new InvalidPayloadException("Illegal state error: unexpected param type");
            }
        }
        return arrayValue;
    }

    private boolean isAzAnnotationExist(Object annotation) {
        if (annotation == null) {
            return false;
        }
        return true;
    }

    private PathParameter[] getPathParams(ResourceMethodType resourceMethod, BMap<?, ?> body) {
        String[] resourcePath = resourceMethod.getResourcePath();
        Parameter[] parameters = resourceMethod.getParameters();
        List<PathParameter> pathParams = new ArrayList<>();
        int count = 0;
        for (String path : resourcePath) {
            if (path.equals("*")) {
                Parameter parameter = parameters[count];
                BMap<?, ?> payload = body.getMapValue(StringUtils.fromString("httpPayload"));
                BMap<?, ?> params = payload.getMapValue(StringUtils.fromString("Params"));
                BString param = params.getStringValue(StringUtils.fromString(parameter.name));
                pathParams.add(new PathParameter(count, parameter, param.getValue()));
                count++;
            }
        }

        return pathParams.toArray(PathParameter[]::new);
    }

    private Optional<PayloadParameter> processPayloadParam(ResourceMethodType resourceMethod, BMap<?, ?> body)
            throws PayloadNotFoundException {
        Parameter[] parameters = resourceMethod.getParameters();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation = resourceMethod.getAnnotation(StringUtils.fromString("$param$." + name));
            if (!ParamHandler.isPayloadAnnotationParam(annotation)) {
                continue;
            }
//            if (annotation == null) {
//                continue;
//            }
//            if (!(annotation instanceof BMap)) {
//                continue;
//            }
//            Boolean booleanValue = ((BMap<BString, Boolean>) annotation).getBooleanValue(StringUtils.fromString(
//                    "ballerinax/azure_functions:3:Payload"));
//            if (booleanValue == null || !booleanValue) {
//                continue;
//            }
            BMap<?, ?> httpPayload = body.getMapValue(StringUtils.fromString("httpPayload"));
            BMap<?, ?> headers = httpPayload.getMapValue(StringUtils.fromString("Headers"));
            Type type = parameter.type;
            String contentType = getContentTypeHeader(headers);
            BString bodyValue = getRequestBody(httpPayload, name, type);
            if (isNilType(type) && bodyValue == null) {
                return Optional.of(new PayloadParameter(i, parameter, null));
            }
            try {
                AbstractPayloadBuilder builder = AbstractPayloadBuilder.getBuilder(contentType, type);
                Object bValue = builder.getValue(bodyValue, false);
                return Optional.of(new PayloadParameter(i, parameter, bValue));
            } catch (BError error) {
                throw new InvalidPayloadException(error.getMessage());
            }
        }
        return Optional.empty();
    }

    private boolean isNilType(Type type) {
        if (type.getTag() == TypeTags.UNION_TAG) {
            List<Type> memberTypes = ((UnionType) type).getMemberTypes();
            for (Type memberType : memberTypes) {
                if (isNilType(memberType)) {
                    return true;
                }
            }
        } else if (type.getTag() == TypeTags.NULL_TAG) {
            return true;
        }
        return false;
    }

    private BString getRequestBody(BMap<?, ?> httpPayload, String name, Type type) throws PayloadNotFoundException {
        BString bBody = StringUtils.fromString("Body");
        if (httpPayload.containsKey(bBody)) {
            return httpPayload.getStringValue(bBody);
        }
        if (!isNilType(type)) {
            throw new PayloadNotFoundException("payload not found for the variable '" + name + "'");
        }
        return null;
    }

    private String getContentTypeHeader(BMap<?, ?> headers) {
        //TODO fix lower case
        if (headers.containsKey(StringUtils.fromString(Constants.CONTENT_TYPE))) {
            BArray headersArrayValue = headers.getArrayValue(StringUtils.fromString(Constants.CONTENT_TYPE));
            return headersArrayValue.getBString(0).getValue();
        } else {
            return null;
        }
    }

    public Object[] getArgList() {
        List<AZFParameter> parameters = new ArrayList<>(Arrays.asList(pathParams));
        parameters.addAll(Arrays.asList(queryParameter));
        parameters.addAll(Arrays.asList(inputBindingParameters));
        if (payloadParameter != null) {
            parameters.add(payloadParameter);
        }
        //TODO add more input output binding params
        Collections.sort(parameters);

        Object[] args = new Object[parameters.size() * 2];
        int i = 0;
        for (AZFParameter parameter : parameters) {
            Object bValue = parameter.getValue();
            args[i++] = bValue;
            args[i++] = true;
        }
        return args;
    }
}
