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
package org.ballerinax.azurefunctions.test.utils;

import com.google.gson.Gson;
import io.ballerina.projects.Package;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinax.azurefunctions.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test utility class.
 */
public class TestUtils {

    private static final Log log = LogFactory.getLog(TestUtils.class);
    private static final Path DISTRIBUTION_PATH = Paths.get("../", "target", "ballerina-runtime")
            .toAbsolutePath();
    private static final Path BALLERINA_COMMAND = DISTRIBUTION_PATH.resolve("bin")
            .resolve((System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win") ?
                    "bal.bat" : "bal"));
    private static final String BUILD = "build";
    private static final String RUN = "run";
    private static final String EXECUTING_COMMAND = "Executing command: ";
    private static final String COMPILING = "Compiling: ";
    private static final String RUNNING = "Running: ";
    private static final String EXIT_CODE = "Exit code: ";
    private static final String JAVA_OPTS = "JAVA_OPTS";

    private static String logOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            br.lines().forEach(line -> {
                output.append(line);
                log.info(line);
            });
        }
        return output.toString();
    }

    /**
     * Compile a ballerina file in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput compileProject(Path sourceDirectory, boolean isNative, boolean failDocker,
                                               boolean isLocal, String fileName)
            throws InterruptedException, IOException {
        

        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        List<String> commands = new ArrayList<>(Arrays.asList(BALLERINA_COMMAND.toString(), BUILD, "--offline"));
        if (isNative) {
            commands.add("--native");
        }

        if (isLocal) {
            commands.add("--cloud=" + Constants.AZURE_FUNCTIONS_LOCAL_BUILD_OPTION);
        }
        
        if (fileName != null) {
            commands.add(fileName);
        }

        ProcessBuilder pb = new ProcessBuilder(commands);

        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
        if (failDocker) {
            environment.put("DOCKER_HOST", "tcp://192.168.59.103:2300");
        }
        log.info(COMPILING + sourceDirectory.normalize());
        log.debug(EXECUTING_COMMAND + commands);
        pb.directory(sourceDirectory.toFile());
        Process process = pb.start();
        ProcessOutput po = new ProcessOutput();
        po.setStdOutput(logOutput(process.getInputStream()));
        po.setErrOutput(logOutput(process.getErrorStream()));
        int exitCode = process.waitFor();

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }
        
        log.info(EXIT_CODE + exitCode);
        po.setExitCode(exitCode);
        return po;
    }

    private static synchronized void addJavaAgents(Map<String, String> envProperties) {
        String javaOpts = "";
        if (envProperties.containsKey(JAVA_OPTS)) {
            javaOpts = envProperties.get(JAVA_OPTS);
        }
        if (javaOpts.contains("jacocoAgentLine")) {
            return;
        }
        javaOpts = getJacocoAgentArgs() + javaOpts;
        envProperties.put(JAVA_OPTS, javaOpts);
    }

    private static String getJacocoAgentArgs() {
        String jacocoArgLine = System.getProperty("jacocoAgentLine");
        if (jacocoArgLine == null || jacocoArgLine.isEmpty()) {
            log.warn("Running integration test without jacoco test coverage");
            return "";
        }
        return jacocoArgLine + " ";
    }

    public static HostJson parseHostJson(Path hostJson) throws IOException {
        Gson gson = new Gson();
        return gson.fromJson(Files.readString(hostJson), HostJson.class);
    }

    /**
     * Represents Host.json file in tests.
     */
    public static class HostJson {

        public CustomHandler customHandler;

        /**
         * Represents custom handler in host.json.
         */
        public static class CustomHandler {

            public Description description;

            /**
             * Represents description in custom handler.
             */
            public static class Description {

                public String defaultExecutablePath;
                public String defaultWorkerPath;
            }
        }
    }
    public static Package loadPackage(Path projectDirPath) {
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }
    private static ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }
}
