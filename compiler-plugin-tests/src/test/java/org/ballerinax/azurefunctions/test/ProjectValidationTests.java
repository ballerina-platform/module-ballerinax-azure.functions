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
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ballerinax.azurefunctions.test.utils.TestUtils.loadPackage;

/**
 * Contains the project related validations of azure functions.
 *
 * @since 2.0.0
 */
public class ProjectValidationTests {

    protected static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/validations/");

    @Test
    public void headerAnnotationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("header-annotation"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
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

    @Test
    public void httpServiceConfigTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http-service-config"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.warnings().toArray();
        Assert.assertEquals(diagnosticResult.warningCount(), 2);
        String diagnosticMessage = "'treatNilableAsOptional' is the only @http:serviceConfig " +
                "field supported by Azure Function at the moment";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
        Assert.assertEquals(((Diagnostic) diagnostics[1]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void httpQueryRecordParamValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("query")
                .resolve("record-param"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "invalid type of query param 'name': expected one of the 'string', 'int', 'float'," +
                " 'boolean', 'decimal', 'map<json>' types or the array types of them";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void httpQueryMapNonJsonParamValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("query")
                .resolve("map-non-json-param"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "invalid type of query param 'name': expected one of the 'string', 'int', 'float'," +
                " 'boolean', 'decimal', 'map<json>' types or the array types of them";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void httpQueryArrayNonBasicParamValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("query")
                .resolve("array-non-basic-param"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "invalid type of query param 'name': expected one of the 'string', 'int', 'float'," +
                " 'boolean', 'decimal', 'map<json>' types or the array types of them";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void httpQueryArrayMapNonJsonParamValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("query")
                .resolve("array-map-non-json-param"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "invalid type of query param 'name': expected one of the 'string', 'int', 'float'," +
                " 'boolean', 'decimal', 'map<json>' types or the array types of them";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void httpQueryUnionParamValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("query")
                .resolve("union-param"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
//        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 6);
    }

    @Test
    public void httpAllowedFunctionTypesValidationTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("http").resolve("function-types"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
//        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
    }
    
    @Test
    public void validateInvalidCloudOptionTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("build-options")
                .resolve("invalid-cloud-option"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Object[] diagnostics = diagnosticResult.errors().toArray();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        String diagnosticMessage = "invalid 'cloud' build option specified. found 'azure_functions_test', expected " +
                "'azure_functions' or 'azure_functions_local'";
        Assert.assertEquals(((Diagnostic) diagnostics[0]).diagnosticInfo().messageFormat(), diagnosticMessage);
    }

    @Test
    public void testCodeModifierPayloadAnnotation() {
        Package currentPackage = loadPackage(RESOURCE_DIRECTORY.resolve("http/modifier-payload"));
        DiagnosticResult modifierDiagnosticResult = currentPackage.runCodeGenAndModifyPlugins();
        Assert.assertEquals(modifierDiagnosticResult.errorCount(), 0);
    }

    @Test
    public void testQueryAnnotation() {
        Package currentPackage = loadPackage(RESOURCE_DIRECTORY.resolve("http/query-annotation"));
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 12);
    }

    @Test
    public void testCodeModifierErrorTest() {
        Package currentPackage = loadPackage(RESOURCE_DIRECTORY.resolve("http/modifier-errors"));
        DiagnosticResult modifierDiagnosticResult = currentPackage.runCodeGenAndModifyPlugins();
        Assert.assertEquals(modifierDiagnosticResult.errorCount(), 5);
        assertTrue(modifierDiagnosticResult, 0, "ambiguous types for parameter 'a' and 'b'. Use " +
                "annotations to avoid ambiguity", "AF_017");
        assertTrue(modifierDiagnosticResult, 1, "ambiguous types for parameter 'c' and 'd'. Use " +
                "annotations to avoid ambiguity", "AF_017");
        assertTrue(modifierDiagnosticResult, 2, "ambiguous types for parameter 'e' and 'f'. Use " +
                "annotations to avoid ambiguity", "AF_017");
        assertTrue(modifierDiagnosticResult, 3, "invalid union type for default payload param: 'g'. " +
                "Use basic structured types", "AF_018");
        assertTrue(modifierDiagnosticResult, 4, "ambiguous types for parameter 'q' and 'p'. Use " +
                "annotations to avoid ambiguity", "AF_017");
    }

    private void assertTrue(DiagnosticResult diagnosticResult, int index, String message, String code) {
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[index];
        Assert.assertTrue(diagnostic.diagnosticInfo().messageFormat().contains(message));
        Assert.assertEquals(diagnostic.diagnosticInfo().code(), code);
    }
}
