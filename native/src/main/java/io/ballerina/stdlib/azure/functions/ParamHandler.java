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
import io.ballerina.stdlib.azure.functions.bindings.input.BlobInput;
import io.ballerina.stdlib.azure.functions.bindings.input.CosmosInput;
import io.ballerina.stdlib.azure.functions.bindings.input.InputBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.azure.functions.Constants.HEADER_ANNOTATION;
import static io.ballerina.stdlib.azure.functions.Constants.HTTP_PACKAGE_NAME;
import static io.ballerina.stdlib.azure.functions.Constants.HTTP_PACKAGE_ORG;
import static io.ballerina.stdlib.azure.functions.Constants.SERVICE_CONF_ANNOTATION;

/**
 * Represents the input binding handler.
 *
 * @since 2.0.0
 */
public class ParamHandler {

    public static boolean isAzureAnnotationExist(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        for (BString bKey : ((BMap<BString, ?>) annotation).getKeys()) {
            String key = bKey.getValue();
            if (key.startsWith(Constants.PACKAGE_ORG + Constants.SLASH + Constants.PACKAGE_NAME)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPayloadAnnotationParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        for (BString bKey : ((BMap<BString, ?>) annotation).getKeys()) {
            String key = bKey.getValue();
            if (key.startsWith(HTTP_PACKAGE_ORG + Constants.SLASH + Constants.HTTP_PACKAGE_NAME) &&
                    key.endsWith(Constants.PAYLOAD_ANNOTATAION)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHeaderAnnotationParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        for (BString bKey : ((BMap<BString, ?>) annotation).getKeys()) {
            String[] keySegments = (bKey.getValue()).split("[/:]");
            if ((keySegments.length == 4) && HTTP_PACKAGE_ORG.equals(keySegments[0]) &&
                    HTTP_PACKAGE_NAME.equals(keySegments[1]) && HEADER_ANNOTATION.equals(keySegments[3])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHttpServiceConfExist(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        for (BString bKey : ((BMap<BString, ?>) annotation).getKeys()) {
            String[] keySegments = (bKey.getValue()).split("[/:]");
            if ((keySegments.length == 4) && HTTP_PACKAGE_ORG.equals(keySegments[0]) &&
                    HTTP_PACKAGE_NAME.equals(keySegments[1]) && SERVICE_CONF_ANNOTATION.equals(keySegments[3])) {
                return true;
            }
        }
        return false;
    }

    public static Optional<InputBinding> getInputBindingHandler(Object annotation) {
        if (annotation == null) {
            return Optional.empty();
        }
        if (!(annotation instanceof BMap)) {
            return Optional.empty();
        }
        for (BString key : ((BMap<BString, ?>) annotation).getKeys()) {
            String annotationKey = key.getValue();
            String annotationName = annotationKey.substring(annotationKey.lastIndexOf(':') + 1);

            List<InputBinding> inputBindings = new ArrayList<>();
            inputBindings.add(new BlobInput());
            inputBindings.add(new CosmosInput());

            for (InputBinding inputBinding : inputBindings) {
                if (inputBinding.getName().equals(annotationName)) {
                    return Optional.of(inputBinding);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isBindingNameParam(Object annotation) {
        if (annotation == null) {
            return false;
        }
        if (!(annotation instanceof BMap)) {
            return false;
        }

        Object value =
                ((BMap<?, ?>) annotation).get(StringUtils.fromString(Constants.PACKAGE_COMPLETE + ":BindingName"));
        return value != null;
    }

}
