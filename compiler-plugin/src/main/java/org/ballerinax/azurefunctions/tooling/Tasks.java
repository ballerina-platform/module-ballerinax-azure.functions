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

import org.ballerinax.azurefunctions.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents tasks.json in the .vscode directory.
 *
 * @since 2201.3.0
 */
public class Tasks {

    private String version;
    private Tasks.Task[] tasks;

    public Tasks(boolean isNative) {
        this.version = "2.0.0";
        this.tasks = new Tasks.Task[1];
        this.tasks[0] = new Tasks.Task(isNative);
    }

    static class Task {

        private String type;
        private String command;
        private String problemMatcher;
        private boolean isBackground;
        private Map<String, String> options;

        public Task(boolean isNative) {
            this.type = "func";
            this.command = "host start";
            this.problemMatcher = "$func-watch";
            this.isBackground = true;
            this.options = new HashMap<>();
            if (isNative) {
                this.options.put("cwd", "${workspaceFolder}/" + Constants.LOCAL_ARTIFACT_PATH);
            } else {
                this.options.put("cwd", "${workspaceFolder}/" + Constants.ARTIFACT_PATH);
            }
        }
    }
}
