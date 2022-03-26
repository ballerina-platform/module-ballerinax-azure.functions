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
package org.ballerinax.azurefunctions.generator;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.generator.handlers.blob.BlobInputParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.blob.BlobOutputParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.blob.BlobTriggerParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.context.ContextParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.cosmosdb.CosmosDBInputParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.cosmosdb.CosmosDBReturnHandler;
import org.ballerinax.azurefunctions.generator.handlers.cosmosdb.CosmosDBTriggerHandler;
import org.ballerinax.azurefunctions.generator.handlers.http.HTTPOutputParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.http.HTTPReturnHandler;
import org.ballerinax.azurefunctions.generator.handlers.http.HTTPTriggerParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.metadata.MetadataBindingParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.queue.QueueOutputParameterHandler;
import org.ballerinax.azurefunctions.generator.handlers.queue.QueueTriggerHandler;
import org.ballerinax.azurefunctions.generator.handlers.timer.TimerTriggerHandler;
import org.ballerinax.azurefunctions.generator.handlers.twilio.TwilioSmsOutputParameterHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Factory class to create parameter and return handlers.
 */
public class HandlerFactory {

    public static ParameterHandler createParameterHandler(ParameterNode param, SemanticModel semanticModel,
                                                          Map<String, TypeDefinitionNode> generatedTypeDefinitions)
            throws AzureFunctionsException {
        if (param.kind() != SyntaxKind.REQUIRED_PARAM) {
            throw new AzureFunctionsException(GeneratorUtil.getAFDiagnostic(param.location(), "AZ0004",
                    "required.param.supported", DiagnosticSeverity.ERROR, "only required params are supported"));
        }
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) param;
        if (requiredParameterNode.paramName().isEmpty()) {
            throw new AzureFunctionsException(GeneratorUtil.getAFDiagnostic(param.location(), "AZ0005",
                    "required.param.name", DiagnosticSeverity.ERROR, "param name is required"));
        }
        Optional<Symbol> paramSymbol = semanticModel.symbol(requiredParameterNode.paramName().get());
        if (paramSymbol.isEmpty() || paramSymbol.get().kind() != SymbolKind.PARAMETER) {
            throw new AzureFunctionsException(GeneratorUtil.getAFDiagnostic(param.location(), "AZ0010",
                    "symbol.not.found", DiagnosticSeverity.ERROR, "parameter symbol not found"));
        }
        ParameterSymbol variableSymbol = (ParameterSymbol) paramSymbol.get();
        if (GeneratorUtil.isContextType(variableSymbol)) {
            return new ContextParameterHandler(variableSymbol, requiredParameterNode);
        }

        Optional<AnnotationSymbol> annotationNode =
                GeneratorUtil.extractAzureFunctionAnnotation(variableSymbol.annotations());
        if (annotationNode.isEmpty()) {
            throw new AzureFunctionsException(GeneratorUtil.getAFDiagnostic(requiredParameterNode.location(), "AZ0006",
                    "missing.required.annotation", DiagnosticSeverity.ERROR, "azure functions annotation is required"));
        }

        String annotationName = annotationNode.get().getName().orElseThrow();
        switch (annotationName) {
            case "HTTPOutput":
                return new HTTPOutputParameterHandler(variableSymbol, requiredParameterNode);
            case "HTTPTrigger":
                return new HTTPTriggerParameterHandler(variableSymbol, requiredParameterNode);
            case "QueueOutput":
                return new QueueOutputParameterHandler(variableSymbol, requiredParameterNode);
            case "QueueTrigger":
                return new QueueTriggerHandler(variableSymbol, requiredParameterNode);
            case "TimerTrigger":
                return new TimerTriggerHandler(variableSymbol, requiredParameterNode);
            case "BlobTrigger":
                return new BlobTriggerParameterHandler(variableSymbol, requiredParameterNode);
            case "BlobInput":
                return new BlobInputParameterHandler(variableSymbol, requiredParameterNode);
            case "BlobOutput":
                return new BlobOutputParameterHandler(variableSymbol, requiredParameterNode);
            case "TwilioSmsOutput":
                return new TwilioSmsOutputParameterHandler(variableSymbol, requiredParameterNode);
            case "BindingName":
                return new MetadataBindingParameterHandler(variableSymbol, requiredParameterNode);
            case "CosmosDBTrigger":
                return new CosmosDBTriggerHandler(variableSymbol, requiredParameterNode,
                        generatedTypeDefinitions);
            case "CosmosDBInput":
                return new CosmosDBInputParameterHandler(variableSymbol, requiredParameterNode,
                        generatedTypeDefinitions);
            default:
                throw new AzureFunctionsException(
                        GeneratorUtil.getAFDiagnostic(annotationNode.get().getLocation().orElseThrow(),
                                "AZ0006", "unsupported.param.handler", DiagnosticSeverity.ERROR,
                                "param handler not found for the type: " + annotationName));
        }
    }

    public static ReturnHandler createReturnHandler(TypeSymbol symbol,
                                                    ReturnTypeDescriptorNode returnTypeDescriptorNode)
            throws AzureFunctionsException {
        //TODO avoid using syntax tree and use semantic api instead when the its supported.
        //https://github.com/ballerina-platform/ballerina-lang/issues/27225
        TypeSymbol retType = GeneratorUtil.getMainParamType(symbol);
        NodeList<AnnotationNode> annotations = returnTypeDescriptorNode.annotations();
        Optional<AnnotationNode> azureAnnotations = GeneratorUtil.extractAzureFunctionAnnotation(annotations);
        if (azureAnnotations.isEmpty()) {
            return null;
        }
        Node annotReference = azureAnnotations.get().annotReference();
        if (annotReference.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            throw new AzureFunctionsException(
                    GeneratorUtil.getAFDiagnostic(annotReference.location(), "AZ0002", "unexpected.node.type",
                            DiagnosticSeverity.ERROR, "unexpected node type"));
        }

        String name = ((QualifiedNameReferenceNode) annotReference).identifier().text();
        if ("HTTPOutput".equals(name)) {
            return new HTTPReturnHandler(retType, azureAnnotations.get());
        } else if ("CosmosDBOutput".equals(name)) {
            return new CosmosDBReturnHandler(retType, azureAnnotations.get());
        } else {
            throw new AzureFunctionsException(
                    GeneratorUtil.getAFDiagnostic(annotReference.location(), "AZ0001", "unsupported.return.handler",
                            DiagnosticSeverity.ERROR, "return handler not found for the type: " + symbol.signature()));
        }
    }
}
