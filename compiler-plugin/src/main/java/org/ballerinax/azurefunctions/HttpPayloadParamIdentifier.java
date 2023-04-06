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
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinax.azurefunctions.context.DocumentContext;
import org.ballerinax.azurefunctions.context.ParamAvailability;
import org.ballerinax.azurefunctions.context.ParamData;
import org.ballerinax.azurefunctions.context.ResourceContext;
import org.ballerinax.azurefunctions.context.ServiceContext;
import org.ballerinax.azurefunctions.validators.http.HttpServiceValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.ballerinax.azurefunctions.Constants.ANYDATA;
import static org.ballerinax.azurefunctions.Constants.GET;
import static org.ballerinax.azurefunctions.Constants.HEAD;
import static org.ballerinax.azurefunctions.Constants.OPTIONS;
import static org.ballerinax.azurefunctions.Constants.PAYLOAD_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Util.diagnosticContainsErrors;
import static org.ballerinax.azurefunctions.Util.getCtxTypes;
import static org.ballerinax.azurefunctions.Util.getEffectiveTypeFromReadonlyIntersection;
import static org.ballerinax.azurefunctions.Util.updateDiagnostic;
import static org.ballerinax.azurefunctions.validators.http.HttpServiceValidator.isNilableType;
import static org.ballerinax.azurefunctions.validators.http.HttpServiceValidator.isStructuredType;
import static org.ballerinax.azurefunctions.validators.http.HttpServiceValidator.subtypeOf;

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
        Map<String, TypeSymbol> typeSymbols = getCtxTypes(syntaxNodeAnalysisContext);
        SyntaxKind kind = syntaxNodeAnalysisContext.node().kind();
        if (kind == SyntaxKind.SERVICE_DECLARATION) {
            validateServiceDeclaration(syntaxNodeAnalysisContext, typeSymbols);
        }
    }

    private void validateServiceDeclaration(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                            Map<String, TypeSymbol> typeSymbols) {
        ServiceDeclarationNode serviceDeclarationNode = Util.getServiceDeclarationNode(syntaxNodeAnalysisContext);
        if (serviceDeclarationNode == null) {
            return;
        }
        NodeList<Node> members = serviceDeclarationNode.members();
        ServiceContext serviceContext = new ServiceContext(serviceDeclarationNode.hashCode());
        for (Node member : members) {
            if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                validateResource(syntaxNodeAnalysisContext, (FunctionDefinitionNode) member, serviceContext,
                        typeSymbols);
            }
        }
    }

    void validateResource(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member, ServiceContext serviceContext,
                          Map<String, TypeSymbol> typeSymbols) {
        extractInputParamTypeAndValidate(ctx, member, serviceContext, typeSymbols);
    }

    void extractInputParamTypeAndValidate(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member,
                                          ServiceContext serviceContext, Map<String, TypeSymbol> typeSymbols) {

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
        // Disable error diagnostic in the code modifier since this validation is also done in the code analyzer
        paramAvailability.setErrorDiagnostic(false);
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

            if (validateNonAnnotatedParams(ctx, parameterSymbol.typeDescriptor(),
                    paramAvailability, parameterSymbol, typeSymbols)) {
                ResourceContext resourceContext =
                        new ResourceContext(parameterSymbol, nonAnnotatedParam.getIndex());
                DocumentContext documentContext = documentContextMap.get(ctx.documentId());
                if (documentContext == null) {
                    documentContext = new DocumentContext(ctx);
                    documentContextMap.put(ctx.documentId(), documentContext);
                }
                serviceContext.setResourceContext(resourceId, resourceContext);
                documentContext.setServiceContext(serviceContext);
            }
            if (paramAvailability.isErrorOccurred()) {
                serviceContext.removeResourceContext(resourceId);
                return;
            }
        }
    }

    public static void validateAnnotatedParams(ParameterSymbol parameterSymbol, ParamAvailability paramAvailability) {
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

    public static boolean validateNonAnnotatedParams(SyntaxNodeAnalysisContext analysisContext,
                                                     TypeSymbol typeSymbol, ParamAvailability paramAvailability,
                                                     ParameterSymbol parameterSymbol,
                                                     Map<String, TypeSymbol> typeSymbols) {
        typeSymbol = getEffectiveType(typeSymbol);
        if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            if (isUnionStructuredType(analysisContext, (UnionTypeSymbol) typeSymbol, parameterSymbol,
                    paramAvailability, typeSymbols)) {
                if (!subtypeOf(typeSymbols, typeSymbol, ANYDATA)) {
                    HttpServiceValidator.reportInvalidPayloadParameterType(analysisContext,
                            parameterSymbol.getLocation().get(), parameterSymbol.typeDescriptor().signature());
                    paramAvailability.setErrorOccurred(true);
                    return false;
                }
                return checkErrorsAndReturn(analysisContext, paramAvailability, parameterSymbol);
            }
        }
        if (isStructuredType(typeSymbols, typeSymbol)) {
            if (!subtypeOf(typeSymbols, typeSymbol, ANYDATA)) {
                HttpServiceValidator.reportInvalidPayloadParameterType(analysisContext,
                        parameterSymbol.getLocation().get(), parameterSymbol.typeDescriptor().signature());
                paramAvailability.setErrorOccurred(true);
                return false;
            }
            return checkErrorsAndReturn(analysisContext, paramAvailability, parameterSymbol);
        }
        return false;
    }

    private static boolean isUnionStructuredType(SyntaxNodeAnalysisContext ctx, UnionTypeSymbol unionTypeSymbol,
                                                 ParameterSymbol parameterSymbol, ParamAvailability paramAvailability,
                                                 Map<String, TypeSymbol> typeSymbols) {
        List<TypeSymbol> typeDescriptors = unionTypeSymbol.memberTypeDescriptors();
        boolean foundNonStructuredType = false;
        boolean foundStructuredType = false;
        for (TypeSymbol symbol : typeDescriptors) {
            if (isNilableType(typeSymbols, symbol)) {
                continue;
            }
            if (isStructuredType(typeSymbols, symbol)) {
                foundStructuredType = true;
                if (foundNonStructuredType) {
                    reportInvalidUnionPayloadParam(ctx, parameterSymbol, paramAvailability);
                    return false;
                }
            } else {
                foundNonStructuredType = true;
                if (foundStructuredType) {
                    reportInvalidUnionPayloadParam(ctx, parameterSymbol, paramAvailability);
                    return false;
                }
            }
        }
        return foundStructuredType;
    }

    public static TypeSymbol getEffectiveType(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return getEffectiveType(getEffectiveTypeFromTypeReference(typeSymbol));
        } else if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            return getEffectiveType(getEffectiveTypeFromReadonlyIntersection((IntersectionTypeSymbol) typeSymbol));
        }
        return typeSymbol;
    }

    public static TypeSymbol getEffectiveTypeFromTypeReference(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return getEffectiveTypeFromTypeReference(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        }
        return typeSymbol;
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
