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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinax.azurefunctions.validators.MainFunctionValidator;
import org.ballerinax.azurefunctions.validators.SubmoduleValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for Extracting azure functions from a ballerina project.
 *
 * @since 2.0.0
 */
public class AzureFunctionExtractor {

    private final Project project;

    public AzureFunctionExtractor(Project project) {
        this.project = project;
    }

    public List<FunctionDefinitionNode> extractFunctions() {
        Module module = project.currentPackage().getDefaultModule();
        List<FunctionDefinitionNode> moduleFunctions = new ArrayList<>();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            Node node = document.syntaxTree().rootNode();
            AzureFunctionVisitor azureFunctionVisitor = new AzureFunctionVisitor();
            node.accept(azureFunctionVisitor);
            moduleFunctions.addAll(azureFunctionVisitor.getFunctions());
        }
        return moduleFunctions;
    }

    public List<Diagnostic> validateModules() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ModuleId moduleId : project.currentPackage().moduleIds()) {
            Module module = project.currentPackage().module(moduleId);
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                Node rootNode = document.syntaxTree().rootNode();
                if (document.name().endsWith(Constants.GENERATED_FILE_NAME)) {
                    continue;
                }
                diagnostics.addAll(validateMainFunction(rootNode));
                if (module.isDefaultModule()) {
                    continue;
                }
                diagnostics.addAll(validateSubmoduleDocument(rootNode));
            }
        }
        return diagnostics;
    }

    private List<Diagnostic> validateMainFunction(Node node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        node.accept(new MainFunctionValidator(diagnostics));
        return diagnostics;
    }

    private List<Diagnostic> validateSubmoduleDocument(Node node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        node.accept(new SubmoduleValidator(diagnostics));
        return diagnostics;
    }
}
