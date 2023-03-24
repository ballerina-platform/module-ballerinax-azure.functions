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
package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.plugins.ModifierTask;
import io.ballerina.projects.plugins.SourceModifierContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.context.DocumentContext;

import java.util.Map;

/**
 * {@code FunctionUpdaterTask} modifies the source by adding required meta-info for the azure function service
 * declarations.
 */
public class FunctionUpdaterTask implements ModifierTask<SourceModifierContext> {

    private final Map<DocumentId, DocumentContext> documentContextMap;

    public FunctionUpdaterTask(Map<DocumentId, DocumentContext> documentContextMap) {
        this.documentContextMap = documentContextMap;
    }

    @Override
    public void modify(SourceModifierContext context) {

        boolean erroneousCompilation = context.compilation().diagnosticResult()
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        Module module = context.currentPackage().getDefaultModule();
        SemanticModel semanticModel = module.getCompilation().getSemanticModel();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            ModulePartNode rootNode = document.syntaxTree().rootNode();
            DocumentContext documentContext = documentContextMap.get(documentId);
            AzureFunctionModifier azureFunctionVisitor = new AzureFunctionModifier(semanticModel, documentContext);
            rootNode = (ModulePartNode) rootNode.apply(azureFunctionVisitor);
            NodeList<ImportDeclarationNode> updatedImports = addHttpImport(rootNode.imports(), documentContext);
            ModulePartNode newModulePart = rootNode.modify(updatedImports, rootNode.members(), rootNode.eofToken());
            SyntaxTree updatedSyntaxTree = document.syntaxTree().modifyWith(newModulePart);
            context.modifySourceFile(updatedSyntaxTree.textDocument(), documentId);
        }

        // for test files
        for (ModuleId modId : context.currentPackage().moduleIds()) {
            Module currentModule = context.currentPackage().module(modId);
            for (DocumentId docId : currentModule.testDocumentIds()) {
                Document document = module.document(docId);
                ModulePartNode rootNode = document.syntaxTree().rootNode();
                DocumentContext documentContext = documentContextMap.get(docId);
                AzureFunctionModifier azureFunctionVisitor = new AzureFunctionModifier(semanticModel, documentContext);
                rootNode = (ModulePartNode) rootNode.apply(azureFunctionVisitor);
                NodeList<ImportDeclarationNode> updatedImports = addHttpImport(rootNode.imports(), documentContext);
                ModulePartNode newModulePart = rootNode.modify(updatedImports, rootNode.members(), rootNode.eofToken());
                SyntaxTree updatedSyntaxTree = document.syntaxTree().modifyWith(newModulePart);
                context.modifyTestSourceFile(updatedSyntaxTree.textDocument(), docId);
            }
        }
//
//        for (Map.Entry<DocumentId, DocumentContext> entry : documentContextMap.entrySet()) {
//            DocumentId documentId = entry.getKey();
//            DocumentContext documentContext = entry.getValue();
//            modifyPayloadParam(context, documentId, documentContext);
//        }
    }

//    private void modifyPayloadParam(SourceModifierContext modifierContext, DocumentId documentId,
//                                    DocumentContext documentContext) {
//
//        ModuleId moduleId = documentId.moduleId();
//        Module currentModule = modifierContext.currentPackage().module(moduleId);
//        Document currentDoc = currentModule.document(documentId);
//        ModulePartNode rootNode = currentDoc.syntaxTree().rootNode();
//        NodeList<ModuleMemberDeclarationNode> newMembers = updateMemberNodes(rootNode.members(), documentContext);
//        NodeList<ImportDeclarationNode> updatedImports = addHttpImport(rootNode.imports());
//        ModulePartNode newModulePart = rootNode.modify(updatedImports, newMembers, rootNode.eofToken());
//        SyntaxTree updatedSyntaxTree = currentDoc.syntaxTree().modifyWith(newModulePart);
//        TextDocument textDocument = updatedSyntaxTree.textDocument();
//        if (currentModule.documentIds().contains(documentId)) {
//            modifierContext.modifySourceFile(textDocument, documentId);
//        } else {
//            modifierContext.modifyTestSourceFile(textDocument, documentId);
//        }
//    }

    private NodeList<ImportDeclarationNode> addHttpImport(NodeList<ImportDeclarationNode> oldImports,
                                                          DocumentContext documentContext) {

        if (documentContext == null) {
            return oldImports;
        }
        boolean isHttpImportExists = false;
        for (ImportDeclarationNode importNode : oldImports) {
            if (importNode.orgName().isPresent()) {
                if (importNode.orgName().get().orgName().text().equals(Constants.BALLERINA_ORG)) {
                    if (importNode.moduleName().size() != 1) {
                        continue;
                    }
                    if (importNode.moduleName().get(0).text().equals(Constants.HTTP)) {
                        isHttpImportExists = true;
                        break;
                    }
                }
            }
        }
        if (isHttpImportExists) {
            return oldImports;
        }
        ImportDeclarationNode importNode = NodeFactory.createImportDeclarationNode(
                NodeFactory.createToken(SyntaxKind.IMPORT_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae(Constants.SPACE))),
                NodeFactory.createImportOrgNameNode(NodeFactory.createIdentifierToken(Constants.BALLERINA_ORG),
                        NodeFactory.createToken(SyntaxKind.SLASH_TOKEN)),
                NodeFactory.createSeparatedNodeList(NodeFactory.createIdentifierToken(Constants.HTTP)), null,
                NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
        return oldImports.add(importNode);
    }
}
