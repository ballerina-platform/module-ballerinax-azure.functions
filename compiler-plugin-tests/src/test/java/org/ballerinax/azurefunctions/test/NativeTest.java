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
package org.ballerinax.azurefunctions.test;

import org.ballerinax.azurefunctions.test.utils.ProcessOutput;
import org.ballerinax.azurefunctions.test.utils.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Azure functions deployment test.
 */
public class NativeTest {

    private static final Path SOURCE_DIR = Paths.get("src").resolve("test").resolve("resources");

    @Test
    public void testNativeAzureFunctionsDeploymentProject() throws Exception {
        Path handlers = SOURCE_DIR.resolve("handlers");
        Path depedenciesToml = handlers.resolve("Dependencies.toml");
        Files.deleteIfExists(depedenciesToml);
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(handlers, true, false);
        Assert.assertEquals(processOutput.getExitCode(), 0);
        Assert.assertTrue(processOutput.getStdOutput().contains("@azure_functions"));

        // check if the executable jar and the host.json files are in the generated zip file
        Path target = handlers.resolve("target");
        Path azureFunctionsDir = target.resolve("azure_functions");
        Assert.assertTrue(Files.exists(azureFunctionsDir));

        Assert.assertTrue(Files.exists(azureFunctionsDir.resolve("azure_functions_tests")));
        Assert.assertTrue(Files.exists(azureFunctionsDir.resolve("host.json")));

        Path azureFunctionsLocalDir = target.resolve("azure_functions_local");
        Assert.assertTrue(Files.exists(azureFunctionsLocalDir));

        Assert.assertTrue(Files.exists(azureFunctionsLocalDir.resolve("azure_functions_tests")));
        Assert.assertTrue(Files.exists(azureFunctionsLocalDir.resolve("host.json")));
        Files.deleteIfExists(depedenciesToml);
    }

    @Test
    public void testNativeAzureFunctionsBuildFail() throws Exception {
        Path handlers = SOURCE_DIR.resolve("handlers");
        Path depedenciesToml = handlers.resolve("Dependencies.toml");
        Files.deleteIfExists(depedenciesToml);
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(handlers, true, true);
        Assert.assertEquals(processOutput.getExitCode(), 1);
        String stdOutput = processOutput.getStdOutput();
        String stdErr = processOutput.getErrOutput();
        Assert.assertTrue(stdOutput.contains("@azure_functions"));
        Assert.assertTrue(stdErr.contains("Native executable generation for cloud using docker failed"));
        Files.deleteIfExists(depedenciesToml);
    }
}
