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
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the output artifact (.zip) generated for Azure Functions.
 */
public class FunctionsArtifact {

    private static final String HOST_JSON_NAME = "host.json";
    private static final String FUNCTION_JSON_NAME = "function.json";

    private static final String VSCODE_DIRECTORY = ".vscode";
    private static final String GITIGNORE = ".gitignore";

    private Map<String, JsonObject> functions;
    private Path jarPath;
    private boolean isNative;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public FunctionsArtifact(Map<String, JsonObject> functions, Path jarPath, boolean isNative) throws IOException {
        this.functions = functions;
        this.jarPath = jarPath;
        this.isNative = isNative;
    }

    public Map<String, JsonObject> getFunctions() {
        return functions;
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

    private JsonObject generateHostJson() throws IOException {
        JsonObject hostJson = readExistingHostJson();
        if (hostJson == null) {
            hostJson = new JsonObject();
        }
        hostJson.add("version", new JsonPrimitive("2.0"));
        JsonObject extensions = new JsonObject();
        JsonObject http = new JsonObject();
        http.addProperty("routePrefix", "");
        extensions.add("http", http);
        hostJson.add("extensions", extensions);
        JsonObject httpWorker = new JsonObject();
        hostJson.add("customHandler", httpWorker);
        JsonObject httpWorkerDesc = new JsonObject();
        httpWorker.add("description", httpWorkerDesc);
        httpWorkerDesc.add("defaultExecutablePath", new JsonPrimitive("java"));
        Path fileName = this.jarPath.getFileName();
        if (fileName != null) {
            httpWorkerDesc.add("defaultWorkerPath", new JsonPrimitive(fileName.toString()));
        }
        JsonArray workerArgs = new JsonArray();
        workerArgs.add("-jar");
        httpWorkerDesc.add("arguments", workerArgs);
        httpWorker.add("enableForwardingHttpRequest", new JsonPrimitive(false));
        JsonObject extensionBundle = new JsonObject();
        hostJson.add("extensionBundle", extensionBundle);
        extensionBundle.add("id", new JsonPrimitive("Microsoft.Azure.Functions.ExtensionBundle"));
        extensionBundle.add("version", new JsonPrimitive("[3.*, 4.0.0)"));
        return hostJson;
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
        if (this.jarPath == null) {
            return;
        }
        Path parent = this.jarPath.toAbsolutePath().getParent();
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
        generateVsCodeConfigs(projectDir, isNative);

        Path functionsDir = targetDir.resolve(Constants.FUNCTION_DIRECTORY);
        Optional<String> cachedLocalSettings = cacheLocalSettings(functionsDir);
        deleteDirectory(functionsDir);
        Files.createDirectories(functionsDir);
        Path jarFileName = this.jarPath.getFileName();
        Path azureFunctionsJar = functionsDir.resolve(jarFileName);
        Files.copy(this.jarPath, azureFunctionsJar, StandardCopyOption.REPLACE_EXISTING);
        if (isNative) {
            buildImage(functionsDir, jarFileName.toString());
            Files.deleteIfExists(functionsDir.resolve(azureFunctionsJar));
        }
        Files.copy(this.jtos(this.generateHostJson()), functionsDir.resolve(HOST_JSON_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        if (cachedLocalSettings.isEmpty()) {
            generateLocalSettings(functionsDir);
        } else {
            String localSettings = cachedLocalSettings.get();
            ByteArrayInputStream inStream =
                    new ByteArrayInputStream(localSettings.getBytes(StandardCharsets.UTF_8));
            Files.copy(inStream, functionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        for (Map.Entry<String, JsonObject> entry : this.functions.entrySet()) {
            Path functionDir = functionsDir.resolve(entry.getKey());
            Files.createDirectories(functionDir);
            Files.copy(this.jtos(entry.getValue()), functionDir.resolve(FUNCTION_JSON_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void buildImage(Path azureFunctionsDir, String jarFileName) {
        String executableName = jarFileName.replaceFirst(".jar", "");
        ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", "-it", "-v",
                azureFunctionsDir.toAbsolutePath() + ":/app/build/temp", "azure_builder", jarFileName, executableName);
        pb.inheritIO();

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("docker build failed. refer to the build log");
            }

        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new RuntimeException();
        }
    }

    private void deleteDirectory(Path azureFunctionsDir) throws IOException {
        if (azureFunctionsDir.toFile().exists()) {
            Files.walk(azureFunctionsDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void generateLocalSettings(Path azureFunctionsDir) throws IOException {
        Files.copy(jtos(new LocalSettings()), azureFunctionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private Optional<String> cacheLocalSettings(Path azureFunctionsDir) throws IOException {
        Path localSettingsPath = azureFunctionsDir.resolve(Constants.SETTINGS_LOCAL_FILE_NAME);
        if (localSettingsPath.toFile().exists()) {
            return Optional.of(Files.readString(localSettingsPath));
        }
        return Optional.empty();
    }

    private void generateVsCodeConfigs(Path projectDir, boolean isNative) throws IOException {
        Path vsCodeDir = projectDir.resolve(VSCODE_DIRECTORY);
        Files.createDirectories(vsCodeDir);
        Files.copy(jtos(new Extensions()), vsCodeDir.resolve(Constants.EXTENSIONS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Settings(isNative)), vsCodeDir.resolve(Constants.SETTINGS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Tasks(isNative)), vsCodeDir.resolve(Constants.TASKS_FILE_NAME),
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

