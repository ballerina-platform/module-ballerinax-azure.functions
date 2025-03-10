/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.azure.functions.builder;

import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.TypedescType;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.util.List;
import java.util.Locale;

import static io.ballerina.runtime.api.types.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.types.TypeTags.STRING_TAG;
import static io.ballerina.runtime.api.types.TypeTags.XML_TAG;


/**
 * The abstract class to build and convert the payload based on the content-type header. If the content type is not
 * standard, the parameter type is used to infer the builder.
 *
 * @since SwanLake update 1
 */
public abstract class AbstractPayloadBuilder {

    private static final String JSON_PATTERN = "^.*json.*$";
    private static final String XML_PATTERN = "^.*xml.*$";
    private static final String TEXT_PATTERN = "^.*text.*$";
    private static final String OCTET_STREAM_PATTERN = "^.*octet-stream.*$";
    private static final String URL_ENCODED_PATTERN = "^.*x-www-form-urlencoded.*$";
    private static final String FORM_DATA_PATTERN = "^.*form-data.*$";

    /**
     * Get the built inbound payload after binding it to the respective type.
     *
     * @param dataSource inbound request entity
     * @param readonly        readonly status of parameter
     * @return the payload
     */
    public abstract Object getValue(BString dataSource, boolean readonly);

    public static AbstractPayloadBuilder getBuilder(String contentType, Type payloadType) {
        if (contentType == null || contentType.isEmpty()) {
            return getBuilderFromType(payloadType);
        }
        String contentTypeLower = contentType.toLowerCase(Locale.getDefault());
        if (contentTypeLower.matches(XML_PATTERN)) {
            return new XmlPayloadBuilder(payloadType);
        } else if (contentTypeLower.matches(TEXT_PATTERN)) {
            return new StringPayloadBuilder(payloadType);
        } else if (contentTypeLower.matches(URL_ENCODED_PATTERN)) {
            return new StringPayloadBuilder(payloadType);
        } else if (contentTypeLower.matches(OCTET_STREAM_PATTERN)) {
            return new BinaryPayloadBuilder(payloadType);
        } else if (contentTypeLower.matches(JSON_PATTERN)) {
            return new JsonPayloadBuilder(payloadType);
        } else if (contentTypeLower.matches(FORM_DATA_PATTERN)) {
            return new FormPayloadBuilder(payloadType, contentType);
        } else {
            return getBuilderFromType(payloadType);
        }
    }

    private static AbstractPayloadBuilder getBuilderFromType(Type payloadType) {
        switch (payloadType.getTag()) {
            case STRING_TAG:
                return new StringPayloadBuilder(payloadType);
            case XML_TAG:
                return new XmlPayloadBuilder(payloadType);
            case ARRAY_TAG:
                return new ArrayBuilder(payloadType);
            default:
                return new JsonPayloadBuilder(payloadType);
        }
    }

    public static boolean isSubtypeOfAllowedType(Type payloadType, int targetTypeTag) {
        if (payloadType.getTag() == targetTypeTag) {
            return true;
        } else if (payloadType.getTag() == TypeTags.UNION_TAG) {
            assert payloadType instanceof UnionType : payloadType.getClass();
            List<Type> memberTypes = ((UnionType) payloadType).getMemberTypes();
            return memberTypes.stream().anyMatch(memberType -> isSubtypeOfAllowedType(memberType, targetTypeTag));
        }
        return false;
    }

    public static boolean typeIncludedInUnion(BTypedesc unionType, BTypedesc targetType) {
        int targetTypeTag = ((TypedescType) targetType.getDescribingType()).getConstraint().getTag();
        Type unionTypeDescribingType = unionType.getDescribingType();
        if (unionTypeDescribingType.getTag() == TypeTags.UNION_TAG) {
            List<Type> memberTypes = ((UnionType) unionTypeDescribingType).getMemberTypes();
            return memberTypes.stream().anyMatch(memberType -> memberType.getTag() == targetTypeTag);
        }
        return false;
    }
}
