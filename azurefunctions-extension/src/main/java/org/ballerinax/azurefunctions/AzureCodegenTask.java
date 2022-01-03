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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.GeneratorTask;
import io.ballerina.projects.plugins.SourceGeneratorContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.ballerina.tools.text.TextRange;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@code AnalysisTask} that is triggered for Cloud.toml validation.
 *
 * @since 1.0.0
 */
public class AzureCodegenTask implements GeneratorTask<SourceGeneratorContext> {

    @Override
    public void generate(SourceGeneratorContext sourceGeneratorContext) {
        Package currentPackage = sourceGeneratorContext.currentPackage();
        AzureFunctionExtractor azureFunctionExtractor = new AzureFunctionExtractor(currentPackage);
        List<FunctionDefinitionNode> extractedFunctions = azureFunctionExtractor.extractFunctions();
        Module module = currentPackage.getDefaultModule();
        SemanticModel semanticModel = sourceGeneratorContext.compilation().getSemanticModel(module.moduleId());
        Map<String, TypeDefinitionNode> typeDefinitions = new HashMap<>();
        FunctionHandlerGenerator functionGenerator = new FunctionHandlerGenerator(semanticModel, typeDefinitions);
        Map<String, JsonObject> generatedFunctions = new LinkedHashMap<>();
        List<FunctionDeploymentContext> contextList = new ArrayList<>();
        for (FunctionDefinitionNode function : extractedFunctions) {
            try {
                FunctionDeploymentContext context = functionGenerator.generateHandlerFunction(function);
                contextList.add(context);
                generatedFunctions.put(function.functionName().text(), context.getFunctionDefinition());
            } catch (AzureFunctionsException e) {
                sourceGeneratorContext.reportDiagnostic(e.getDiagnostic());
            }
        }
        try {
            writeObjectToJson(sourceGeneratorContext.currentPackage().project().targetDir(), generatedFunctions);
        } catch (IOException e) {
            DiagnosticInfo
                    diagnosticInfo = new DiagnosticInfo("azf-001", e.getMessage(), DiagnosticSeverity.ERROR);
            sourceGeneratorContext.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    new NullLocation()));
        }
        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        TextDocument textDocument = generateHandlerDocument(typeDefinitions, contextList);
        sourceGeneratorContext.addSourceFile(textDocument, Constants.AZ_FUNCTION_PREFIX, module.moduleId());
    }

    private TextDocument generateHandlerDocument(Map<String, TypeDefinitionNode> typeDefinitions,
                                                 List<FunctionDeploymentContext> ctx) {
        ModulePartNode modulePartNode =
                STUtil.createModulePartNode(ctx, typeDefinitions);
        return TextDocuments.from(modulePartNode.toSourceCode());
    }
    private void writeObjectToJson(Path targetPath, Map<String, JsonObject> ctxMap)
            throws IOException {
        Gson gson = new Gson();
        Path jsonPath = targetPath.resolve("azf.json");
        Files.deleteIfExists(jsonPath);
        Files.createFile(jsonPath);
        try (FileWriter r = new FileWriter(jsonPath.toAbsolutePath().toString(), StandardCharsets.UTF_8)) {
            gson.toJson(ctxMap, r);
        }
    }
}

/**
 * Represents Null Location in a ballerina document.
 *
 * @since 2.0.0
 */
class NullLocation implements Location {

    @Override
    public LineRange lineRange() {
        LinePosition from = LinePosition.from(0, 0);
        return LineRange.from("", from, from);
    }

    @Override
    public TextRange textRange() {
        return TextRange.from(0, 0);
    }
}
