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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.GeneratorTask;
import io.ballerina.projects.plugins.SourceGeneratorContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@code AnalysisTask} that is triggered for Cloud.toml validation.
 *
 * @since 1.0.0
 */
public class AzureCodegenTask1 implements GeneratorTask<SourceGeneratorContext> {

    @Override
    public void generate(SourceGeneratorContext sourceGeneratorContext) {
        Package currentPackage = sourceGeneratorContext.currentPackage();
        AzureFunctionExtractor azureFunctionExtractor = new AzureFunctionExtractor(currentPackage);
        List<Diagnostic> diagnostics = new ArrayList<>(azureFunctionExtractor.validateModules());
        List<FunctionDefinitionNode> extractedFunctions = azureFunctionExtractor.extractFunctions();
        Module module = currentPackage.getDefaultModule();
        SemanticModel semanticModel = sourceGeneratorContext.compilation().getSemanticModel(module.moduleId());
        Map<String, TypeDefinitionNode> typeDefinitions = new HashMap<>();
        FunctionHandlerGenerator functionGenerator = new FunctionHandlerGenerator(semanticModel, typeDefinitions);
        AzureFunctionHolder functionHolder = AzureFunctionHolder.getInstance();
        Map<String, FunctionDeploymentContext> generatedFunctions = functionHolder.getGeneratedFunctions();
        for (FunctionDefinitionNode function : extractedFunctions) {
            try {
                FunctionDeploymentContext context = functionGenerator.generateHandlerFunction(function);
                generatedFunctions.put(function.functionName().text(), context);
            } catch (AzureFunctionsException e) {
                sourceGeneratorContext.reportDiagnostic(e.getDiagnostic());
            }
        }

        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        TextDocument textDocument =
                generateHandlerDocument(typeDefinitions, functionHolder);
        sourceGeneratorContext.addSourceFile(textDocument, Constants.AZ_FUNCTION_PREFIX, module.moduleId());
        for (Diagnostic diagnostic : diagnostics) {
            sourceGeneratorContext.reportDiagnostic(diagnostic);
        }
    }

    private TextDocument generateHandlerDocument(Map<String, TypeDefinitionNode> typeDefinitions,
                                                 AzureFunctionHolder holder) {
        ModulePartNode modulePartNode =
                STUtil.createModulePartNode(holder.getGeneratedFunctions().values(), typeDefinitions);
        return TextDocuments.from(modulePartNode.toSourceCode());
    }
}
