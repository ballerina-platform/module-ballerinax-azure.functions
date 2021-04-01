/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.ballerinax.azurefunctions.test.utils.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains all the tests that mocks azure function like http calls.
 *
 * @since 2.0.0
 */
public class RuntimeTest {

    private static final Path SOURCE_DIR =
            Paths.get("src").resolve("test").resolve("resources").resolve("handlers").resolve("code");

    @Test
    public void helloRuntimeTest() {
        try {
            String returnMessage = TestUtils.runBallerinaProject(SOURCE_DIR);
            Assert.assertEquals(returnMessage, "{\"Outputs\":{}, \"Logs\":[], \"ReturnValue\":\"Hello, Jack!\"}");
        } catch (InterruptedException | IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
