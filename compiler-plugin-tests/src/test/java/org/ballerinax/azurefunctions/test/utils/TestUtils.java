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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Test utility class.
 */
public class TestUtils {

    private static final Log log = LogFactory.getLog(TestUtils.class);
    private static final Path DISTRIBUTION_PATH = Paths.get(FilenameUtils.separatorsToSystem(
            System.getProperty("ballerina.home"))).resolve("bin");
    private static final Path BALLERINA_COMMAND = DISTRIBUTION_PATH
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
     * Compile a ballerina project in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput compileBallerinaProject(Path sourceDirectory) throws InterruptedException,
            IOException {

        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND.toString(), BUILD);
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
        log.info(COMPILING + sourceDirectory);
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        ProcessOutput po = new ProcessOutput();
        log.info(EXIT_CODE + exitCode);
        po.setExitCode(exitCode);
        po.setStdOutput(logOutput(process.getInputStream()));
        po.setErrOutput(logOutput(process.getErrorStream()));
        return po;
    }

    /**
     * Compile a ballerina file in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling
     * @throws IOException          if an error occurs while writing file
     */
    public static ProcessOutput compileBallerinaProject(Path sourceDirectory) throws InterruptedException,
            IOException {

        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND.toString(), BUILD);
        Map<String, String> environment = pb.environment();
        addJavaAgents(environment);
        log.info(COMPILING + sourceDirectory.normalize());
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Process process = pb.start();
        int exitCode = process.waitFor();

        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        ProcessOutput po = new ProcessOutput();
        log.info(EXIT_CODE + exitCode);
        po.setExitCode(exitCode);
        po.setStdOutput(logOutput(process.getInputStream()));
        po.setErrOutput(logOutput(process.getErrorStream()));
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

    /**
     * Compile a run ballerina project in a given directory.
     *
     * @param sourceDirectory Ballerina source directory
     * @return Exit code
     * @throws InterruptedException if an error occurs while compiling or running
     * @throws IOException          if an error occurs while writing file
     */
    public static String runBallerinaProject(Path sourceDirectory) throws InterruptedException, 
            IOException {
        Path ballerinaInternalLog = Paths.get(sourceDirectory.toAbsolutePath().toString(), "ballerina-internal.log");
        if (ballerinaInternalLog.toFile().exists()) {
            log.warn("Deleting already existing ballerina-internal.log file.");
            FileUtils.deleteQuietly(ballerinaInternalLog.toFile());
        }

        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND.toString(), RUN);
        log.info(COMPILING + sourceDirectory.normalize());
        log.debug(EXECUTING_COMMAND + pb.command());
        pb.directory(sourceDirectory.toFile());
        Map<String, String> environment = pb.environment();
        int port = generateRandomPort();
        environment.put("FUNCTIONS_CUSTOMHANDLER_PORT", Integer.toString(port));
        Process process = pb.start();
        Thread.sleep(30000);
        String returnValue = invokeFunction(sourceDirectory.getParent().resolve("request.json"), port);
        process.destroy();
        // log ballerina-internal.log content
        if (Files.exists(ballerinaInternalLog)) {
            log.info("ballerina-internal.log file found. content: ");
            log.info(FileUtils.readFileToString(ballerinaInternalLog.toFile(), Charset.defaultCharset()));
        }

        return returnValue;
    }

    private static String invokeFunction(Path payload, int port) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofFile(payload))
                .uri(URI.create("http://localhost:" + port + "/hello"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static int generateRandomPort() {
        Random r = new Random();
        int low = 1000;
        int high = 10000;
        return r.nextInt(high - low) + low;
    }
}
