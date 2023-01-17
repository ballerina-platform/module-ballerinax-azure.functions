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

package io.ballerina.stdlib.azure.functions.exceptions;

import io.ballerina.stdlib.azure.functions.Constants;

/**
 * Represents Bad Request related exceptions.
 *
 * @since 2.0.0
 */
public class UnsupportedTypeException extends RuntimeException {

    private String type;

    public UnsupportedTypeException() {
        super(Constants.UNSUPPORTED_TYPE_MESSAGE);
        this.type = Constants.UNSUPPORTED_TYPE_ERROR;
    }

    public String getType() {
        return type;
    }
}
