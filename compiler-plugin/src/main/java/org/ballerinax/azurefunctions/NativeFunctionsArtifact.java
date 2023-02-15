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
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.ProjectKind;
import org.ballerinax.azurefunctions.tooling.Extensions;
import org.ballerinax.azurefunctions.tooling.Settings;
import org.ballerinax.azurefunctions.tooling.Tasks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

import static io.ballerina.projects.util.ProjectConstants.BIN_DIR_NAME;
import static io.ballerina.projects.util.ProjectConstants.DOT;

/**
 * Responsible for generating artifacts for native executables.
 */
public class NativeFunctionsArtifact extends FunctionsArtifact {
    private static final PrintStream OUT = System.out;

    public NativeFunctionsArtifact(Map<String, JsonObject> functions, Path jarPath, Project project) {
        super(functions, jarPath, project);
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
    protected JsonObject generateHostJson() throws IOException {
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
        String cloudBuildOption = Util.getCloudBuildOption(project);
        if (!cloudBuildOption.equals(Constants.AZURE_FUNCTIONS_BUILD_OPTION)) {
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
        String cloudBuildOption = Util.getCloudBuildOption(project);
        if (cloudBuildOption.equals(Constants.AZURE_FUNCTIONS_BUILD_OPTION)) {
            buildRemoteArtifacts(functionsDir, jarFileNameString);
        } else {
            buildLocalArtifacts(functionsDir, jarFileNameString);
        }
        Files.deleteIfExists(functionsDir.resolve(azureFunctionsJar));
    }

    @Override
    protected void generateVsCodeConfigs(Path projectDir) throws IOException {
        Path vsCodeDir = projectDir.resolve(Constants.VSCODE_DIRECTORY);
        Files.createDirectories(vsCodeDir);
        Files.copy(jtos(new Extensions()), vsCodeDir.resolve(Constants.EXTENSIONS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Settings(project)), vsCodeDir.resolve(Constants.SETTINGS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(jtos(new Tasks(project)), vsCodeDir.resolve(Constants.TASKS_FILE_NAME),
                StandardCopyOption.REPLACE_EXISTING);

        addToGitIgnore(projectDir);
    }
    public void buildLocalArtifacts(Path azureFunctionsDir, String jarFileName) {
        OUT.println("\n\t@azure_functions: Building native executable compatible for the local operating system." +
                "This may take a while.\n");
        Path jarPath = azureFunctionsDir.resolve(jarFileName);
        String nativeImageName;
        String[] command;
        String nativeImageCommand = System.getenv("GRAALVM_HOME");

        if (nativeImageCommand == null) {
            throw new ProjectException("GraalVM installation directory not found. Set GRAALVM_HOME as an " +
                    "environment variable\nHINT: To install GraalVM, follow the link: " +
                    "https://ballerina.io/learn/build-a-native-executable/#configure-graalvm");
        }
        String os = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        nativeImageCommand += File.separator + BIN_DIR_NAME + File.separator
                + (os.contains("win") ? "native-image.cmd" : "native-image");

        File commandExecutable = Paths.get(nativeImageCommand).toFile();
        if (!commandExecutable.exists()) {
            throw new ProjectException("cannot find '" + commandExecutable.getName() + "' in the GRAALVM_HOME. " +
                    "Install it using: gu install native-image");
        }

        if (project.kind().equals(ProjectKind.SINGLE_FILE_PROJECT)) {
            String fileName = project.sourceRoot().toFile().getName();
            nativeImageName = fileName.substring(0, fileName.lastIndexOf(DOT));
            command = new String[] {
                    nativeImageCommand,
                    "-jar",
                    jarPath.toString(),
                    "-H:Path=" + azureFunctionsDir,
                    "-H:Name=" + nativeImageName,
                    "--no-fallback"
            };
        } else {
            nativeImageName = project.currentPackage().packageName().toString();
            command = new String[]{
                    nativeImageCommand,
                    "-jar",
                    jarPath.toString(),
                    "-H:Name=" + nativeImageName,
                    "-H:Path=" + azureFunctionsDir,
                    "--no-fallback"
            };
        }

        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            builder.inheritIO();
            Process process = builder.start();

            if (process.waitFor() != 0) {
                throw new ProjectException("unable to create native image");
            }
        } catch (IOException e) {
            throw new ProjectException("unable to create native image : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void buildRemoteArtifacts(Path azureFunctionsDir, String jarFileName) {
        OUT.println("\n\t@azure_functions: Building native image compatible for the Cloud using Docker. " +
                "This may take a while.\n");
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
