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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.azurefunctions.service;

import org.ballerinax.azurefunctions.Constants;

/**
 * Represents an Output Binding in Azure Functions.
 *
 * @since 2.0.0
 */
public abstract class OutputBinding extends Binding {
    public OutputBinding(String triggerType, int index) {
        super(triggerType, Constants.DIRECTION_OUT);
        if (index == 0) {
            this.setVarName(Constants.RETURN_VAR_NAME);
        } else {
            this.setVarName(Constants.RETURN_VAR_NAME + index);
        }
    }
}
