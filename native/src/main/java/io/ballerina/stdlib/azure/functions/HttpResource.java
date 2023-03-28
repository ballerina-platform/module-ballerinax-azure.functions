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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.ReferenceType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.bindings.input.InputBinding;
import io.ballerina.stdlib.azure.functions.builder.AbstractPayloadBuilder;
import io.ballerina.stdlib.azure.functions.exceptions.HeaderNotFoundException;
import io.ballerina.stdlib.azure.functions.exceptions.InvalidPayloadException;
import io.ballerina.stdlib.azure.functions.exceptions.PayloadNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an Azure Resource function property.
 *
 * @since 2.0.0
 */
public class HttpResource {

    private PathParameter[] pathParams;
    private QueryParameter[] queryParameter;
    private PayloadParameter payloadParameter;
    private InputBindingParameter[] inputBindingParameters;
    private HeaderParameter headerParameter;

    public HttpResource(ResourceMethodType resourceMethodType, BMap<?, ?> body, BMap<?, ?> serviceAnnotations) {
        this.pathParams = getPathParams(resourceMethodType, body);
        this.payloadParameter = processPayloadParam(resourceMethodType, body).orElse(null);
        this.queryParameter = getQueryParams(resourceMethodType, body);
        this.inputBindingParameters = getInputBindingParams(resourceMethodType, body);
        this.headerParameter = processHeaderParam(resourceMethodType, body, serviceAnnotations).orElse(null);
    }

