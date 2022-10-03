/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.azurefunctions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.ballerinax.azurefunctions.tooling.Extensions;
import org.ballerinax.azurefunctions.tooling.LocalSettings;
import org.ballerinax.azurefunctions.tooling.Settings;
import org.ballerinax.azurefunctions.tooling.Tasks;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Represents the output artifact (.zip) generated for Azure Functions.
 */
public class FunctionsArtifact {

    private static final String HOST_JSON_NAME = "host.json";
    private static final String FUNCTION_JSON_NAME = "function.json";

    private static final String VSCODE_DIRECTORY = ".vscode";
    private static final String GITIGNORE = ".gitignore";

    private Map<String, JsonObject> functions;

    private Path binaryPath;

    private JsonObject hostJson;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FunctionsArtifact(Map<String, JsonObject> functions, Path binaryPath) throws IOException {
        this.functions = functions;
        this.binaryPath = binaryPath;
        this.generateHostJson();
    }

    public Map<String, JsonObject> getFunctions() {
        return functions;
    }

    public Path getBinaryPath() {
        return binaryPath;
    }

    private JsonObject readExistingHostJson() throws IOException {
        File file = new File(HOST_JSON_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), Constants.CHARSET))) {
                JsonParser parser = new JsonParser();
                return parser.parse(reader).getAsJsonObject();
            }
        } else {
            return null;
        }
    }

    private void generateHostJson() throws IOException {
        this.hostJson = readExistingHostJson();
        if (this.hostJson == null) {
            this.hostJson = new JsonObject();
        }
        this.hostJson.add("version", new JsonPrimitive("2.0"));
        JsonObject httpWorker = new JsonObject();
        this.hostJson.add("customHandler", httpWorker);
        JsonObject httpWorkerDesc = new JsonObject();
        httpWorker.add("description", httpWorkerDesc);
        httpWorkerDesc.add("defaultExecutablePath", new JsonPrimitive("java"));
        Path fileName = this.binaryPath.getFileName();
        if (fileName != null) {
            httpWorkerDesc.add("defaultWorkerPath", new JsonPrimitive(fileName.toString()));
        }
        JsonArray workerArgs = new JsonArray();
        workerArgs.add("-jar");
        httpWorkerDesc.add("arguments", workerArgs);
        httpWorker.add("enableForwardingHttpRequest", new JsonPrimitive(false));
        JsonObject extensionBundle = new JsonObject();
        this.hostJson.add("extensionBundle", extensionBundle);
        extensionBundle.add("id", new JsonPrimitive("Microsoft.Azure.Functions.ExtensionBundle"));
        extensionBundle.add("version", new JsonPrimitive("[2.*, 3.0.0)"));
    }
    
    private InputStream jtos(Object element) {
        try {
            return new ByteArrayInputStream(this.gson.toJson(element).getBytes(Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void generate() throws IOException {
        // if an earlier generated file is there, delete it, or else
        // this will merge content to the earlier artifact
        if (this.binaryPath == null) {
            return;
        }
        Path parent = this.binaryPath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        Path targetDir = parent.getParent();
        if (targetDir == null) {
            return;
        }

        Path projectDir = targetDir.getParent();
        if (projectDir == null) {
            return;
        }
        generateVsCodeConfigs(projectDir);
        
        Path functionsDir = targetDir.resolve(Constants.FUNCTION_DIRECTORY);
        Files.createDirectories(functionsDir);
        Files.copy(this.binaryPath, functionsDir.resolve(this.binaryPath.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(this.jtos(this.hostJson), functionsDir.resolve(HOST_JSON_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        generateLocalSettings(functionsDir);
        for (Map.Entry<String, JsonObject> entry : this.functions.entrySet()) {
            Path functionDir = functionsDir.resolve(entry.getKey());
            Files.createDirectories(functionDir);
            Files.copy(this.jtos(entry.getValue()), functionDir.resolve(FUNCTION_JSON_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    private void generateLocalSettings(Path azureFunctionsDir) throws IOException {
        Files.copy(jtos(new LocalSettings()), azureFunctionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
    }
    
    private void generateVsCodeConfigs(Path projectDir) throws IOException {
        Path vsCodeDir = projectDir.resolve(VSCODE_DIRECTORY);
        Files.createDirectories(vsCodeDir);
        Files.copy(jtos(new Extensions()), vsCodeDir.resolve(Constants.EXTENSIONS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Settings()), vsCodeDir.resolve(Constants.SETTINGS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Tasks()), vsCodeDir.resolve(Constants.TASKS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        
        addToGitIgnore(projectDir);
    }

    private void addToGitIgnore(Path projectDir) throws IOException {
        Path gitIgnore = projectDir.resolve(GITIGNORE);
        if (!Files.exists(gitIgnore)) {
            return;
        }
        String gitIgnoreContent = Files.readString(gitIgnore);
        if (gitIgnoreContent.contains(VSCODE_DIRECTORY)) {
            return;
        }

        Files.write(gitIgnore, VSCODE_DIRECTORY.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }
}

