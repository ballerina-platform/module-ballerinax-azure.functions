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

import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.values.BString;

/**
 * The array type payload builder.
 *
 * @since SwanLake update 1
 */
public class ArrayBuilder extends AbstractPayloadBuilder {
    private final Type payloadType;

    public ArrayBuilder(Type payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public Object getValue(BString entity, boolean readonly) {
        Type elementType = ((ArrayType) payloadType).getElementType();
        if (elementType.getTag() == TypeTags.BYTE_TAG) {
            return new BinaryPayloadBuilder(payloadType).getValue(entity, readonly);
        }
        return new JsonPayloadBuilder(payloadType).getValue(entity, readonly);
    }
}
