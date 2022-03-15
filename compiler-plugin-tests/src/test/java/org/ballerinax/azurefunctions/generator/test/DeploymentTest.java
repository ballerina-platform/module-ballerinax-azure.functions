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
package org.ballerinax.azurefunctions.generator.test;

import org.ballerinax.azurefunctions.generator.test.utils.ProcessOutput;
import org.ballerinax.azurefunctions.generator.test.utils.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Azure functions deployment test.
 */
public class DeploymentTest {

    private static final Path SOURCE_DIR = Paths.get("src").resolve("test").resolve("resources");
    
    @Test
    public void testAzureFunctionsDeploymentProject() throws Exception {
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(SOURCE_DIR.resolve("deployment"));
        Assert.assertEquals(processOutput.getExitCode(), 0);
        Assert.assertTrue(processOutput.getStdOutput().contains("@azure_functions"));
        
        // check if the executable jar and the host.json files are in the generated zip file
        Path zipFilePath = SOURCE_DIR.resolve("deployment").resolve("target").resolve("bin").resolve("azure-functions" +
                ".zip");
        Assert.assertTrue(Files.exists(zipFilePath));
        URI uri = URI.create("jar:file:" + zipFilePath.toUri().getPath());
        FileSystem zipfs = FileSystems.newFileSystem(uri, new HashMap<>());
        try {
            Path jarFile = zipfs.getPath("/deployment.jar");
            Path hostJson = zipfs.getPath("/host.json");
            Assert.assertTrue(Files.exists(jarFile));
            Assert.assertTrue(Files.exists(hostJson));
        } finally {
            zipfs.close();
        }
    }
}

