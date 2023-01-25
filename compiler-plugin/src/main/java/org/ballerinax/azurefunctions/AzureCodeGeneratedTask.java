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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import org.ballerinax.azurefunctions.service.Binding;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contains the code generation part of the azure functions.
 *
 * @since 2.0.0
 */
public class AzureCodeGeneratedTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {

    private static final PrintStream OUT = System.out;

    @Override
    public void perform(CompilerLifecycleEventContext compilerLifecycleEventContext) {

        AzureFunctionServiceExtractor azureFunctionServiceExtractor =
                new AzureFunctionServiceExtractor(compilerLifecycleEventContext.currentPackage());
        List<FunctionContext> functionContexts = azureFunctionServiceExtractor.extractFunctions();
        Map<String, JsonObject> generatedFunctions = new HashMap<>();

        for (FunctionContext ctx : functionContexts) {
            JsonObject functions = new JsonObject();
            JsonArray bindings = new JsonArray();
            List<Binding> bindingList = ctx.getBindingList();
            for (Binding binding : bindingList) {
                bindings.add(binding.getJsonObject());
            }
            functions.add("bindings", bindings);
            generatedFunctions.put(ctx.getFunctionName(), functions);
        }

        Optional<Path> generatedArtifactPath = compilerLifecycleEventContext.getGeneratedArtifactPath();
        boolean isNative = compilerLifecycleEventContext.currentPackage().project().buildOptions().nativeImage();
        generatedArtifactPath.ifPresent(path -> {
            try {
                if (isNative) {
                    OUT.println("\n\t@azure_functions: Building native image compatible for the Cloud using Docker. " +
                            "This may take a while\n");
                }
                this.generateFunctionsArtifact(generatedFunctions, path, isNative);
            } catch (IOException | DockerBuildException e) {
                OUT.println("Error generating Azure Functions: " + e.getMessage());
                return;
            }
            OUT.println("\n\t@azure_functions:Function: " + String.join(", ", generatedFunctions.keySet()));
            OUT.println("\n\tExecute the command below to deploy the function locally:");
            OUT.println(
                    "\tfunc start --script-root " + getLocalArtifactPath(isNative) + getLocalRuntimeFlag(isNative));
            OUT.println("\n\tExecute the command below to deploy Ballerina Azure Functions:");
            Path parent = path.getParent();
            if (parent != null) {
                OUT.println(
                        "\tfunc azure functionapp publish <function_app_name> --script-root " +
                                Constants.ARTIFACT_PATH + " \n");
            }
        });
    }

    private void generateFunctionsArtifact(Map<String, JsonObject> functions, Path binaryPath, boolean isNative)
            throws IOException {
        if (isNative) {
            new NativeFunctionsArtifact(functions, binaryPath).generate();
        } else {
            new FunctionsArtifact(functions, binaryPath).generate();
        }
    }

    private String getLocalArtifactPath(boolean isNative) {
        if (isNative) {
            return Constants.LOCAL_ARTIFACT_PATH;
        } else {
            return Constants.ARTIFACT_PATH;
        }
    }

    private String getLocalRuntimeFlag(boolean isNative) {
        if (!isNative) {
            return " --java";
        }
        return "";
    }
}
