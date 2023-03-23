/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinax.azurefunctions.context.DocumentContext;
import org.ballerinax.azurefunctions.context.ParamAvailability;
import org.ballerinax.azurefunctions.context.ParamData;
import org.ballerinax.azurefunctions.context.ResourceContext;
import org.ballerinax.azurefunctions.context.ServiceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.ballerinax.azurefunctions.Constants.GET;
import static org.ballerinax.azurefunctions.Constants.HEAD;
import static org.ballerinax.azurefunctions.Constants.OPTIONS;
import static org.ballerinax.azurefunctions.Constants.PAYLOAD_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Util.diagnosticContainsErrors;
import static org.ballerinax.azurefunctions.Util.updateDiagnostic;

/**
 * {@code HttpPayloadParamIdentifier} identifies the payload param during the initial syntax node analysis.
 *
 * @since 2201.5.0
 */
public class HttpPayloadParamIdentifier implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final Map<DocumentId, DocumentContext> documentContextMap;

    public HttpPayloadParamIdentifier(Map<DocumentId, DocumentContext> documentContextMap) {
        this.documentContextMap = documentContextMap;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        if (diagnosticContainsErrors(syntaxNodeAnalysisContext)) {
            return;
        }
        SyntaxKind kind = syntaxNodeAnalysisContext.node().kind();
        if (kind == SyntaxKind.SERVICE_DECLARATION) {
            validateServiceDeclaration(syntaxNodeAnalysisContext);
        } else if (kind == SyntaxKind.CLASS_DEFINITION) {
            validateClassDefinition(syntaxNodeAnalysisContext);
        }
    }

    private void validateServiceDeclaration(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ServiceDeclarationNode serviceDeclarationNode = Util.getServiceDeclarationNode(syntaxNodeAnalysisContext);
        if (serviceDeclarationNode == null) {
            return;
        }
        NodeList<Node> members = serviceDeclarationNode.members();
        ServiceContext serviceContext = new ServiceContext(serviceDeclarationNode.hashCode());
        for (Node member : members) {
            if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                validateResource(syntaxNodeAnalysisContext, (FunctionDefinitionNode) member, serviceContext);
            }
        }
    }

    private void validateClassDefinition(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) syntaxNodeAnalysisContext.node();
        NodeList<Token> tokens = classDefinitionNode.classTypeQualifiers();
        if (tokens.isEmpty()) {
            return;
        }
        if (!tokens.stream().allMatch(token -> token.text().equals(Constants.SERVICE_KEYWORD))) {
            return;
        }
        NodeList<Node> members = classDefinitionNode.members();
        ServiceContext serviceContext = new ServiceContext(classDefinitionNode.hashCode());
        boolean proceed = false;
        for (Node member : members) {
            if (member.kind() == SyntaxKind.TYPE_REFERENCE) {
                String typeReference = ((TypeReferenceNode) member).typeName().toString();
                if (typeReference.equals(Constants.AZF_SERVICE)) {
                    proceed = true;
                }
            }
        }
        if (proceed) {
            for (Node member : members) {
                if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                    validateResource(syntaxNodeAnalysisContext, (FunctionDefinitionNode) member, serviceContext);
                }
            }
        }
    }

    void validateResource(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member, ServiceContext serviceContext) {
        extractInputParamTypeAndValidate(ctx, member, serviceContext);
    }

    void extractInputParamTypeAndValidate(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member,
                                          ServiceContext serviceContext) {

        Optional<Symbol> resourceMethodSymbolOptional = ctx.semanticModel().symbol(member);
        int resourceId = member.hashCode();
        if (resourceMethodSymbolOptional.isEmpty()) {
            return;
        }
        Optional<String> resourceMethodOptional = resourceMethodSymbolOptional.get().getName();

        if (resourceMethodOptional.isPresent()) {
            String accessor = resourceMethodOptional.get();
            if (accessor.equals(GET) || accessor.equals(HEAD) || accessor.equals(OPTIONS)) {
                return; // No modification is done for non-entity body resources
            }
        } else {
            return; // No modification is done for non resources functions
        }

        Optional<List<ParameterSymbol>> parametersOptional =
                ((ResourceMethodSymbol) resourceMethodSymbolOptional.get()).typeDescriptor().params();
        if (parametersOptional.isEmpty()) {
            return; // No modification is done for non param resources functions
        }

        List<ParamData> nonAnnotatedParams = new ArrayList<>();
        List<ParamData> annotatedParams = new ArrayList<>();
        ParamAvailability paramAvailability = new ParamAvailability();
        int index = 0;
        for (ParameterSymbol param : parametersOptional.get()) {
            List<AnnotationSymbol> annotations = param.annotations().stream()
                    .filter(annotationSymbol -> annotationSymbol.typeDescriptor().isPresent())
                    .collect(Collectors.toList());
            if (annotations.isEmpty()) {
                nonAnnotatedParams.add(new ParamData(param, index++));
            } else {
                annotatedParams.add(new ParamData(param, index++));
            }
        }

        for (ParamData annotatedParam : annotatedParams) {
            validateAnnotatedParams(annotatedParam.getParameterSymbol(), paramAvailability);
            if (paramAvailability.isAnnotatedPayloadParam()) {
                return;
            }
        }

        for (ParamData nonAnnotatedParam : nonAnnotatedParams) {
            ParameterSymbol parameterSymbol = nonAnnotatedParam.getParameterSymbol();

            if (validateNonAnnotatedParams(ctx, parameterSymbol.typeDescriptor(), paramAvailability, parameterSymbol)) {
                ResourceContext resourceContext =
                        new ResourceContext(parameterSymbol, nonAnnotatedParam.getIndex());
                DocumentContext documentContext = documentContextMap.get(ctx.documentId());
                if (documentContext == null) {
                    documentContext = new DocumentContext(ctx);
                    documentContextMap.put(ctx.documentId(), documentContext);
                }
                serviceContext.setResourceContext(resourceId, resourceContext);
                documentContext.addServiceContext(serviceContext);
            }
            if (paramAvailability.isErrorOccurred()) {
                return;
            }
        }
    }

    private static void validateAnnotatedParams(ParameterSymbol parameterSymbol, ParamAvailability paramAvailability) {
        List<AnnotationSymbol> annotations = parameterSymbol.annotations().stream()
                .filter(annotationSymbol -> annotationSymbol.typeDescriptor().isPresent())
                .collect(Collectors.toList());
        for (AnnotationSymbol annotation : annotations) {
            Optional<TypeSymbol> annotationTypeSymbol = annotation.typeDescriptor();
            if (annotationTypeSymbol.isEmpty()) {
                return;
            }
            Optional<String> annotationTypeNameOptional = annotationTypeSymbol.get().getName();
            if (annotationTypeNameOptional.isEmpty()) {
                return;
            }
            String typeName = annotationTypeNameOptional.get();
            if (typeName.equals(PAYLOAD_ANNOTATION_TYPE)) {
                paramAvailability.setAnnotatedPayloadParam(true);
            }
        }
    }

    private static boolean validateNonAnnotatedParams(SyntaxNodeAnalysisContext analysisContext,
                                                      TypeSymbol typeSymbol, ParamAvailability paramAvailability,
                                                      ParameterSymbol parameterSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            typeSymbol = Util.getEffectiveTypeFromReadonlyIntersection((IntersectionTypeSymbol) typeSymbol);
            if (typeSymbol == null) {
                return false;
            }
        }
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
                return false;
            }
            TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            return validateNonAnnotatedParams(analysisContext, typeDescriptor, paramAvailability, parameterSymbol);
        }

        if (typeSymbol.subtypeOf(analysisContext.semanticModel().types().XML)) {
            return checkErrorsAndReturn(analysisContext, paramAvailability, parameterSymbol);
        }
        if (kind == TypeDescKind.RECORD || kind == TypeDescKind.MAP ||
                kind == TypeDescKind.TUPLE || kind == TypeDescKind.TABLE || kind == TypeDescKind.NIL) {
            return checkErrorsAndReturn(analysisContext, paramAvailability, parameterSymbol);
        } else if (kind == TypeDescKind.ARRAY) {
            return validateArrayElementType(analysisContext, typeSymbol, paramAvailability, parameterSymbol);
        } else if (kind == TypeDescKind.UNION) {
            List<TypeSymbol> typeDescriptors = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
            for (TypeSymbol symbol : typeDescriptors) {
                if (!validateNonAnnotatedParams(analysisContext, symbol, paramAvailability, parameterSymbol)) {
                    if (paramAvailability.isDefaultPayloadParam()) {
                        reportInvalidUnionPayloadParam(analysisContext, parameterSymbol, paramAvailability);
                        paramAvailability.setErrorOccurred(true);
                    }
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean validateArrayElementType(SyntaxNodeAnalysisContext analysisContext, TypeSymbol typeSymbol,
                                                    ParamAvailability availability, ParameterSymbol pSymbol) {
        TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
        TypeDescKind elementKind = Util.getReferencedTypeDescKind(arrTypeSymbol);

        if (elementKind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            return validateArrayElementType(analysisContext, typeDescriptor, availability, pSymbol);
        }

        if (elementKind == TypeDescKind.ARRAY) {
            arrTypeSymbol = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
            return validateArrayElementType(analysisContext, arrTypeSymbol, availability, pSymbol);
        }

        if (arrTypeSymbol.subtypeOf(analysisContext.semanticModel().types().XML)) {
            return checkErrorsAndReturn(analysisContext, availability, pSymbol);
        }

        if (elementKind == TypeDescKind.BYTE || elementKind == TypeDescKind.MAP || elementKind == TypeDescKind.RECORD ||
                elementKind == TypeDescKind.TUPLE || elementKind == TypeDescKind.TABLE) {
            return checkErrorsAndReturn(analysisContext, availability, pSymbol);
        }
        return false;
    }

    private static boolean checkErrorsAndReturn(SyntaxNodeAnalysisContext analysisContext,
                                                ParamAvailability availability, ParameterSymbol pSymbol) {
        if (availability.isDefaultPayloadParam() && isDistinctVariable(availability, pSymbol)) {
            reportAmbiguousPayloadParam(analysisContext, pSymbol, availability.getPayloadParamSymbol());
            availability.setErrorOccurred(true);
            return false;
        }
        availability.setPayloadParamSymbol(pSymbol);
        return true;
    }

    private static boolean isDistinctVariable(ParamAvailability availability, ParameterSymbol pSymbol) {
        return !pSymbol.getName().get().equals(availability.getPayloadParamSymbol().getName().get());
    }

    private static void reportAmbiguousPayloadParam(SyntaxNodeAnalysisContext analysisContext,
                                                    ParameterSymbol parameterSymbol, ParameterSymbol payloadSymbol) {
        updateDiagnostic(analysisContext, parameterSymbol.getLocation().get(), AzureDiagnosticCodes.AF_017,
                         payloadSymbol.getName().get(), parameterSymbol.getName().get());
    }

    private static void reportInvalidUnionPayloadParam(SyntaxNodeAnalysisContext analysisContext,
                                                       ParameterSymbol parameterSymbol,
                                                       ParamAvailability paramAvailability) {
        if (paramAvailability.isErrorOccurred()) {
            return;
        }
        updateDiagnostic(analysisContext, parameterSymbol.getLocation().get(), AzureDiagnosticCodes.AF_018,
                         parameterSymbol.getName().get());
    }
}
