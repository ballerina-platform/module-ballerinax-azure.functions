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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.FunctionContext;
import org.ballerinax.azurefunctions.Util;

import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.EXPLICIT_NEW_EXPRESSION;
/**
 * Represents an Trigger Binding in Azure Functions.
 *
 * @since 2.0.0
 */
public abstract class TriggerBinding extends Binding {
    protected ServiceDeclarationNode serviceDeclarationNode;
    protected SemanticModel semanticModel;
    
    public TriggerBinding(String triggerType) {
        super(triggerType, Constants.DIRECTION_IN);
    }
    
    public abstract List<FunctionContext> getBindings();

    public Optional<AnnotationNode> getListenerAnnotation(ServiceDeclarationNode svcDeclNode, String annotationName) {
        for (ExpressionNode expression : svcDeclNode.expressions()) {
            Optional<MetadataNode> metadata;
            if (EXPLICIT_NEW_EXPRESSION == expression.kind()) {
                metadata = svcDeclNode.metadata();
            } else {
                Optional<Symbol> symbol = this.semanticModel.symbol(expression);
                if (symbol.isEmpty()) {
                    continue;
                }
                Symbol listenerSymbol = symbol.get();
                if (SymbolKind.VARIABLE != listenerSymbol.kind()) {
                    continue;
                }
                VariableSymbol variableSymbol = (VariableSymbol) listenerSymbol;
                ListenerDeclarationNode listenerDeclarationNode =
                        (ListenerDeclarationNode) Util.findNode(svcDeclNode, variableSymbol);
                metadata = listenerDeclarationNode != null ? listenerDeclarationNode.metadata() : Optional.empty();
            }
            if (metadata.isEmpty()) {
                continue;
            }
            NodeList<AnnotationNode> annotations = metadata.get().annotations();
            for (AnnotationNode annotationNode : annotations) {
                Optional<Symbol> typeSymbol = this.semanticModel.symbol(annotationNode);
                if (typeSymbol.isEmpty()) {
                    continue;
                }
                Symbol annotationType = typeSymbol.get();
                Optional<String> name = annotationType.getName();
                if (name.isEmpty()) {
                    continue;
                }
                if (name.get().equals(annotationName)) {
                    return Optional.of(annotationNode);
                }
            }
        }
        return Optional.empty();
    }

    protected boolean isAzureFunctionsAnnotationExist(NodeList<AnnotationNode> nodes) {
        for (AnnotationNode annotation : nodes) {
            Node annotRef = annotation.annotReference();
            Optional<Symbol> annotationSymbol = semanticModel.symbol(annotRef);
            if (annotationSymbol.isEmpty()) {
                continue;
            }
            Optional<ModuleSymbol> module = annotationSymbol.get().getModule();
            if (module.isEmpty()) {
                continue;
            }
            Optional<String> name = module.get().getName();
            if (name.isEmpty()) {
                continue;
            }

            if (Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(name.get())) {
                return true;
            }
        }
        return false;
    }
}
