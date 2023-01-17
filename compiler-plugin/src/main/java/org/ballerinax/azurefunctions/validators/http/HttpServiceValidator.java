/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.ballerinax.azurefunctions.validators.http;

import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinax.azurefunctions.AzureDiagnosticCodes;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;
import org.wso2.ballerinalang.compiler.diagnostic.properties.BSymbolicProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.ballerinax.azurefunctions.AzureDiagnosticCodes.AF_008;
import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_MODULE_NAME;
import static org.ballerinax.azurefunctions.Constants.COLON;
import static org.ballerinax.azurefunctions.Constants.HEADER_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Constants.HTTP;
import static org.ballerinax.azurefunctions.Constants.SERVICE_CONFIG_ANNOTATION;
import static org.ballerinax.azurefunctions.Constants.TREAT_NILABLE_AS_OPTIONAL;
import static org.ballerinax.azurefunctions.Util.updateDiagnostic;

/**
 * Validates azure-function service on a HTTPListener .
 */
public class HttpServiceValidator extends BaseHttpCodeAnalyzerTask {

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        if (!isHttpListener(syntaxNodeAnalysisContext)) {
            return;
        }
        
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        extractServiceAnnotationAndValidate(syntaxNodeAnalysisContext, serviceDeclarationNode);
        NodeList<Node> members = serviceDeclarationNode.members();
        for (Node member : members) {
            if (member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                FunctionDefinitionNode node = (FunctionDefinitionNode) member;
                NodeList<Token> tokens = node.qualifierList();
                if (tokens.isEmpty()) {
                    // Object methods are allowed.
                    continue;
                }
                if (tokens.stream().anyMatch(token -> token.text().equals(Constants.REMOTE_KEYWORD))) {
                    Diagnostic
                            diagnostic = Util.getDiagnostic(member.location(), AzureDiagnosticCodes.AF_015);
                    syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
                }
            } else if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                validateResourceFunction(syntaxNodeAnalysisContext, (FunctionDefinitionNode) member);
            }
        }
    }

    private static void validateResourceFunction(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member) {
        validateInputParameters(ctx, member);
        //TODO : Other necessary validation for a resource function
    }

    private static void extractServiceAnnotationAndValidate(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                                            ServiceDeclarationNode serviceDeclarationNode) {
        //HTTP serviceconfig validation currently supports only for treatNilableAsTrue field
        Optional<MetadataNode> metadataNodeOptional = serviceDeclarationNode.metadata();
        if (metadataNodeOptional.isEmpty()) {
            return;
        }
        NodeList<AnnotationNode> annotations = metadataNodeOptional.get().annotations();
        for (AnnotationNode annotation : annotations) {
            Node annotReference = annotation.annotReference();
            String annotName = annotReference.toString();
            Optional<MappingConstructorExpressionNode> annotValue = annotation.annotValue();
            if (annotReference.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                continue;
            }
            String[] annotStrings = annotName.split(COLON);
            if (SERVICE_CONFIG_ANNOTATION.equals(annotStrings[annotStrings.length - 1].trim())
                    && (annotValue.isPresent())) {
                MappingConstructorExpressionNode mapping = annotValue.get();
                NodeList fields = mapping.fields();
                if (fields.size() == 1) {
                    MappingFieldNode field = (MappingFieldNode) fields.get(0);
                    String fieldName = ((IdentifierToken) (field.children()).get(0)).text();
                    if (!TREAT_NILABLE_AS_OPTIONAL.equals(fieldName)) {
                        warnInvalidServiceConfig(syntaxNodeAnalysisContext, field);
                    }
                } else {
                    warnInvalidServiceConfig(syntaxNodeAnalysisContext, mapping);
                }
            }
        }

    }

    private static void validateInputParameters(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member) {
        Optional<Symbol> resourceMethodSymbolOptional = ctx.semanticModel().symbol(member);
        Location paramLocation = member.location();
        if (resourceMethodSymbolOptional.isEmpty()) {
            return;
        }
        Optional<List<ParameterSymbol>> parametersOptional =
                ((ResourceMethodSymbol) resourceMethodSymbolOptional.get()).typeDescriptor().params();
        if (parametersOptional.isEmpty()) {
            return;
        }

        for (ParameterSymbol param : parametersOptional.get()) {
            Optional<Location> paramLocationOptional = param.getLocation();
            if (paramLocationOptional.isPresent()) {
                paramLocation = paramLocationOptional.get();
            }
            Optional<String> nameOptional = param.getName();
            String paramName = nameOptional.isEmpty() ? "" : nameOptional.get();
            
            //TODO filter only azure and http annotations
            List<AnnotationSymbol> annotations = param.annotations().stream()
                    .filter(annotationSymbol -> annotationSymbol.typeDescriptor().isPresent())
                    .collect(Collectors.toList());

            if (!annotations.isEmpty()) {
                validateAnnotatedInputParam(ctx, paramLocation, param, paramName, annotations);
            } else {
                //Query params
                TypeSymbol typeSymbol = param.typeDescriptor();
                TypeDescKind kind = typeSymbol.typeKind();
                if (isAllowedQueryParamType(kind, typeSymbol)) {
                    continue;
                }
                if (kind == TypeDescKind.MAP) {
                    TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) typeSymbol).typeParam();
                    TypeDescKind constrainedType = getReferencedTypeDescKind(constrainedTypeSymbol);
                    if (constrainedType != TypeDescKind.JSON) {
                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
                        continue;
                    }
                } else if (kind == TypeDescKind.ARRAY) {
                    // Allowed query param array types
                    TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
                    TypeDescKind elementKind = getReferencedTypeDescKind(arrTypeSymbol);
                    if (elementKind == TypeDescKind.MAP) {
                        TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) arrTypeSymbol).typeParam();
                        TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
                        if (constrainedType != TypeDescKind.JSON) {
                            updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
                        }
                        continue;
                    }
                    if (!isAllowedQueryParamType(elementKind, arrTypeSymbol)) {
                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
                        continue;
                    }
                } else if (kind == TypeDescKind.UNION) {
                    // Allowed query param union types
                    List<TypeSymbol> symbolList = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
                    int size = symbolList.size();
                    if (size > 2) {
                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_011, paramName);
                        continue;
                    }
                    if (symbolList.stream().noneMatch(type -> type.typeKind() == TypeDescKind.NIL)) {
                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_011, paramName);
                        continue;
                    }
                    for (TypeSymbol type : symbolList) {
                        TypeDescKind elementKind = getReferencedTypeDescKind(type);
                        if (elementKind == TypeDescKind.ARRAY) {
                            TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) type).memberTypeDescriptor();
                            TypeDescKind arrElementKind = getReferencedTypeDescKind(arrTypeSymbol);
                            if (arrElementKind == TypeDescKind.MAP) {
                                TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) arrTypeSymbol).typeParam();
                                TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
                                if (constrainedType == TypeDescKind.JSON) {
                                    continue;
                                }
                            }
                            if (isAllowedQueryParamType(arrElementKind, arrTypeSymbol)) {
                                continue;
                            }
                        } else if (elementKind == TypeDescKind.MAP) {
                            TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) type).typeParam();
                            TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
                            if (constrainedType == TypeDescKind.JSON) {
                                continue;
                            }
                        } else {
                            if (elementKind == TypeDescKind.NIL || isAllowedQueryParamType(elementKind, type)) {
                                continue;
                            }
                        }
                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
                    }
                }  else {
                    updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
                }
            }
        }
    }

    private static boolean isAllowedQueryParamType(TypeDescKind kind, TypeSymbol typeSymbol) {
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            kind =  getReferencedTypeDescKind(typeDescriptor);
        }
        return kind == TypeDescKind.STRING || kind == TypeDescKind.INT || kind == TypeDescKind.FLOAT ||
                kind == TypeDescKind.DECIMAL || kind == TypeDescKind.BOOLEAN;
    }

    private static TypeDescKind getReferencedTypeDescKind(TypeSymbol typeSymbol) {
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            kind = getReferencedTypeDescKind(typeDescriptor);
        }
        return kind;
    }

    private static void validateAnnotatedInputParam(SyntaxNodeAnalysisContext ctx, Location paramLocation,
                                                    ParameterSymbol param, String paramName,
                                                    List<AnnotationSymbol> annotations) {

        for (AnnotationSymbol annotation : annotations) {
            Optional<TypeSymbol> typeSymbolOptional = annotation.typeDescriptor();
            if (typeSymbolOptional.isEmpty()) {
                reportInvalidParameter(ctx, paramLocation, paramName);
                continue;
            }
            // validate annotation module
            Optional<ModuleSymbol> moduleSymbolOptional = typeSymbolOptional.get().getModule();
            if (moduleSymbolOptional.isEmpty()) {
                reportInvalidParameter(ctx, paramLocation, paramName);
                continue;
            }
            Optional<String> nameSymbolOptional = moduleSymbolOptional.get().getName();
            if (nameSymbolOptional.isEmpty()) {
                reportInvalidParameter(ctx, paramLocation, paramName);
                continue;
            }
            if (!HTTP.equals(nameSymbolOptional.get()) &&
                    !AZURE_FUNCTIONS_MODULE_NAME.equals(nameSymbolOptional.get())) {
                reportInvalidParameterAnnotation(ctx, paramLocation, paramName);
                continue;
            }

            Optional<String> annotationTypeNameOptional = typeSymbolOptional.get().getName();
            if (annotationTypeNameOptional.isEmpty()) {
                reportInvalidParameter(ctx, paramLocation, paramName);
                continue;
            }
            String typeName = annotationTypeNameOptional.get();
            TypeSymbol typeDescriptor = param.typeDescriptor();
            if (typeDescriptor.typeKind() == TypeDescKind.INTERSECTION) {
                typeDescriptor =
                        getEffectiveTypeFromReadonlyIntersection((IntersectionTypeSymbol) typeDescriptor);
                if (typeDescriptor == null) {
                    reportInvalidIntersectionType(ctx, paramLocation, typeName);
                    continue;
                }
            }
            if (HEADER_ANNOTATION_TYPE.equals(typeName)) {
                if (annotations.size() == 2) {
                    reportInvalidMultipleAnnotation(ctx, paramLocation, paramName);
                    continue;
                }
                validateHeaderParamType(ctx, paramLocation, param, paramName, typeDescriptor);
                break;
            }
        }
    }

    private static void validateHeaderParamType(SyntaxNodeAnalysisContext ctx, Location paramLocation, Symbol param,
                                                String paramName, TypeSymbol paramTypeDescriptor) {
        switch (paramTypeDescriptor.typeKind()) {
            case STRING:
            case INT:
            case DECIMAL:
            case FLOAT:
            case BOOLEAN:
                break;
            case ARRAY:
                TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) paramTypeDescriptor).memberTypeDescriptor();
                TypeDescKind arrElementKind = arrTypeSymbol.typeKind();
                checkAllowedHeaderParamTypes(ctx, paramLocation, param, paramName, arrElementKind);
                break;
            case UNION:
                List<TypeSymbol> symbolList = ((UnionTypeSymbol) paramTypeDescriptor).memberTypeDescriptors();
                int size = symbolList.size();
                if (size > 2) {
                    reportInvalidUnionHeaderType(ctx, paramLocation, paramName);
                    return;
                }
                if (symbolList.stream().noneMatch(type -> type.typeKind() == TypeDescKind.NIL)) {
                    reportInvalidUnionHeaderType(ctx, paramLocation, paramName);
                    return;
                }
                for (TypeSymbol type : symbolList) {
                    TypeDescKind elementKind = type.typeKind();
                    if (elementKind == TypeDescKind.ARRAY) {
                        elementKind = ((ArrayTypeSymbol) type).memberTypeDescriptor().typeKind();
                        checkAllowedHeaderParamUnionType(ctx, paramLocation, param, paramName, elementKind);
                        continue;
                    }
                    if (elementKind == TypeDescKind.TYPE_REFERENCE) {
                        validateHeaderParamType(ctx, paramLocation, param, paramName, type);
                        return;
                    }
                    checkAllowedHeaderParamTypes(ctx, paramLocation, param, paramName, elementKind);
                }
                break;
            case TYPE_REFERENCE:
                TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) paramTypeDescriptor).typeDescriptor();
                TypeDescKind typeDescKind = typeDescriptor.typeKind();
                if (typeDescKind == TypeDescKind.RECORD) {
                    validateHeaderRecordFields(ctx, paramLocation, typeDescriptor);
                } else {
                    reportInvalidHeaderParameterType(ctx, paramLocation, paramName, param);
                }
                break;
            case RECORD:
                validateHeaderRecordFields(ctx, paramLocation, paramTypeDescriptor);
                break;
            default:
                reportInvalidHeaderParameterType(ctx, paramLocation, paramName, param);
                break;
        }
    }

    private static void checkAllowedHeaderParamTypes(SyntaxNodeAnalysisContext ctx, Location paramLocation,
                                                     Symbol param, String paramName, TypeDescKind elementKind) {
        if (!isAllowedHeaderParamPureType(elementKind)) {
            reportInvalidHeaderParameterType(ctx, paramLocation, paramName, param);
        }
    }

    private static void checkAllowedHeaderParamUnionType(SyntaxNodeAnalysisContext ctx, Location paramLocation,
                                                         Symbol param, String paramName, TypeDescKind elementKind) {
        if (!isAllowedHeaderParamPureType(elementKind)) {
            reportInvalidUnionHeaderType(ctx, paramLocation, paramName);
        }
    }

    private static boolean isAllowedHeaderParamPureType(TypeDescKind elementKind) {
        return elementKind == TypeDescKind.NIL || elementKind == TypeDescKind.STRING ||
                elementKind == TypeDescKind.INT || elementKind == TypeDescKind.FLOAT ||
                elementKind == TypeDescKind.DECIMAL || elementKind == TypeDescKind.BOOLEAN;
    }

    private static void validateHeaderRecordFields(SyntaxNodeAnalysisContext ctx, Location paramLocation,
                                                   TypeSymbol typeDescriptor) {
        RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeDescriptor;
        Map<String, RecordFieldSymbol> recordFieldSymbolMap = recordTypeSymbol.fieldDescriptors();
        for (Map.Entry<String, RecordFieldSymbol> entry : recordFieldSymbolMap.entrySet()) {
            RecordFieldSymbol value = entry.getValue();
            typeDescriptor = value.typeDescriptor();
            String typeName = typeDescriptor.signature();
            TypeDescKind typeDescKind = typeDescriptor.typeKind();
            if (typeDescKind == TypeDescKind.INTERSECTION) {
                typeDescriptor = getEffectiveTypeFromReadonlyIntersection((IntersectionTypeSymbol) typeDescriptor);
                if (typeDescriptor == null) {
                    reportInvalidIntersectionType(ctx, paramLocation, typeName);
                    continue;
                }
            }
            validateHeaderParamType(ctx, paramLocation, value, entry.getKey(), typeDescriptor);
        }
        Optional<TypeSymbol> restTypeDescriptor = recordTypeSymbol.restTypeDescriptor();
        if (restTypeDescriptor.isPresent()) {
            reportInvalidHeaderRecordRestFieldType(ctx, paramLocation);
        }
    }

    private static TypeSymbol getEffectiveTypeFromReadonlyIntersection(IntersectionTypeSymbol intersectionTypeSymbol) {
        List<TypeSymbol> effectiveTypes = new ArrayList<>();
        for (TypeSymbol typeSymbol : intersectionTypeSymbol.memberTypeDescriptors()) {
            if (typeSymbol.typeKind() == TypeDescKind.READONLY) {
                continue;
            }
            effectiveTypes.add(typeSymbol);
        }
        if (effectiveTypes.size() == 1) {
            return effectiveTypes.get(0);
        }
        return null;
    }

    private static void reportInvalidParameterAnnotation(SyntaxNodeAnalysisContext ctx, Location location,
                                                         String paramName) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_001, paramName);
    }

    private static void reportInvalidParameter(SyntaxNodeAnalysisContext ctx, Location location,
                                               String paramName) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_002, paramName);
    }

    private static void reportInvalidHeaderParameterType(SyntaxNodeAnalysisContext ctx, Location location,
                                                         String paramName, Symbol parameterSymbol) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_003, List.of(new BSymbolicProperty(parameterSymbol))
                , paramName);
    }

    private static void reportInvalidUnionHeaderType(SyntaxNodeAnalysisContext ctx, Location location,
                                                     String paramName) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_004, paramName);
    }

    private static void reportInvalidIntersectionType(SyntaxNodeAnalysisContext ctx, Location location,
                                                      String typeName) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_005, typeName);
    }

    private static void reportInvalidHeaderRecordRestFieldType(SyntaxNodeAnalysisContext ctx, Location location) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_006);
    }

    private static void reportInvalidMultipleAnnotation(SyntaxNodeAnalysisContext ctx, Location location,
                                                        String paramName) {
        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_007, paramName);
    }

    private static void warnInvalidServiceConfig(SyntaxNodeAnalysisContext ctx, Node node) {
        DiagnosticInfo diagInfo = new DiagnosticInfo(AF_008.getCode(), AF_008.getMessage(), AF_008.getSeverity());
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagInfo, node.location()));
    }
}


