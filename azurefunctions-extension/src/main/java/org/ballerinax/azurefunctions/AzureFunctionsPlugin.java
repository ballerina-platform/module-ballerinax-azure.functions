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
package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportedAnnotationPackages;
import org.ballerinalang.core.util.exceptions.BallerinaException;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.util.diagnostic.DiagnosticLog;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiler plugin to process Azure Functions function annotations.
 */
@SupportedAnnotationPackages(value = "ballerinax/azure_functions:0.0.0")
public class AzureFunctionsPlugin extends AbstractCompilerPlugin {

    private static final PrintStream OUT = System.out;

    private static Map<String, FunctionDeploymentContext> generatedFunctions = new LinkedHashMap<>();

    private DiagnosticLog dlog;

    @Override
    public void init(DiagnosticLog dlog) {
        this.dlog = dlog;
    }

    @Override
    public void process(PackageNode packageNode) {
        super.process(packageNode);
    }

    @Override
    public List<Diagnostic> codeAnalyze(Project project) {
        AzureFunctionExtractor azureFunctionExtractor = new AzureFunctionExtractor(project);
        List<Diagnostic> diagnostics = new ArrayList<>(azureFunctionExtractor.validateModules());
        List<FunctionDefinitionNode> extractedFunctions = azureFunctionExtractor.extractFunctions();
        Module module = project.currentPackage().getDefaultModule();
        SemanticModel semanticModel = module.getCompilation().getSemanticModel();
        Map<String, TypeDefinitionNode> typeDefinitions = new HashMap<>();
        FunctionHandlerGenerator functionGenerator = new FunctionHandlerGenerator(semanticModel, typeDefinitions);
        for (FunctionDefinitionNode function : extractedFunctions) {
            try {
                FunctionDeploymentContext functionDeploymentContext =
                        functionGenerator.generateHandlerFunction(function);
                generatedFunctions.put(function.functionName().text(), functionDeploymentContext);
            } catch (AzureFunctionsException e) {
                return Collections.singletonList(e.getDiagnostic());
            }
        }

        DocumentConfig documentConfig = generateHandlerDocument(project, typeDefinitions, module);
        //Used to avoid duplicate documents as codeAnalyze is getting called multiple times
        if (!STUtil.isDocumentExistInModule(module, documentConfig)) {
            module.modify().addDocument(documentConfig).apply();
            project.currentPackage().getCompilation();
        }
        return diagnostics;
    }

    private DocumentConfig generateHandlerDocument(Project project, Map<String, TypeDefinitionNode> typeDefinitions,
                                                   Module module) {
        FunctionDefinitionNode mainFunction = STUtil.createMainFunction(generatedFunctions.values());
        ModulePartNode modulePartNode =
                STUtil.createModulePartNode(generatedFunctions.values(), mainFunction, typeDefinitions);

        String newFileContent = modulePartNode.toSourceCode();
        String fileName = module.moduleName().toString() + "-" + Constants.GENERATED_FILE_NAME;
        Path filePath = project.sourceRoot().resolve(fileName);
        DocumentId newDocumentId = DocumentId.create(filePath.toString(), module.moduleId());
        return DocumentConfig.from(newDocumentId, newFileContent, fileName);
    }

    @Override
    public void codeGenerated(Project project, Target target) {
        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        OUT.println("\t@azure_functions:Function: " + String.join(", ", generatedFunctions.keySet()));
        try {
            this.generateFunctionsArtifact(generatedFunctions, target.getExecutablePath(project.currentPackage()));
        } catch (IOException e) {
            String msg = "Error generating Azure Functions: " + e.getMessage();
            OUT.println(msg);
            throw new BallerinaException(msg, e);
        }
        OUT.println("\n\tExecute the below command to deploy Ballerina Azure Functions:");
        try {
            Path parent = target.getExecutablePath(project.currentPackage()).getParent();
            if (parent != null) {
                OUT.println(
                        "\taz functionapp deployment source config-zip -g <resource_group> -n <function_app_name> " +
                                "--src " + parent.toString() + File.separator +
                                Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME + "\n\n");
            }
        } catch (IOException e) {
            //ignored;
        }
    }

    private void generateFunctionsArtifact(Map<String, FunctionDeploymentContext> functions, Path binaryPath)
            throws IOException {
        new FunctionsArtifact(functions, binaryPath).generate(Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME);
    }

}
