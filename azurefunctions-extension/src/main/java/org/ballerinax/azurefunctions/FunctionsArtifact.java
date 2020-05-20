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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the output artifact (.zip) generated for Azure Functions.
 */
public class FunctionsArtifact {

    private static final String HOST_JSON_NAME = "host.json";

    private Map<String, FunctionDeploymentContext> functions;

    private Path binaryPath;

    private JsonObject hostJson;

    private Gson gson = new Gson();

    public FunctionsArtifact(Map<String, FunctionDeploymentContext> functions, Path binaryPath) {
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

    private void generateHostJson() {
        this.hostJson = new JsonObject();
        this.hostJson.add("version", new JsonPrimitive("2.0"));
    }

    private InputStream jtos(JsonElement element) {
        try {
            return new ByteArrayInputStream(this.gson.toJson(element).getBytes(Constants.CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void generate(String outputFileName) throws IOException {
        Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + this.binaryPath.toAbsolutePath().getParent()
                .resolve(outputFileName).toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Files.copy(this.binaryPath, zipfs.getPath("/" + this.binaryPath.getFileName()), 
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(this.jtos(this.hostJson), zipfs.getPath("/" + HOST_JSON_NAME), 
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
