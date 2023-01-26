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
package org.ballerinax.azurefunctions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.ballerinax.azurefunctions.tooling.Extensions;
import org.ballerinax.azurefunctions.tooling.Settings;
import org.ballerinax.azurefunctions.tooling.Tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

/**
 * Responsible for generating artifacts for native executables.
 */
public class NativeFunctionsArtifact extends FunctionsArtifact {

    public NativeFunctionsArtifact(Map<String, JsonObject> functions, Path jarPath) {
        super(functions, jarPath);
    }

    @Override
    public void generate() throws IOException {
        super.generate();
        Optional<Path> targetDir = getTargetDir();
        if (targetDir.isPresent()) {
            createLocalArtifactDirectory(targetDir.get());
        }
    }

    private void createLocalArtifactDirectory(Path targetDir) throws IOException {
        Path from = targetDir.resolve(Constants.FUNCTION_DIRECTORY);
        Path destination = targetDir.resolve(Constants.LOCAL_FUNCTION_DIRECTORY);
        Util.deleteDirectory(destination);
        Util.copyFolder(from, destination);
        String executableName = getLocalExecutableFileName();
        Path executablePath = destination.resolve(executableName);
        Files.deleteIfExists(executablePath);
        Files.deleteIfExists(destination.resolve(getExecutableFileName()));
        Path originalExecutable = targetDir.resolve("bin").resolve(executableName);
        Files.copy(originalExecutable, destination.resolve(executableName));
        Files.copy(this.jtos(this.generateHostJson(true)), destination.resolve(Constants.HOST_JSON_NAME),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private String getLocalExecutableFileName() {
        Path fileName = this.jarPath.getFileName();
        return fileName.toString().replaceFirst(".jar", Util.getExecutableExtension());
    }

    private String getExecutableFileName() {
        Path fileName = this.jarPath.getFileName();
        return fileName.toString().replaceFirst(".jar", "");
    }

    @Override
    protected JsonObject generateHostJson(boolean isLocal) throws IOException {
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
        String execName = "";
        if (isLocal) {
            execName = getLocalExecutableFileName();
        } else {
            execName = getExecutableFileName();
        }
        httpWorkerDesc.add("defaultExecutablePath", new JsonPrimitive(execName));
        httpWorkerDesc.add("workingDirectory", new JsonPrimitive(""));
        JsonArray workerArgs = new JsonArray();
        httpWorkerDesc.add("arguments", workerArgs);
        httpWorker.add("enableForwardingHttpRequest", new JsonPrimitive(false));
        JsonObject extensionBundle = new JsonObject();
        hostJson.add("extensionBundle", extensionBundle);
        extensionBundle.add("id", new JsonPrimitive("Microsoft.Azure.Functions.ExtensionBundle"));
        extensionBundle.add("version", new JsonPrimitive("[3.*, 4.0.0)"));
        return hostJson;
    }

    @Override
    protected void generateExecutable(Path functionsDir) throws IOException {
        Path jarFileName = this.jarPath.getFileName();
        Path azureFunctionsJar = functionsDir.resolve(jarFileName);
        Files.copy(this.jarPath, azureFunctionsJar, StandardCopyOption.REPLACE_EXISTING);
        String jarFileNameString = jarFileName.toString();
        buildImage(functionsDir, jarFileNameString);
        Files.deleteIfExists(functionsDir.resolve(azureFunctionsJar));
    }

    @Override
    protected void generateVsCodeConfigs(Path projectDir) throws IOException {
        Path vsCodeDir = projectDir.resolve(Constants.VSCODE_DIRECTORY);
        Files.createDirectories(vsCodeDir);
        Files.copy(jtos(new Extensions()), vsCodeDir.resolve(Constants.EXTENSIONS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Settings(true)), vsCodeDir.resolve(Constants.SETTINGS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Tasks(true)), vsCodeDir.resolve(Constants.TASKS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);

        addToGitIgnore(projectDir);
    }

    public void buildImage(Path azureFunctionsDir, String jarFileName) {
        String executableName = getExecutableFileName();
        String volumeMount = azureFunctionsDir.toAbsolutePath() + Constants.CONTAINER_OUTPUT_PATH;
        ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", Constants.DOCKER_PLATFORM_FLAG,
                Constants.AZURE_REMOTE_COMPATIBLE_ARCHITECTURE, "-v", volumeMount, Constants.NATIVE_BUILDER_IMAGE,
                jarFileName, executableName);
        pb.inheritIO();

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new DockerBuildException(
                        "Native executable generation for cloud using docker failed with exit code " + exitCode +
                                ". Refer to the above build log for information");
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new DockerBuildException(
                    "Native executable generation for cloud using docker failed. Refer to the above build log for " +
                            "information");
        }
    }
}

