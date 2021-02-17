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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the output artifact (.zip) generated for Azure Functions.
 */
public class FunctionsArtifact {

    private static final String HOST_JSON_NAME = "host.json";

    private static final String FUNCTION_JSON_NAME = "function.json";

    private Map<String, FunctionDeploymentContext> functions;

    private Path binaryPath;

    private JsonObject hostJson;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FunctionsArtifact(Map<String, FunctionDeploymentContext> functions, Path binaryPath) throws IOException {
        this.functions = functions;
        this.binaryPath = binaryPath;
        this.generateHostJson();
    }

    public Map<String, FunctionDeploymentContext> getFunctions() {
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
        extensionBundle.add("version", new JsonPrimitive("[1.*, 2.0.0)"));
    }

    private InputStream jtos(JsonElement element) {
        try {
            return new ByteArrayInputStream(this.gson.toJson(element).getBytes(Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void generate(String outputFileName) throws IOException {
        // if an earlier generated file is there, delete it, or else
        // this will merge content to the earlier artifact
        Files.deleteIfExists(Paths.get(outputFileName));
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        Path parent = this.binaryPath.toAbsolutePath().getParent();
        if (parent != null) {
            URI uri = URI.create("jar:file:" + parent.resolve(outputFileName).toUri().getPath());
            if (this.binaryPath != null) {
                try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
                    Files.copy(this.binaryPath, zipfs.getPath("/" + this.binaryPath.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(this.jtos(this.hostJson), zipfs.getPath("/" + HOST_JSON_NAME),
                            StandardCopyOption.REPLACE_EXISTING);
                    for (Map.Entry<String, FunctionDeploymentContext> entry : this.functions.entrySet()) {
                        Path functionDir = zipfs.getPath("/" + entry.getKey());
                        Files.createDirectory(functionDir);
                        Files.copy(this.jtos(entry.getValue().getFunctionDefinition()),
                                functionDir.resolve(FUNCTION_JSON_NAME),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

}
