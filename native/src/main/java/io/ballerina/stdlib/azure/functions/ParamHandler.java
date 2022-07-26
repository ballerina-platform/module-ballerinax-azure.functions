package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.Arrays;

enum InputBindings {
    COSMOS("CosmosDBInput");

    private String annotation;
    InputBindings (String annotation) {
        this.annotation = annotation;
    }

    public String getAnnotation() {
        return annotation;
    }
}

/**
 * Represents the input binding handler.
 * 
 * @since 2.0.0
 */
public class ParamHandler {

    public static boolean isPayloadAnnotationParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        Object value = ((BMap<?, ?>) annotation).get(StringUtils.fromString("ballerinax/azure_functions:3:Payload"));
        return value instanceof Boolean;
    }
    
    public static boolean isInputAnnotationParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }
        for (Object key : ((BMap<?, ?>) annotation).getKeys()) {
            if (key instanceof BString) {
                String annotationKey = ((BString) key).getValue();
                String annotationName = annotationKey.substring(annotationKey.lastIndexOf(':') + 1);
                return Arrays.stream(InputBindings.values())
                        .anyMatch(name -> name.getAnnotation().equals(annotationName));
            }
        }
        return false;
    }

    public static boolean isBindingNameParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        Object value = ((BMap<?, ?>) annotation).get(StringUtils.fromString("ballerinax/azure_functions:3:BindingName"));
        return value != null;
    }

}
