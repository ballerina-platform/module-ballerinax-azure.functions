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

import com.google.gson.JsonObject;

/**
 * Represents a binding in the functions.json. 
 * 
 * @since 2.0.0
 */
public abstract class Binding {
    private String triggerType;
    private String varName;
    private String direction;

    public Binding(String triggerType, String direction) {
        this.triggerType = triggerType;
        this.direction = direction;
    }

    public String getTriggerType() {
        return triggerType;
    }
    
    public String getVarName() {
        return varName;
    }

    public String getDirection() {
        return direction;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public abstract JsonObject getJsonObject();
}
