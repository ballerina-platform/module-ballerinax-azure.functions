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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.Arrays;

enum InputBindings {
    COSMOS("CosmosDBInput");

    private String annotation;

    InputBindings(String annotation) {
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

        Object value =
                ((BMap<?, ?>) annotation).get(StringUtils.fromString("ballerinax/azure_functions:3:BindingName"));
        return value != null;
    }

}
