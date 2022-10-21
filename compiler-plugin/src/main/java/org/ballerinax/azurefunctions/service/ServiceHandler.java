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
package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.service.blob.BlobTriggerBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBTriggerBinding;
import org.ballerinax.azurefunctions.service.http.HTTPTriggerBinding;
import org.ballerinax.azurefunctions.service.queue.QueueTriggerBinding;
import org.ballerinax.azurefunctions.service.timer.TimerTriggerBinding;

import java.util.Optional;

/**
 * Represents the base handler for each azure service.
 *
 * @since 2.0.0
 */
public abstract class ServiceHandler {

    public static Optional<TriggerBinding> getBuilder(ServiceDeclarationNode svcDeclarationNode,
                                                SemanticModel semanticModel) {
        SeparatedNodeList<ExpressionNode> expressions = svcDeclarationNode.expressions();
        for (ExpressionNode expressionNode : expressions) {
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
            if (typeSymbol.isEmpty()) {
                continue;
            }
            TypeReferenceTypeSymbol typeSymbol1;
            if (TypeDescKind.UNION == typeSymbol.get().typeKind()) {
                UnionTypeSymbol union = (UnionTypeSymbol) typeSymbol.get();
                typeSymbol1 = (TypeReferenceTypeSymbol) union.memberTypeDescriptors().get(0);

            } else {
                typeSymbol1 = (TypeReferenceTypeSymbol) typeSymbol.get();
            }
            Symbol definition = typeSymbol1.definition();
            if (!Util.isSymbolAzureFunctions(definition)) {
                continue;
            }
            Optional<String> name = definition.getName();
            if (name.isEmpty()) {
                continue;
            }

            String serviceTypeName = name.get();
            switch (serviceTypeName) {
                case Constants.AZURE_HTTP_LISTENER:
                    return Optional.of(new HTTPTriggerBinding(svcDeclarationNode, semanticModel));
                case Constants.AZURE_QUEUE_LISTENER:
                    return Optional.of(new QueueTriggerBinding(svcDeclarationNode, semanticModel));
                case Constants.AZURE_COSMOS_LISTENER:
                    return Optional.of(new CosmosDBTriggerBinding(svcDeclarationNode, semanticModel));
                case Constants.AZURE_TIMER_LISTENER:
                    return Optional.of(new TimerTriggerBinding(svcDeclarationNode, semanticModel));
                case Constants.AZURE_BLOB_LISTENER: 
                    return Optional.of(new BlobTriggerBinding(svcDeclarationNode, semanticModel));
                default:
                    throw new RuntimeException("Unsupported Listener type");
            }
        }
        return Optional.empty();
    }
}
