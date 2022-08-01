package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.builder.AbstractPayloadBuilder;
import io.ballerina.stdlib.azure.functions.builder.JsonPayloadBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.ballerina.runtime.api.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.TypeTags.STRING_TAG;

/**
 * Represents a Azure Resource function property.
 *
 * @since 2.0.0
 */
public class HttpResource {

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

    private InputBindingParameter[] getInputBindingParams(ResourceMethodType resourceMethod, BMap<?, ?> body) {
        Parameter[] parameters = resourceMethod.getParameters();
        List<InputBindingParameter> inputBindingParameters = new ArrayList<>();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation = resourceMethod.getAnnotation(StringUtils.fromString("$param$." + name));
            if (!ParamHandler.isInputAnnotationParam(annotation)) {
                continue;
            }
            BString bodyValue = body.getStringValue(StringUtils.fromString(name));
            Type type = parameter.type;
            JsonPayloadBuilder jsonPayloadBuilder = new JsonPayloadBuilder(type);
            Object bValue = jsonPayloadBuilder.getValue(bodyValue, false);
//            Object bValue = Utilities.convertJsonToDataBoundParamValue(bodyValue, type);
            //TODO handle other types records n stuff
            inputBindingParameters.add(new InputBindingParameter(i, parameter, bValue));
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
            if (annotation != null) { //Add other annotations as well
                continue;
            }
            Object arr = queryParams.get(StringUtils.fromString(name));
            //TODO Handle optional null type
//            if (arr == null) {
//                
//            }
            int tag = parameter.type.getTag();
            Object bValue = null;
            switch (tag) {
                case STRING_TAG:
                    //TODO error
//                    if (!(arr instanceof BString)) {
//                        
//                    }
                    bValue = arr;
                    break;
                case ARRAY_TAG:
                    //TODO handle other cases
                    BArray values = (BArray) arr;
//                    Type elementType = ((ArrayType) parameter.type).getElementType();
                    bValue = values; //TODO check all types
//                    if (elementType.getTag() == STRING_TAG) {
//                        BString[] bString = new BString[values.length];
//                        for (int j = 0, valuesLength = values.length; j < valuesLength; j++) {
//                            String value = values[j];
//                            bString[j] =StringUtils.fromString(value);
//                            bValue = ValueCreator.createArrayValue(bString);
//                        }
//                    }
                    break;
                default:
                    //TODO unsupported
            }
            queryParameters.add(new QueryParameter(i, parameter, bValue));
        }
        return queryParameters.toArray(QueryParameter[]::new);
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

    private Optional<PayloadParameter> processPayloadParam(ResourceMethodType resourceMethod, BMap<?, ?> body) {
        Parameter[] parameters = resourceMethod.getParameters();
        for (int i = this.pathParams.length, parametersLength = parameters.length; i < parametersLength; i++) {
            Parameter parameter = parameters[i];
            String name = parameter.name;
            Object annotation = resourceMethod.getAnnotation(StringUtils.fromString("$param$." + name));
            if (annotation == null) {
                continue;
            }
            if (!(annotation instanceof BMap)) {
                continue;
            }
            Boolean booleanValue = ((BMap<BString, Boolean>) annotation).getBooleanValue(StringUtils.fromString(
                    "ballerinax/azure_functions:3:Payload"));
            if (!booleanValue) {
                continue;
            }
            BMap<?, ?> httpPayload = body.getMapValue(StringUtils.fromString("httpPayload"));
            BMap<?, ?> headers = body.getMapValue(StringUtils.fromString("httpPayload"))
                    .getMapValue(StringUtils.fromString("Headers"));
            BString contentType = headers.getArrayValue(StringUtils.fromString("Content-Type")).getBString(0);
            BString bodyValue = httpPayload.getStringValue(StringUtils.fromString("Body"));
            //TODO handle payload 400
            Type type = parameter.type;
            AbstractPayloadBuilder builder = AbstractPayloadBuilder.getBuilder(contentType.getValue(), type);
            Object bValue = builder.getValue(bodyValue, false);
//            Object bValue = Utilities.convertJsonToDataBoundParamValue(bodyValue, type);
//            str.createValue(type, false);
//            Object bValue = Utilities.convertJsonToDataBoundParamValue(bodyValue, type);
//            Object bValue = builder.getValue((BObject) body1, false);
            //TODO handle other types records n stuff
            return Optional.of(new PayloadParameter(i, parameter, bValue));
        }
        return Optional.empty();
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
