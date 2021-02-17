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
 * Implementation for the input parameter handler annotation "@CosmosDBTrigger".
 */
public class CosmosDBTriggerHandler extends AbstractParameterHandler {

    private Map<String, TypeDefinitionNode> generatedTypeDefinitions;

    public CosmosDBTriggerHandler(ParameterSymbol variableSymbol, RequiredParameterNode param,
                                  Map<String, TypeDefinitionNode> generatedTypeDefinitions) {
        super(variableSymbol, param, BindingType.TRIGGER);
        this.generatedTypeDefinitions = generatedTypeDefinitions;
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        PositionalArgumentNode params = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.PARAMS)));
        if (STUtil.isJsonType(this.variableSymbol)) {
            PositionalArgumentNode stringArg =
                    NodeFactory.createPositionalArgumentNode(NodeFactory
                            .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                                    .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + this.name + "\"",
                                            NodeFactory.createEmptyMinutiaeList(),
                                            NodeFactory.createEmptyMinutiaeList())));
            return STUtil.createAfFunctionInvocationNode("getJsonFromInputData", true, params, stringArg);
        } else if (STUtil.isRecordArrayType(this.variableSymbol)) {
            PositionalArgumentNode stringArg = NodeFactory.createPositionalArgumentNode(
                    NodeFactory.createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                            .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + this.name + "\"",
                                    NodeFactory.createEmptyMinutiaeList(),
                                    NodeFactory.createEmptyMinutiaeList())));
            ArrayTypeDescriptorNode arrayTypeDescriptor = (ArrayTypeDescriptorNode) param.typeName();
            TypeDefinitionNode arrayTypeDefinitionNode = STUtil.createArrayTypeDefinitionNode(arrayTypeDescriptor);
            generatedTypeDefinitions.put(arrayTypeDefinitionNode.typeName().text(), arrayTypeDefinitionNode);
            PositionalArgumentNode typeDesc =
                    NodeFactory.createPositionalArgumentNode(NodeFactory.createSimpleNameReferenceNode(
                            NodeFactory.createIdentifierToken(arrayTypeDefinitionNode.typeName().text())));
            ExpressionNode checkedExpr =
                    STUtil.createAfFunctionInvocationNode("getBallerinaValueFromInputData", true, params, stringArg,
                            typeDesc);
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

    @Override
    public void postInvocationProcess() throws AzureFunctionsException {
    }

    @Override
    public Map<String, Object> generateBinding() throws AzureFunctionsException {
        Map<String, Object> binding = new LinkedHashMap<>();
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        binding.put("type", "cosmosDBTrigger");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        binding.put("leaseConnectionStringSetting", annonMap.get("leaseConnectionStringSetting"));
        binding.put("leaseDatabaseName", annonMap.get("leaseDatabaseName"));
        binding.put("leaseCollectionName", annonMap.get("leaseCollectionName"));
        Boolean createLeaseCollectionIfNotExists = (Boolean) annonMap.get("createLeaseCollectionIfNotExists");
        if (createLeaseCollectionIfNotExists == null) {
            createLeaseCollectionIfNotExists = Constants.DEFAULT_COSMOS_DB_CREATELEASECOLLECTIONIFNOTEXISTS;
        }
        binding.put("createLeaseCollectionIfNotExists", createLeaseCollectionIfNotExists);
        binding.put("leasesCollectionThroughput", annonMap.get("leasesCollectionThroughput"));
        binding.put("leaseCollectionPrefix", annonMap.get("leaseCollectionPrefix"));
        binding.put("feedPollDelay", annonMap.get("feedPollDelay"));
        binding.put("leaseAcquireInterval", annonMap.get("leaseAcquireInterval"));
        binding.put("leaseExpirationInterval", annonMap.get("leaseExpirationInterval"));
        binding.put("leaseRenewInterval", annonMap.get("leaseRenewInterval"));
        binding.put("checkpointFrequency", annonMap.get("checkpointFrequency"));
        binding.put("maxItemsPerInvocation", annonMap.get("maxItemsPerInvocation"));
        binding.put("startFromBeginning", annonMap.get("startFromBeginning"));
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        return binding;
    }

}
