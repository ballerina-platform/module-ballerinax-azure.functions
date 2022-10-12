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
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.LiteralValueToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TreeModifier;

import java.util.Optional;

/**
 * Responsible for generating annotations for each function.
 *
 * @since 2.0.0
 */
public class AzureFunctionModifier extends TreeModifier {

    private SemanticModel semanticModel;
    private String modulePrefix;

    public AzureFunctionModifier(SemanticModel semanticModel) {
        super();
        this.semanticModel = semanticModel;
        this.modulePrefix = Constants.AZURE_FUNCTIONS_MODULE_NAME;
    }

    @Override
    public ImportDeclarationNode transform(ImportDeclarationNode importDeclarationNode) {
        Optional<ImportOrgNameNode> importOrgNameNode = importDeclarationNode.orgName();
        if (importOrgNameNode.isEmpty()) {
            return importDeclarationNode;
        }
        if (!Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(importOrgNameNode.get().orgName().text())) {
            return importDeclarationNode;
        }
        SeparatedNodeList<IdentifierToken> identifierTokens = importDeclarationNode.moduleName();
        if (identifierTokens.size() != 1) {
            return importDeclarationNode;
        }
        if (!Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(identifierTokens.get(0).text())) {
            return importDeclarationNode;
        }

        Optional<ImportPrefixNode> prefix = importDeclarationNode.prefix();
        if (prefix.isEmpty()) {
            this.modulePrefix = Constants.AZURE_FUNCTIONS_MODULE_NAME;
            return importDeclarationNode;
        }
        this.modulePrefix = prefix.get().prefix().text();
        return importDeclarationNode;
    }

    @Override
    public ServiceDeclarationNode transform(ServiceDeclarationNode serviceDeclarationNode) {
        String servicePath = Util.resourcePathToString(serviceDeclarationNode.absoluteResourcePath());
        ExpressionNode listenerExpressionNode = serviceDeclarationNode.expressions().get(0);
        Optional<TypeSymbol> listenerSymbol = semanticModel.typeOf(listenerExpressionNode);
        if (listenerSymbol.isEmpty()) {
            return super.transform(serviceDeclarationNode);
        }
        TypeReferenceTypeSymbol typeRefSymbol;
        if (TypeDescKind.UNION == listenerSymbol.get().typeKind()) {
            UnionTypeSymbol union = (UnionTypeSymbol) listenerSymbol.get();
            typeRefSymbol = (TypeReferenceTypeSymbol) union.memberTypeDescriptors().get(0);

        } else {
            typeRefSymbol = (TypeReferenceTypeSymbol) listenerSymbol.get();
        }
        Optional<String> name = typeRefSymbol.definition().getName();
        if (name.isEmpty()) {
            return super.transform(serviceDeclarationNode);
        }
        NodeList<Node> members = serviceDeclarationNode.members();
        if (!Constants.AZURE_HTTP_LISTENER.equals(name.get())) {
            return super.transform(serviceDeclarationNode);
        }
        AzureFunctionNameGenerator nameGen = new AzureFunctionNameGenerator(serviceDeclarationNode);
        NodeList<Node> newMembersList = NodeFactory.createNodeList();
        for (Node node : members) {
            Optional<Node> modifiedMember = getModifiedMember(node, servicePath, nameGen);
            if (modifiedMember.isEmpty()) {
                newMembersList = newMembersList.add(node);
            } else {
                newMembersList = newMembersList.add(modifiedMember.get());
            }
        }
        return new ServiceDeclarationNode.ServiceDeclarationNodeModifier(serviceDeclarationNode)
                .withMembers(newMembersList).apply();
    }

    public Optional<Node> getModifiedMember(Node node, String servicePath, AzureFunctionNameGenerator nameGen) {
        if (SyntaxKind.RESOURCE_ACCESSOR_DEFINITION != node.kind()) {
            return Optional.empty();
        }
        FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
        String uniqueFunctionName = nameGen.getUniqueFunctionName(servicePath, functionDefinitionNode);
        Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
        NodeList<AnnotationNode> existingAnnotations = NodeFactory.createNodeList();
        MetadataNode metadataNode;
        if (metadata.isPresent()) {
            metadataNode = metadata.get();
            existingAnnotations = metadataNode.annotations();
        } else {
            metadataNode = NodeFactory.createMetadataNode(null, existingAnnotations);
        }

        //Create and add annotation
        NodeList<AnnotationNode> modifiedAnnotations =
                existingAnnotations.add(createFunctionAnnotation(uniqueFunctionName));
        MetadataNode modifiedMetadata =
                new MetadataNode.MetadataNodeModifier(metadataNode).withAnnotations(modifiedAnnotations).apply();
        FunctionDefinitionNode updatedFunctionNode =
                new FunctionDefinitionNode.FunctionDefinitionNodeModifier(functionDefinitionNode)
                        .withMetadata(modifiedMetadata).apply();
        return Optional.of(updatedFunctionNode);
    }

    public AnnotationNode createFunctionAnnotation(String functionName) {
        QualifiedNameReferenceNode azureFunctionAnnotRef =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(modulePrefix),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken(Constants.FUNCTION_ANNOTATION));
        LiteralValueToken literalValueToken =
                NodeFactory.createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + functionName +
                        "\"", NodeFactory.createEmptyMinutiaeList(), AbstractNodeFactory
                        .createEmptyMinutiaeList());
        BasicLiteralNode basicLiteralNode =
                NodeFactory.createBasicLiteralNode(SyntaxKind.STRING_LITERAL, literalValueToken);
        SpecificFieldNode name = NodeFactory.createSpecificFieldNode(null, NodeFactory.createIdentifierToken("name"),
                NodeFactory.createToken(SyntaxKind.COLON_TOKEN), basicLiteralNode);
        SeparatedNodeList<MappingFieldNode> updatedFields = NodeFactory.createSeparatedNodeList(name);
        MappingConstructorExpressionNode annotationValue =
                NodeFactory.createMappingConstructorExpressionNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN), updatedFields,
                        NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        return NodeFactory.createAnnotationNode(NodeFactory.createToken(SyntaxKind.AT_TOKEN), azureFunctionAnnotRef,
                annotationValue);
    }
}
