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

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor for extracting Azure functions from a ballerina document.
 *
 * @since 2.0.0
 */
public class AzureFunctionVisitor extends NodeVisitor {

    private final List<FunctionDefinitionNode> functions;
    private String moduleName;

    public AzureFunctionVisitor() {
        this.functions = new ArrayList<>();
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        super.visit(modulePartNode);
    }

    @Override
    public void visit(ImportDeclarationNode importDeclarationNode) {
        if (importDeclarationNode.orgName().isEmpty()) {
            return;
        }
        String orgName = importDeclarationNode.orgName().get().orgName().text();
        if (!Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(orgName)) {
            return;
        }
        if (importDeclarationNode.moduleName().size() != 1) {
            return;
        }
        String moduleName = importDeclarationNode.moduleName().get(0).text();
        if (Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(moduleName)) {
            this.moduleName = moduleName;
        }
        if (importDeclarationNode.prefix().isEmpty()) {
            return;
        }
        this.moduleName = importDeclarationNode.prefix().get().prefix().text();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        if (this.moduleName == null) {
            return;
        }
        if (functionDefinitionNode.metadata().isEmpty()) {
            return;
        }

        MetadataNode metadataNode = functionDefinitionNode.metadata().get();
        for (AnnotationNode annotation : metadataNode.annotations()) {
            if (annotation.annotReference().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                continue;
            }
            QualifiedNameReferenceNode qualifiedNameReferenceNode =
                    (QualifiedNameReferenceNode) annotation.annotReference();
            String modulePrefix = qualifiedNameReferenceNode.modulePrefix().text();
            String identifier = qualifiedNameReferenceNode.identifier().text();
            if (modulePrefix.equals(this.moduleName) && Constants.AWS_FUNCTION_TYPE.equals(identifier)) {
                functions.add(functionDefinitionNode);
            }
        }
    }

    public List<FunctionDefinitionNode> getFunctions() {
        return functions;
    }
}
