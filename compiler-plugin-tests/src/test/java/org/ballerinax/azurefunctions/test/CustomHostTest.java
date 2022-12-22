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
public class CustomHostTest {

    private static final Path SOURCE_DIR = Paths.get("src").resolve("test").resolve("resources");

    @Test
    public void testAzureFunctionsDeploymentProject() throws Exception {
        Path handlers = SOURCE_DIR.resolve("custom-host");
        Path depedenciesToml = handlers.resolve("Dependencies.toml");
        Files.deleteIfExists(depedenciesToml);
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(handlers, false);
        Assert.assertEquals(processOutput.getExitCode(), 0);
        Assert.assertTrue(processOutput.getStdOutput().contains("@azure_functions"));

        // check if the executable jar and the host.json files are in the generated zip file
        Path zipFilePath = handlers.resolve("target").resolve("azure_functions");
        Assert.assertTrue(Files.exists(zipFilePath));

        Assert.assertTrue(Files.exists(zipFilePath.resolve("custom_host.jar")));
        Assert.assertTrue(Files.exists(zipFilePath.resolve("host.json")));
        
        String hostContent = new String(Files.readAllBytes(zipFilePath.resolve("host.json")));
        if (!hostContent.contains("logging")) {
            Assert.fail("Custom host has not updated");
        }
        Files.deleteIfExists(depedenciesToml);
    }
}

