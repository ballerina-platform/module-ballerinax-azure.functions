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
package org.ballerinax.azurefunctions.tooling;

import com.google.gson.annotations.SerializedName;
import org.ballerinax.azurefunctions.Constants;

/**
 * Represents settings.json in the .vscode directory.
 *
 * @since 2201.3.0
 */
public class Settings {

    @SerializedName("azureFunctions.deploySubpath")
    private String deploySubpath;

    @SerializedName("azureFunctions.projectLanguage")
    private String projectLanguage;

    @SerializedName("azureFunctions.projectRuntime")
    private String projectRuntime;

    @SerializedName("debug.internalConsoleOptions")
    private String internalConsoleOptions;

    public Settings() {
        this.deploySubpath = Constants.ARTIFACT_PATH;
        this.projectLanguage = "Custom";
        this.projectRuntime = "~4";
        this.internalConsoleOptions = "neverOpen";
    }
}
