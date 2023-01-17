/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test cases to validate output validation types.
 *
 * @since 2201.3.0
 */
public class OutputTypeValidatorTests {

    protected static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/validations/");

    @Test
    public void outputBindingTypeTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("output").resolve("invalid-types"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Assert.assertEquals(diagnosticResult.errors().iterator().next().message(),
                "invalid return type for BlobOutput");
    }
    
    @Test
    public void invalidMultipleAzAnnotTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("output").resolve("multi-annot"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "multiple bindings not allowed for the parameter 'string'";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void emptyAzureReturnAnnotTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("output").resolve("empty-annot"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "binding annotation not found for the parameter 'string'";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }
}
