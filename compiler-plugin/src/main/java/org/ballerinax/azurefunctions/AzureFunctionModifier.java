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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.LiteralValueToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SpreadMemberNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TreeModifier;
import org.ballerinax.azurefunctions.context.DocumentContext;
import org.ballerinax.azurefunctions.context.ResourceContext;
import org.ballerinax.azurefunctions.context.ServiceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsible for generating annotations for each function.
 *
 * @since 2.0.0
 */
public class AzureFunctionModifier extends TreeModifier {

    private SemanticModel semanticModel;
    private String modulePrefix;
    private DocumentContext documentContext;

    public AzureFunctionModifier(SemanticModel semanticModel, DocumentContext documentContext) {

        super();
        this.semanticModel = semanticModel;
        this.modulePrefix = Constants.AZURE_FUNCTIONS_MODULE_NAME;
        this.documentContext = documentContext;
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
        Optional<ModuleSymbol> module = typeRefSymbol.definition().getModule();
        if (module.isEmpty()) {
            return super.transform(serviceDeclarationNode);
        }
        Optional<String> name = module.get().getName();
        if (name.isEmpty()) {
            return super.transform(serviceDeclarationNode);
        }
        if (!module.get().id().orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG)) {
            return super.transform(serviceDeclarationNode);
        }
        NodeList<Node> members = serviceDeclarationNode.members();
        AzureFunctionNameGenerator nameGen = new AzureFunctionNameGenerator(serviceDeclarationNode);
        NodeList<Node> newMembersList = NodeFactory.createNodeList();
        for (Node node : members) {
            Node modifiedMember;
            boolean isNetworkFunction = SyntaxKind.RESOURCE_ACCESSOR_DEFINITION == node.kind() ||
                    SyntaxKind.OBJECT_METHOD_DEFINITION == node.kind();
            if (isNetworkFunction) {
                modifiedMember = getModifiedFunction((FunctionDefinitionNode) node, servicePath,
                        nameGen, getServiceContext(serviceDeclarationNode));
            } else {
                modifiedMember = node;
            }
            newMembersList = newMembersList.add(modifiedMember);
        }
        return new ServiceDeclarationNode.ServiceDeclarationNodeModifier(serviceDeclarationNode)
                .withMembers(newMembersList).apply();
    }

    private ServiceContext getServiceContext(ServiceDeclarationNode serviceDeclarationNode) {
        if (documentContext == null) {
            return null;
        }
        return documentContext.getServiceContext(serviceDeclarationNode.hashCode());
    }

    public FunctionDefinitionNode getModifiedFunction(FunctionDefinitionNode functionDefinitionNode, String servicePath,
                                    AzureFunctionNameGenerator nameGen, ServiceContext serviceContext) {
        //TODO refactor modifiers to multiple methods
        FunctionBodyNode functionBodyNode = functionDefinitionNode.functionBody();
        if (functionBodyNode.kind() != SyntaxKind.FUNCTION_BODY_BLOCK) {
            return functionDefinitionNode;
        }
        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) functionBodyNode;
        NodeList<StatementNode> statements = functionBodyBlockNode.statements();
        List<StatementNode> newStatements = new ArrayList<>();
        for (StatementNode statementNode : statements) {
            if (statementNode.kind() == SyntaxKind.RETURN_STATEMENT) {
                ReturnStatementNode returnStatementNode = (ReturnStatementNode) statementNode;
                Optional<ExpressionNode> expression = returnStatementNode.expression();
                if (expression.isEmpty()) {
                    newStatements.add(statementNode);
                    continue;
                }
                ExpressionNode expressionNode = expression.get();
                Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
                if (typeSymbol.isPresent()) {
                    if (typeSymbol.get().typeKind() == TypeDescKind.TUPLE) {
                        expressionNode = createSpreadOperator(expressionNode);
                        returnStatementNode = returnStatementNode.modify().withExpression(expressionNode).apply();
                        newStatements.add(returnStatementNode);
                        continue;
                    }
                }
            }
            newStatements.add(statementNode);
        }
        functionBodyBlockNode = functionBodyBlockNode.modify().withStatements(NodeFactory.createNodeList(newStatements))
                .apply();

        FunctionDefinitionNode.FunctionDefinitionNodeModifier functionDefModifier = functionDefinitionNode.modify();
        if (SyntaxKind.RESOURCE_ACCESSOR_DEFINITION == functionDefinitionNode.kind()) {
            Optional<FunctionSignatureNode> modifiedFunctionSignature = getModifiedFunctionSignature(serviceContext,
                    functionDefinitionNode);
            if (modifiedFunctionSignature.isPresent()) {
                functionDefModifier = functionDefModifier.withFunctionSignature(modifiedFunctionSignature.get());
            }
            String uniqueFunctionName = nameGen.getUniqueFunctionName(servicePath, functionDefinitionNode);
            Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
            NodeList<AnnotationNode> existingAnnotations = NodeFactory.createNodeList();
            MetadataNode metadataNode;
            if (metadata.isPresent()) {
                metadataNode = metadata.get();
                if (isFunctionAnnotationExist(functionDefinitionNode)) {
                    return functionDefModifier.withFunctionBody(functionBodyBlockNode).apply();
                }
                existingAnnotations = metadataNode.annotations();
            } else {
                metadataNode = NodeFactory.createMetadataNode(null, existingAnnotations);
            }

            //Create and add annotation
            NodeList<AnnotationNode> modifiedAnnotations =
                    existingAnnotations.add(createFunctionAnnotation(uniqueFunctionName));
            MetadataNode modifiedMetadata =
                    new MetadataNode.MetadataNodeModifier(metadataNode).withAnnotations(modifiedAnnotations).apply();
            return functionDefModifier.withMetadata(modifiedMetadata).withFunctionBody(functionBodyBlockNode).apply();
        } else {
            return functionDefModifier.withFunctionBody(functionBodyBlockNode).apply();
        }
    }

    private Optional<FunctionSignatureNode> getModifiedFunctionSignature(ServiceContext serviceContext,
                                                                         FunctionDefinitionNode functionDefNode) {
        if (serviceContext == null) {
            return Optional.empty();
        }
        int resourceId = functionDefNode.hashCode();
        if (!serviceContext.containsResource(resourceId)) {
            return Optional.empty();
        }
        ResourceContext resourceContext = serviceContext.getResourceContext(resourceId);
        FunctionSignatureNode functionSignatureNode = functionDefNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameterNodes = functionSignatureNode.parameters();
        List<Node> newParameterNodes = new ArrayList<>();
        int index = 0;
        for (ParameterNode parameterNode : parameterNodes) {
            if (index++ != resourceContext.getIndex()) {
                newParameterNodes.add(parameterNode);
                newParameterNodes.add(AbstractNodeFactory.createToken(SyntaxKind.COMMA_TOKEN));
                continue;
            }
            RequiredParameterNode param = (RequiredParameterNode) parameterNode;
            //add the annotation
            AnnotationNode payloadAnnotation = getHttpPayloadAnnotation();
            NodeList<AnnotationNode> annotations = NodeFactory.createNodeList();
            NodeList<AnnotationNode> updatedAnnotations = annotations.add(payloadAnnotation);
            RequiredParameterNode.RequiredParameterNodeModifier paramModifier = param.modify();
            paramModifier.withAnnotations(updatedAnnotations);
            RequiredParameterNode updatedParam = paramModifier.apply();
            newParameterNodes.add(updatedParam);
            newParameterNodes.add(AbstractNodeFactory.createToken(SyntaxKind.COMMA_TOKEN));
        }
        if (newParameterNodes.size() > 1) {
            newParameterNodes.remove(newParameterNodes.size() - 1);
        }

        FunctionSignatureNode.FunctionSignatureNodeModifier signatureModifier = functionSignatureNode.modify();
        SeparatedNodeList<ParameterNode> separatedNodeList = AbstractNodeFactory.createSeparatedNodeList(
                new ArrayList<>(newParameterNodes));
        signatureModifier.withParameters(separatedNodeList);
        return Optional.of(signatureModifier.apply());
    }

    private AnnotationNode getHttpPayloadAnnotation() {

        String payloadIdentifierString = Constants.HTTP + SyntaxKind.COLON_TOKEN.stringValue() +
                Constants.PAYLOAD_ANNOTATION + Constants.SPACE;
        IdentifierToken identifierToken = NodeFactory.createIdentifierToken(payloadIdentifierString);
        SimpleNameReferenceNode nameReferenceNode = NodeFactory.createSimpleNameReferenceNode(identifierToken);
        Token atToken = NodeFactory.createToken(SyntaxKind.AT_TOKEN);
        return NodeFactory.createAnnotationNode(atToken, nameReferenceNode, null);
    }

    public ListConstructorExpressionNode createSpreadOperator(ExpressionNode expressionNode) {

        Token openBracket = NodeFactory.createToken(SyntaxKind.OPEN_BRACKET_TOKEN);
        Token closeBracket = NodeFactory.createToken(SyntaxKind.CLOSE_BRACKET_TOKEN);
        Token ellipsis = NodeFactory.createToken(SyntaxKind.ELLIPSIS_TOKEN);
        SpreadMemberNode spreadMemberNode = NodeFactory.createSpreadMemberNode(ellipsis, expressionNode);
        SeparatedNodeList<Node> expressions = NodeFactory.createSeparatedNodeList(spreadMemberNode);
        return NodeFactory.createListConstructorExpressionNode(openBracket, expressions, closeBracket);
    }

    public boolean isFunctionAnnotationExist(FunctionDefinitionNode functionDefinitionNode) {
        FunctionSymbol functionSymbol = (FunctionSymbol) semanticModel.symbol(functionDefinitionNode).orElseThrow();
        List<AnnotationSymbol> annotations = functionSymbol.annotations();
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<String> name = annotationSymbol.getName();
            if (name.isEmpty()) {
                continue;
            }
            Optional<ModuleSymbol> module = annotationSymbol.getModule();
            if (module.isEmpty()) {
                continue;
            }
            ModuleID id = module.get().id();
            if (name.get().equals(Constants.FUNCTION_ANNOTATION) &&
                    id.orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG)) {
                return true;
            }
        }
        return false;
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
