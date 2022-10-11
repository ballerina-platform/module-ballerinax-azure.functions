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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.langlib.array.FromBase64;

import java.util.List;

/**
 * The blob type payload builder.
 *
 * @since SwanLake update 1
 */
public class BinaryPayloadBuilder extends AbstractPayloadBuilder {
    private final Type payloadType;

    public BinaryPayloadBuilder(Type payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public Object getValue(BString entity, boolean readonly) {
        if (payloadType.getTag() == TypeTags.ARRAY_TAG) {
            Type elementType = ((ArrayType) payloadType).getElementType();
            if (elementType.getTag() == TypeTags.BYTE_TAG) {
                return FromBase64.fromBase64(entity);
            }
        } else if (payloadType.getTag() == TypeTags.UNION_TAG) {
            List<Type> memberTypes = ((UnionType) payloadType).getMemberTypes();
            for (Type memberType : memberTypes) {
                if (memberType.getTag() == TypeTags.ARRAY_TAG) {
                    Type elementType = ((ArrayType) memberType).getElementType();
                    if (elementType.getTag() == TypeTags.BYTE_TAG) {
                        return FromBase64.fromBase64(entity);
                    }
                }
            }
        }
        throw ErrorCreator.createError(StringUtils.fromString("incompatible type found: '" + payloadType.toString()));
    }
}
