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
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@code AnalysisTask} that is triggered for Cloud.toml validation.
 *
 * @since 1.0.0
 */
public class AzureCodegenTask implements AnalysisTask<CompilationAnalysisContext> {

    @Override
    public void perform(CompilationAnalysisContext compilationAnalysisContext) {
        Package currentPackage = compilationAnalysisContext.currentPackage();
        AzureFunctionExtractor azureFunctionExtractor = new AzureFunctionExtractor(currentPackage);
        List<Diagnostic> diagnostics = new ArrayList<>(azureFunctionExtractor.validateModules());
        List<FunctionDefinitionNode> extractedFunctions = azureFunctionExtractor.extractFunctions();
        Module module = currentPackage.getDefaultModule();
        SemanticModel semanticModel = compilationAnalysisContext.compilation().getSemanticModel(module.moduleId());
        Map<String, TypeDefinitionNode> typeDefinitions = new HashMap<>();
        FunctionHandlerGenerator functionGenerator = new FunctionHandlerGenerator(semanticModel, typeDefinitions);
        AzureFunctionHolder functionHolder = AzureFunctionHolder.getInstance();
        Map<String, FunctionDeploymentContext> generatedFunctions = functionHolder.getGeneratedFunctions();
        for (FunctionDefinitionNode function : extractedFunctions) {
            try {
                FunctionDeploymentContext context = functionGenerator.generateHandlerFunction(function);
                generatedFunctions.put(function.functionName().text(), context);
            } catch (AzureFunctionsException e) {
                compilationAnalysisContext.reportDiagnostic(e.getDiagnostic());
            }
        }

        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        DocumentConfig documentConfig =
                generateHandlerDocument(currentPackage.project(), typeDefinitions, functionHolder);
        //Used to avoid duplicate documents as codeAnalyze is getting called multiple times
        if (!STUtil.isDocumentExistInModule(module, documentConfig)) {
            module.modify().addDocument(documentConfig).apply();
            currentPackage.getCompilation();
        }
        for (Diagnostic diagnostic : diagnostics) {
            compilationAnalysisContext.reportDiagnostic(diagnostic);
        }
    }

    private DocumentConfig generateHandlerDocument(Project project, Map<String, TypeDefinitionNode> typeDefinitions,
                                                   AzureFunctionHolder holder) {
        Module module = project.currentPackage().getDefaultModule();
        ModulePartNode modulePartNode =
                STUtil.createModulePartNode(holder.getGeneratedFunctions().values(), typeDefinitions);
        String newFileContent = modulePartNode.toSourceCode();
        String fileName = module.moduleName().toString() + "-" + Constants.GENERATED_FILE_NAME;
        Path filePath = project.sourceRoot().resolve(fileName);
        DocumentId newDocumentId = DocumentId.create(filePath.toString(), module.moduleId());
        return DocumentConfig.from(newDocumentId, newFileContent, fileName);
    }
}
