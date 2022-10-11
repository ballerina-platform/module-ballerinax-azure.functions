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

package io.ballerina.stdlib.azure.functions.converter;

import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BRefValue;
import org.ballerinalang.langlib.value.CloneWithType;

/**
 * The converter binds the JSON payload to a record.
 *
 * @since SwanLake update 1
 */
public class JsonToRecordConverter {

    public static Object convert(Type type, Object entity, boolean readonly) {
        Object recordEntity = getRecordEntity(entity, type);
        if (readonly && recordEntity instanceof BRefValue) {
            ((BRefValue) recordEntity).freezeDirect();
        }
        return recordEntity;
    }

    private static Object getRecordEntity(Object entity, Type entityBodyType) {
        Object result = getRecord(entityBodyType, entity);
        if (result instanceof BError) {
            throw (BError) result;
        }
        return result;
    }

    /**
     * Convert a json to the relevant record type.
     *
     * @param entityBodyType Represents entity body type
     * @param bjson          Represents the json value that needs to be converted
     * @return the relevant ballerina record or object
     */
    private static Object getRecord(Type entityBodyType, Object bjson) {
        try {
            return CloneWithType.convert(entityBodyType, bjson);
        } catch (NullPointerException ex) {
            throw new RuntimeException("cannot convert payload to record type: " +
                                                          entityBodyType.getName());
        }
    }
    private JsonToRecordConverter() {

    }
}
