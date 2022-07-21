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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BRefValue;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.converter.JsonToRecordConverter;
import org.ballerinalang.langlib.value.FromJsonString;
import org.ballerinalang.langlib.value.FromJsonWithType;

/**
 * The json type payload builder.
 *
 * @since SwanLake update 1
 */
public class JsonPayloadBuilder extends AbstractPayloadBuilder {
    private final Type payloadType;

    public JsonPayloadBuilder(Type payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public Object getValue(BString dataSource, boolean readonly) {
        // Following can be removed based on the solution of
        // https://github.com/ballerina-platform/ballerina-lang/issues/35780
        Object obj = FromJsonString.fromJsonString(dataSource);
        if (isSubtypeOfAllowedType(payloadType, TypeTags.RECORD_TYPE_TAG)) {
            return JsonToRecordConverter.convert(payloadType, obj, readonly);
        }
//        Object bjson = EntityBodyHandler.constructJsonDataSource(null);
//        EntityBodyHandler.addJsonMessageDataSource(null, bjson);
//        var result = FromJsonWithType.fromJsonWithType(bjson, ValueCreator.createTypedescValue(payloadType));
        var result = FromJsonWithType.fromJsonWithType(obj, ValueCreator.createTypedescValue(payloadType));
        if (result instanceof BError) {
            throw (BError) result;
        }
        if (readonly && result instanceof BRefValue) {
            ((BRefValue) result).freezeDirect();
        }
        return result;
    }
}
