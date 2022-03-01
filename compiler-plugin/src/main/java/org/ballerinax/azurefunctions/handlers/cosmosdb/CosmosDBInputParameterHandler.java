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
package org.ballerinax.azurefunctions.handlers.cosmosdb;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.STUtil;
import org.ballerinax.azurefunctions.handlers.AbstractParameterHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation for the input parameter handler annotation "@CosmosDBInput".
 */
public class CosmosDBInputParameterHandler extends AbstractParameterHandler {

    private Map<String, TypeDefinitionNode> generatedTypeDefinitions;

    public CosmosDBInputParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param,
                                         Map<String, TypeDefinitionNode> generatedTypeDefinitions) {
        super(variableSymbol, param, BindingType.INPUT);
        this.generatedTypeDefinitions = generatedTypeDefinitions;
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        boolean singleRecordQuery = this.isSingleRecordQuery();
        PositionalArgumentNode params = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.PARAMS)));
        if (singleRecordQuery) {
            if (STUtil.isJsonType(this.variableSymbol)) {
                PositionalArgumentNode stringArg =
                        NodeFactory.createPositionalArgumentNode(NodeFactory
                                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                                "\"" + this.name + "\"",
                                                NodeFactory.createEmptyMinutiaeList(),
                                                NodeFactory.createEmptyMinutiaeList())));
                return STUtil.createAfFunctionInvocationNode("getParsedJsonFromJsonStringFromInputData", true, params,
                        stringArg);
            } else if (STUtil.isOptionalRecordType(this.variableSymbol)) {
                PositionalArgumentNode stringArg =
                        NodeFactory.createPositionalArgumentNode(NodeFactory
                                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                                "\"" + this.name + "\"",
                                                NodeFactory.createEmptyMinutiaeList(),
                                                NodeFactory.createEmptyMinutiaeList())));
                OptionalTypeDescriptorNode optionalTypeDescriptor = (OptionalTypeDescriptorNode) param.typeName();
                TypeDefinitionNode optionalTypeDefinitionNode =
                        STUtil.createOptionalTypeDefinitionNode(optionalTypeDescriptor);
                generatedTypeDefinitions.put(optionalTypeDefinitionNode.typeName().text(), optionalTypeDefinitionNode);
                PositionalArgumentNode typeDesc =
                        NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                NodeFactory.createIdentifierToken(optionalTypeDefinitionNode.typeName().text())));
                ExpressionNode checkedExpr =
                        STUtil.createAfFunctionInvocationNode("getOptionalBallerinaValueFromInputData", true, params,
                                stringArg, typeDesc);
                return NodeFactory.createTypeCastExpressionNode(NodeFactory.createToken(SyntaxKind.LT_TOKEN),
                        NodeFactory.createTypeCastParamNode(NodeFactory.createEmptyNodeList(), optionalTypeDescriptor),
                        NodeFactory.createToken(SyntaxKind.GT_TOKEN), checkedExpr);
            } else {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(this.param.typeName().location(), "AZ0008",
                        "unsupported.param.type", DiagnosticSeverity.ERROR,
                        "type '" + this.param.typeName().toString() + "'" +
                                " is not supported"));
            }
        } else {
            if (STUtil.isJsonType(this.variableSymbol)) {
                PositionalArgumentNode stringArg =
                        NodeFactory.createPositionalArgumentNode(NodeFactory
                                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                                "\"" + this.name + "\"",
                                                NodeFactory.createEmptyMinutiaeList(),
                                                NodeFactory.createEmptyMinutiaeList())));
                return STUtil.createAfFunctionInvocationNode("getJsonFromInput", true, params, stringArg);
            } else if (STUtil.isRecordArrayType(this.variableSymbol)) {
                PositionalArgumentNode stringArg =
                        NodeFactory.createPositionalArgumentNode(NodeFactory
                                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                                "\"" + this.name + "\"",
                                                NodeFactory.createEmptyMinutiaeList(),
                                                NodeFactory.createEmptyMinutiaeList())));
                ArrayTypeDescriptorNode arrayTypeDescriptor = (ArrayTypeDescriptorNode) param.typeName();
                TypeDefinitionNode arrayTypeDefinitionNode = STUtil.createArrayTypeDefinitionNode(arrayTypeDescriptor);
                generatedTypeDefinitions.put(arrayTypeDefinitionNode.typeName().text(), arrayTypeDefinitionNode);
                PositionalArgumentNode typeDesc =
                        NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                                NodeFactory.createIdentifierToken(arrayTypeDefinitionNode.typeName().text())));
                ExpressionNode checkedExpr =
                        STUtil.createAfFunctionInvocationNode("getBallerinaValueFromInputData", true, params,
                                stringArg, typeDesc);

                return NodeFactory.createTypeCastExpressionNode(NodeFactory.createToken(SyntaxKind.LT_TOKEN),
                        NodeFactory.createTypeCastParamNode(NodeFactory.createEmptyNodeList(), arrayTypeDescriptor),
                        NodeFactory.createToken(SyntaxKind.GT_TOKEN), checkedExpr);
            } else {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(this.param.typeName().location(), "AZ0008",
                        "unsupported.param.type", DiagnosticSeverity.ERROR,
                        "type '" + this.param.typeName().toString() + "'" +
                                " is not supported"));
            }
        }
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException {
    }

    @Override
    public Map<String, Object> generateBinding() throws AzureFunctionsException {
        Map<String, Object> binding = new LinkedHashMap<>();
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        binding.put("type", "cosmosDB");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        binding.put("id", annonMap.get("id"));
        binding.put("sqlQuery", annonMap.get("sqlQuery"));
        binding.put("partitionKey", annonMap.get("partitionKey"));
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        return binding;
    }
    
    private boolean isSingleRecordQuery() throws AzureFunctionsException {
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        return annonMap.get("id") != null;
    }
}
