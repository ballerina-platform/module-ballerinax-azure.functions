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
package io.ballerina.stdlib.azure.functions.bindings.input;

import io.ballerina.runtime.api.types.Type;
import io.ballerina.stdlib.azure.functions.builder.AbstractPayloadBuilder;
import io.ballerina.stdlib.azure.functions.builder.JsonPayloadBuilder;

/**
 * Represents the CosmosDB Input in Azure functions input binding.
 * 
 * @since 2.0.0
 */
public class CosmosInput extends InputBinding {

    public CosmosInput() {
        super("CosmosDBInput");
    }

    @Override
    public AbstractPayloadBuilder getPayloadBuilder(Type type) {
        return new JsonPayloadBuilder(type);
    }
}