    private InputBindingParameter[] getInputBindingParams(ResourceMethodType resourceMethod, BMap<?, ?> body)
            throws InvalidPayloadException {
        Parameter[] parameters = resourceMethod.getParameters();
        List<InputBindingParameter> inputBindingParameters = new ArrayList<>();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation =
                    resourceMethod.getAnnotation(StringUtils.fromString(Constants.PARAMETER_ANNOTATION + name));
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
        BMap<?, ?> queryParams = body.getMapValue(StringUtils.fromString(Constants.HTTP_TRIGGER_IDENTIFIER))
                .getMapValue(StringUtils.fromString(Constants.AZURE_QUERY_HEADERS));
        Parameter[] parameters = resourceMethod.getParameters();
        List<QueryParameter> queryParameters = new ArrayList<>();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation =
                    resourceMethod.getAnnotation(StringUtils.fromString(Constants.PARAMETER_ANNOTATION + name));
            if (!ParamHandler.isQueryAnnotationParam(annotation)) {
                continue;
            }
            BString queryValue = queryParams.getStringValue(StringUtils.fromString(name));
            // '<url-query-param>' and '<url-query-param>=' are identical in azure platform.
            if (queryValue == null && !Utils.isNilType(parameter.type)) {
                throw new InvalidPayloadException("Error : no query param value found for '" + name + "'");
            }
            try {
                Object bValue = Utils.createValue(parameter, queryValue);
                queryParameters.add(new QueryParameter(i, parameter, bValue));
            } catch (BError bError) {
                throw new InvalidPayloadException(bError.getMessage());
            } catch (Exception e) {
                throw new InvalidPayloadException("Query param value parsing failed for '" + name + "'");
            }
        }
        return queryParameters.toArray(QueryParameter[]::new);
    }

    private PathParameter[] getPathParams(ResourceMethodType resourceMethod, BMap<?, ?> body) {
        String[] resourcePath = resourceMethod.getResourcePath();
        Parameter[] parameters = resourceMethod.getParameters();
        List<PathParameter> pathParams = new ArrayList<>();
        int count = 0;
        for (String path : resourcePath) {
            if (path.equals(Constants.PATH_PARAM)) {
                Parameter parameter = parameters[count];
                BMap<?, ?> payload = body.getMapValue(StringUtils.fromString(Constants.HTTP_TRIGGER_IDENTIFIER));
                BMap<?, ?> params = payload.getMapValue(StringUtils.fromString(Constants.AZURE_PAYLOAD_PARAMS));
                BString param = params.getStringValue(StringUtils.fromString(parameter.name));
                Object value = Utils.createValue(parameter, param);
                pathParams.add(new PathParameter(count, parameter, value));
                count++;
            } else if (path.equals(Constants.REST_PATH_PARAM)) {
                Parameter parameter = parameters[count];
                BMap<?, ?> payload = body.getMapValue(StringUtils.fromString(Constants.HTTP_TRIGGER_IDENTIFIER));
                BMap<?, ?> params = payload.getMapValue(StringUtils.fromString(Constants.AZURE_PAYLOAD_PARAMS));
                String param = params.getStringValue(StringUtils.fromString(parameter.name)).getValue();
                BString modParamValue = StringUtils.fromString(param.replace(Constants.SLASH,
                        Constants.ELEMENT_SEPARATOR));
                Object arrValue = Utils.createValue(parameter, modParamValue);
                pathParams.add(new PathParameter(count, parameter, arrValue));
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
            Object annotation =
                    resourceMethod.getAnnotation(StringUtils.fromString(Constants.PARAMETER_ANNOTATION + name));
            if (!ParamHandler.isPayloadAnnotationParam(annotation)) {
                continue;
            }
            BMap<?, ?> httpPayload = body.getMapValue(StringUtils.fromString(Constants.HTTP_TRIGGER_IDENTIFIER));
            BMap<?, ?> headers = httpPayload.getMapValue(StringUtils.fromString(Constants.AZURE_PAYLOAD_HEADERS));
            Type type = parameter.type;
            String contentType = Utils.getContentTypeHeader(headers);
            BString bodyValue = Utils.getRequestBody(httpPayload, name, type);
            if (Utils.isNilType(type) && bodyValue == null) {
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

    private Optional<HeaderParameter> processHeaderParam(ResourceMethodType resourceMethod, BMap<?, ?> body,
                                                         BMap<?, ?> serviceAnnotations) {
        Parameter[] parameters = resourceMethod.getParameters();
        Object headerParam;
        Boolean treatNilableAsOptional = true;
        String serviceConfig = Constants.HTTP_ANNOTATION_PREFIX + Constants.SERVICE_CONF_ANNOTATION;
        boolean isServiceConfExist = ParamHandler.isHttpServiceConfExist(serviceAnnotations);
        if (isServiceConfExist) {
            treatNilableAsOptional = serviceAnnotations.getMapValue(StringUtils.fromString(serviceConfig)).
                    getBooleanValue(StringUtils.fromString("treatNilableAsOptional"));
        }
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation =
                    resourceMethod.getAnnotation(StringUtils.fromString(Constants.PARAMETER_ANNOTATION + name));
            if (annotation == null) {
                continue;
            }
            boolean isHeaderAnnotation = ParamHandler.isHeaderAnnotationParam(annotation);
            if (!isHeaderAnnotation) {
                continue;
            }
            BMap<?, ?> httpPayload = body.getMapValue(StringUtils.fromString(Constants.HTTP_TRIGGER_IDENTIFIER));
            BMap<BString, ?> headers =
                    (BMap<BString, ?>) httpPayload.getMapValue(StringUtils.fromString(Constants.AZURE_PAYLOAD_HEADERS));

            String headerAnnotation = Constants.HTTP_ANNOTATION_PREFIX + Constants.HEADER_ANNOTATION;
            BMap<?, ?> headerAnnotationField =
                    (BMap<?, ?>) ((BMap<?, ?>) annotation).get(StringUtils.fromString(headerAnnotation));
            if (headerAnnotationField.size() == 0) {
                //No annotation field defined {name: ....}
                if ((parameter.type).getTag() == TypeTags.TYPE_REFERENCED_TYPE_TAG) {
                    ReferenceType type = (ReferenceType) parameter.type;
                    headerParam = processHeaderRecordParam(headers, type, treatNilableAsOptional);
                    return Optional.of(new HeaderParameter(i, parameter, headerParam));
                }
                headerParam = getHeaderValue(headers, parameter.type, name, treatNilableAsOptional);
                return Optional.of(new HeaderParameter(i, parameter, headerParam));
            } else if (headerAnnotationField.size() == 1) {
                // Annotation field is defined
                BString headerName = headerAnnotationField.getStringValue(StringUtils.fromString("name"));
                headerParam = getHeaderValue(headers, parameter.type, headerName.getValue(), treatNilableAsOptional);
                return Optional.of(new HeaderParameter(i, parameter, headerParam));
            } else {
                throw new RuntimeException("Header annotation can have only one name field.");
            }
        }
        return Optional.empty();
    }

    private Object getHeaderValue(BMap<BString, ?> headers, Type type, String fieldName,
                                  boolean treatNilableAsOptional) {
        BString headerValue = null;
        boolean isHeaderExist = false;
        for (BString headerKey : headers.getKeys()) {
            if (headerKey.getValue().toLowerCase(Locale.ROOT).equals(fieldName.toLowerCase(Locale.ROOT))) {
                isHeaderExist = true;
                if (((BArray) (headers.get(headerKey))).size() == 0) {
                    break;
                }
                headerValue = (BString) ((BArray) (headers.get(headerKey))).get(0);
            }
        }
        if (!isHeaderExist) {
            //Header name not exist case
            if (Utils.isNilType(type) && treatNilableAsOptional) {
                return null;
            }
            throw new HeaderNotFoundException("no header value found for '" + fieldName + "'");
        } else if (headerValue.getValue().equals("")) {
            //Handle header value not exist case
            if (Utils.isNilType(type)) {
                return null;
            }
            throw new HeaderNotFoundException("no header value found for '" + fieldName + "'");

        }
        return Utils.createValue(type, headerValue);
    }

    private Object processHeaderRecordParam(BMap<BString, ?> headers, ReferenceType parameter,
                                            Boolean treatNilableAsOptional) {
        RecordType recordType = (RecordType) parameter.getReferredType();
        Map<String, Field> fields = recordType.getFields();
        BMap<BString, Object> recordValue = ValueCreator.createRecordValue(recordType);
        for (Map.Entry<String, Field> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Type fieldType = field.getValue().getFieldType();
            Object headerValue = getHeaderValue(headers, fieldType, fieldName, treatNilableAsOptional);
            recordValue.put(StringUtils.fromString(fieldName), headerValue);
        }
        return recordValue;
    }

    public Object[] getArgList() {
        List<AZFParameter> parameters = new ArrayList<>(Arrays.asList(pathParams));
        parameters.addAll(Arrays.asList(queryParameter));
        parameters.addAll(Arrays.asList(inputBindingParameters));
        if (payloadParameter != null) {
            parameters.add(payloadParameter);
        }
        if (headerParameter != null) {
            parameters.add(headerParameter);
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
