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
package org.ballerinax.azurefunctions;

import io.ballerina.projects.CodeGeneratorResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import org.ballerinalang.core.util.exceptions.BallerinaException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Contains the code generation part of the azure functions.
 * 
 * @since 2.0.0
 */
public class AzureCodeGeneratedTask  implements CompilerLifecycleTask<CompilerLifecycleEventContext> {
    private static final PrintStream OUT = System.out;
    
    @Override
    public void perform(CompilerLifecycleEventContext compilerLifecycleEventContext) {
        OUT.println("Code Gen task executed");
        Map<String, FunctionDeploymentContext> generatedFunctions =
                AzureFunctionHolder.getInstance().getGeneratedFunctions();
        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        OUT.println("\t@azure_functions:Function: " + String.join(", ", generatedFunctions.keySet()));
        Optional<Path> generatedArtifactPath = compilerLifecycleEventContext.getGeneratedArtifactPath();
        generatedArtifactPath.ifPresent(path -> {
            try {
                this.generateFunctionsArtifact(generatedFunctions, path);
            } catch (IOException e) {
                String msg = "Error generating Azure Functions: " + e.getMessage();
                OUT.println(msg);
                throw new BallerinaException(msg, e);
            }
            OUT.println("\n\tExecute the below command to deploy Ballerina Azure Functions:");
            Path parent = path.getParent();
            if (parent != null) {
                OUT.println(
                        "\taz functionapp deployment source config-zip -g <resource_group> -n <function_app_name> " +
                                "--src " + parent.toString() + File.separator +
                                Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME + "\n\n");
            }
        });
    }
    private void generateFunctionsArtifact(Map<String, FunctionDeploymentContext> functions, Path binaryPath)
            throws IOException {
        new FunctionsArtifact(functions, binaryPath).generate(Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME);
    }
}
