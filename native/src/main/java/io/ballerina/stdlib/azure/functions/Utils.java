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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.exceptions.InvalidPayloadException;
import io.ballerina.stdlib.azure.functions.exceptions.PayloadNotFoundException;
import org.ballerinalang.langlib.bool.FromString;

import java.util.List;

import static io.ballerina.runtime.api.TypeTags.BOOLEAN_TAG;
import static io.ballerina.runtime.api.TypeTags.DECIMAL_TAG;
import static io.ballerina.runtime.api.TypeTags.FLOAT_TAG;
import static io.ballerina.runtime.api.TypeTags.INT_TAG;
import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * Contains Utilities related to natives.
 *
 * @since 2.0.0
 */
public class Utils {

    private static final ArrayType INT_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_INT);
    private static final ArrayType FLOAT_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_FLOAT);
    private static final ArrayType BOOLEAN_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_BOOLEAN);
    private static final ArrayType DECIMAL_ARR = TypeCreator.createArrayType(PredefinedTypes.TYPE_DECIMAL);

    public static BError createError(Module module, String message, String type) {
        BString errorMessage = fromString(message);
        return ErrorCreator.createError(module, type, errorMessage, ErrorCreator.createError(errorMessage), null);
    }

    public static Object createValue(Type type, BString strValue) {
        switch (type.getTag()) {
            case TypeTags.STRING_TAG:
                return strValue;
            case TypeTags.BOOLEAN_TAG:
                return FromString.fromString(strValue);
            case TypeTags.INT_TAG:
                return org.ballerinalang.langlib.integer.FromString.fromString(strValue);
            case TypeTags.FLOAT_TAG:
                return org.ballerinalang.langlib.floatingpoint.FromString.fromString(strValue);
            case TypeTags.DECIMAL_TAG:
                return org.ballerinalang.langlib.decimal.FromString.fromString(strValue);
            case TypeTags.UNION_TAG:
                List<Type> memberTypes = ((UnionType) type).getMemberTypes();
                for (Type memberType : memberTypes) {
                    try {
                        return createValue(memberType, strValue);
                    } catch (BError ignored) {
                        // thrown errors are ignored until all the types are iterated
                    }
                }
                return null;
            case TypeTags.ARRAY_TAG:
                ArrayType arrayType = (ArrayType) type;
                Type elementType = arrayType.getElementType();
                if (strValue == null) {
                    return null;
                }
                String[] values = strValue.getValue().split(",");
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

    public static boolean isNilType(Type type) {
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

    public static BString getRequestBody(BMap<?, ?> httpPayload, String name, Type type)
            throws PayloadNotFoundException {
        BString bBody = StringUtils.fromString(Constants.AZURE_BODY_HEADERS);
        if (httpPayload.containsKey(bBody)) {
            return httpPayload.getStringValue(bBody);
        }
        if (!isNilType(type)) {
            throw new PayloadNotFoundException("payload not found for the variable '" + name + "'");
        }
        return null;
    }

    public static String getContentTypeHeader(BMap<?, ?> headers) {
        //TODO fix lower case
        if (headers.containsKey(StringUtils.fromString(Constants.CONTENT_TYPE))) {
            BArray headersArrayValue = headers.getArrayValue(StringUtils.fromString(Constants.CONTENT_TYPE));
            return headersArrayValue.getBString(0).getValue();
        } else {
            return null;
        }
    }

    public static boolean isAzAnnotationExist(Object annotation) {
        if (annotation == null) {
            return false;
        }
        return true;
    }
}
