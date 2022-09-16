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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Contains the project related validations of azure functions.
 *
 * @since 2.0.0
 */
public class ProjectValidationTests {

    protected static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/validations/");

    @Test
    public void headerAnnotationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http-header-annotation"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object [] diagnostics =  diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 13);
        String diagnosticMessage0 = "invalid annotation type on param 'a'";
        String diagnosticMessage1 = "invalid union type of header param 'xRate': one of the 'string','int','float'," +
                "'decimal','boolean' types, an array of the above types or a record which consists of the above " +
                "types can only be union with '()'. Eg: string|() or string[]|()";
        String diagnosticMessage2 = "invalid type of header param 'abc': One of the following types is expected: " +
                "'string','int','float','decimal','boolean', an array of the above types or a record which consists " +
                "of the above types";
        String diagnosticMessage3 = "invalid union type of header param 'abc': one of the 'string','int','float'," +
                "'decimal','boolean' types, an array of the above types or a record which consists of the above types "
                + "can only be union with '()'. Eg: string|() or string[]|()";
        String diagnosticMessage4 = "invalid union type of header param 'abc': one of the 'string','int','float'," +
                "'decimal','boolean' types, an array of the above types or a record which consists of the above " +
                "types can only be union with '()'. Eg: string|() or string[]|()";
        String diagnosticMessage5 = "rest fields are not allowed for header binding records. " +
                "Use 'http:Headers' type to access all headers";
        String diagnosticMessage6 = "rest fields are not allowed for header binding records. " +
                "Use 'http:Headers' type to access all headers";
        String diagnosticMessage7 = "invalid type of header param 'abc': One of the following types is expected: " +
                "'string','int','float','decimal','boolean', an array of the above types or a record which " +
                "consists of the above types";
        String diagnosticMessage8 = "invalid multiple resource parameter annotations for 'abc'";
        String diagnosticMessage9 = "invalid type of header param 'abc': One of the following types is expected: " +
                "'string','int','float','decimal','boolean', an array of the above types or a record which " +
                "consists of the above types";
        String diagnosticMessage10 = "invalid union type of header param 'abc': one of the 'string','int','float'," +
                "'decimal','boolean' types, an array of the above types or a record which consists of the " +
                "above types can only be union with '()'. Eg: string|() or string[]|()";
        String diagnosticMessage11 = "invalid type of header param 'abc': One of the following types is expected: " +
                "'string','int','float','decimal','boolean', an array of the above types or a record which " +
                "consists of the above types";
        String diagnosticMessage12 = "invalid union type of header param 'abc': one of the 'string','int','float'," +
                "'decimal','boolean' types, an array of the above types or a record which consists of " +
                "the above types can only be union with '()'. Eg: string|() or string[]|()";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage0);
        Assert.assertEquals(((Diagnostic) diagnostics[1]).diagnosticInfo().messageFormat(), diagnosticMessage1);
        Assert.assertEquals(((Diagnostic) diagnostics[2]).diagnosticInfo().messageFormat(), diagnosticMessage2);
        Assert.assertEquals(((Diagnostic) diagnostics[3]).diagnosticInfo().messageFormat(), diagnosticMessage3);
        Assert.assertEquals(((Diagnostic) diagnostics[4]).diagnosticInfo().messageFormat(), diagnosticMessage4);
        Assert.assertEquals(((Diagnostic) diagnostics[5]).diagnosticInfo().messageFormat(), diagnosticMessage5);
        Assert.assertEquals(((Diagnostic) diagnostics[6]).diagnosticInfo().messageFormat(), diagnosticMessage6);
        Assert.assertEquals(((Diagnostic) diagnostics[7]).diagnosticInfo().messageFormat(), diagnosticMessage7);
        Assert.assertEquals(((Diagnostic) diagnostics[8]).diagnosticInfo().messageFormat(), diagnosticMessage8);
        Assert.assertEquals(((Diagnostic) diagnostics[9]).diagnosticInfo().messageFormat(), diagnosticMessage9);
        Assert.assertEquals(((Diagnostic) diagnostics[10]).diagnosticInfo().messageFormat(), diagnosticMessage10);
        Assert.assertEquals(((Diagnostic) diagnostics[11]).diagnosticInfo().messageFormat(), diagnosticMessage11);
        Assert.assertEquals(((Diagnostic) diagnostics[12]).diagnosticInfo().messageFormat(), diagnosticMessage12);
    }
}
