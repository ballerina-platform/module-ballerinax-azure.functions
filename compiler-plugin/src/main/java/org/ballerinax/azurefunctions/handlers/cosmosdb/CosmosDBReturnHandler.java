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

import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.STUtil;
import org.ballerinax.azurefunctions.handlers.AbstractReturnHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the return handler annotation "@CosmosDBOutput".
 */
public class CosmosDBReturnHandler extends AbstractReturnHandler {

    public CosmosDBReturnHandler(TypeSymbol retType, AnnotationNode annotation) {
        super(retType, annotation);
    }

    @Override
    public void postInvocationProcess(ExpressionNode returnValueExpr) throws AzureFunctionsException {
        PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                STUtil.createVariableRef(Constants.PARAMS));
        if (retType.typeKind() == TypeDescKind.JSON) {
            PositionalArgumentNode returnExpr = NodeFactory.createPositionalArgumentNode(returnValueExpr);
            STUtil.addAzurePkgFunctionCallStatement(this.ctx, "setJsonReturn", true, paramsArg, returnExpr);
        } else if (retType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            PositionalArgumentNode returnExpr = NodeFactory.createPositionalArgumentNode(returnValueExpr);
            STUtil.addAzurePkgFunctionCallStatement(this.ctx, "setBallerinaValueAsJsonReturn", true, paramsArg,
                    returnExpr);
        } else if (STUtil.isRecordArrayType(retType)) {
            PositionalArgumentNode returnExpr = NodeFactory.createPositionalArgumentNode(returnValueExpr);
            STUtil.addAzurePkgFunctionCallStatement(this.ctx, "setBallerinaValueAsJsonReturn", true, paramsArg,
                    returnExpr);
        } else {
            IdentifierToken identifier = ((QualifiedNameReferenceNode) annotation.annotReference()).identifier();
            throw new AzureFunctionsException(STUtil.getAFDiagnostic(returnValueExpr.location(), "AZ0007",
                    "unsupported.return.annotation", DiagnosticSeverity.ERROR, "Type '" + identifier.text() + "' is " +
                            "not supported"));
        }
        //TODO handle record array
    }

    @Override
    public Map<String, Object> generateBinding() throws AzureFunctionsException {
        Map<String, Object> binding = new LinkedHashMap<>();
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotation);
        binding.put("type", "cosmosDB");
        binding.put("connectionStringSetting", annonMap.get("connectionStringSetting"));
        binding.put("databaseName", annonMap.get("databaseName"));
        binding.put("collectionName", annonMap.get("collectionName"));
        binding.put("createIfNotExists", annonMap.get("createIfNotExists"));
        binding.put("partitionKey", annonMap.get("partitionKey"));
        binding.put("collectionThroughput", annonMap.get("collectionThroughput"));
        binding.put("preferredLocations", annonMap.get("preferredLocations"));
        binding.put("useMultipleWriteLocations", annonMap.get("useMultipleWriteLocations"));
        return binding;
    }

}
