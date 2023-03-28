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
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
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
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.context.ParamAvailability;
import org.ballerinax.azurefunctions.context.ParamData;
import org.wso2.ballerinalang.compiler.diagnostic.properties.BSymbolicProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ballerinax.azurefunctions.AzureDiagnosticCodes.AF_008;
import static org.ballerinax.azurefunctions.Constants.ANYDATA;
import static org.ballerinax.azurefunctions.Constants.ARRAY_OF_MAP_OF_JSON;
import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_MODULE_NAME;
import static org.ballerinax.azurefunctions.Constants.BALLERINA_ORG;
import static org.ballerinax.azurefunctions.Constants.BOOLEAN;
import static org.ballerinax.azurefunctions.Constants.BOOLEAN_ARRAY;
import static org.ballerinax.azurefunctions.Constants.BYTE_ARRAY;
import static org.ballerinax.azurefunctions.Constants.COLON;
import static org.ballerinax.azurefunctions.Constants.DECIMAL;
import static org.ballerinax.azurefunctions.Constants.DECIMAL_ARRAY;
import static org.ballerinax.azurefunctions.Constants.FLOAT;
import static org.ballerinax.azurefunctions.Constants.FLOAT_ARRAY;
import static org.ballerinax.azurefunctions.Constants.GET;
import static org.ballerinax.azurefunctions.Constants.HEAD;
import static org.ballerinax.azurefunctions.Constants.HEADER_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Constants.HTTP;
import static org.ballerinax.azurefunctions.Constants.INT;
import static org.ballerinax.azurefunctions.Constants.INT_ARRAY;
import static org.ballerinax.azurefunctions.Constants.MAP_OF_ANYDATA;
import static org.ballerinax.azurefunctions.Constants.MAP_OF_JSON;
import static org.ballerinax.azurefunctions.Constants.MIME_ENTITY_OBJECT;
import static org.ballerinax.azurefunctions.Constants.NIL;
import static org.ballerinax.azurefunctions.Constants.OPTIONS;
import static org.ballerinax.azurefunctions.Constants.PAYLOAD_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Constants.QUERY_ANNOTATION_TYPE;
import static org.ballerinax.azurefunctions.Constants.REMOTE_KEYWORD;
import static org.ballerinax.azurefunctions.Constants.SERVICE_CONFIG_ANNOTATION;
import static org.ballerinax.azurefunctions.Constants.STRING;
import static org.ballerinax.azurefunctions.Constants.STRING_ARRAY;
import static org.ballerinax.azurefunctions.Constants.STRUCTURED_ARRAY;
import static org.ballerinax.azurefunctions.Constants.TABLE_OF_ANYDATA_MAP;
import static org.ballerinax.azurefunctions.Constants.TREAT_NILABLE_AS_OPTIONAL;
import static org.ballerinax.azurefunctions.Constants.TUPLE_OF_ANYDATA;
import static org.ballerinax.azurefunctions.Constants.XML;
import static org.ballerinax.azurefunctions.HttpPayloadParamIdentifier.validateAnnotatedParams;
import static org.ballerinax.azurefunctions.HttpPayloadParamIdentifier.validateNonAnnotatedParams;
import static org.ballerinax.azurefunctions.Util.getCtxTypes;
import static org.ballerinax.azurefunctions.Util.getEffectiveTypeFromReadonlyIntersection;
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
                if (tokens.stream().anyMatch(token -> token.text().equals(REMOTE_KEYWORD))) {
                    Diagnostic
                            diagnostic = Util.getDiagnostic(member.location(), AzureDiagnosticCodes.AF_015);
                    syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
                }
            } else if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                validateResourceFunction(syntaxNodeAnalysisContext, (FunctionDefinitionNode) member,
                        getCtxTypes(syntaxNodeAnalysisContext));
            }
        }
    }

    private static void validateResourceFunction(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member,
                                                 Map<String, TypeSymbol> typeSymbols) {

        validateInputParameters(ctx, member, typeSymbols);
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

    public static List<Integer> mockCodeModifier(SyntaxNodeAnalysisContext ctx, Map<String, TypeSymbol> typeSymbols,
                                                 Optional<List<ParameterSymbol>> parametersOptional) {
        List<ParamData> nonAnnotatedParams = new ArrayList<>();
        List<ParamData> annotatedParams = new ArrayList<>();
        List<Integer> analyzedParams = new ArrayList<>();
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
                return analyzedParams;
            }
        }

        for (ParamData nonAnnotatedParam : nonAnnotatedParams) {
            ParameterSymbol parameterSymbol = nonAnnotatedParam.getParameterSymbol();

            if (validateNonAnnotatedParams(ctx, parameterSymbol.typeDescriptor(), paramAvailability,
                    parameterSymbol, typeSymbols)) {
                analyzedParams.add(parameterSymbol.hashCode());
            }
            if (paramAvailability.isErrorOccurred()) {
                analyzedParams.add(parameterSymbol.hashCode());
                break;
            }
        }

        return analyzedParams;
    }

    private static void validateInputParameters(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode member,
                                                Map<String, TypeSymbol> typeSymbols) {

        Optional<Symbol> resourceMethodSymbolOptional = ctx.semanticModel().symbol(member);
        Location paramLocation = member.location();
        if (resourceMethodSymbolOptional.isEmpty()) {
            return;
        }
        Optional<String> resourceMethodOptional = resourceMethodSymbolOptional.get().getName();
        Optional<List<ParameterSymbol>> parametersOptional =
                ((ResourceMethodSymbol) resourceMethodSymbolOptional.get()).typeDescriptor().params();
        if (parametersOptional.isEmpty()) {
            return;
        }
        // Mocking code modifier here since LS does not run code modifiers
        // Related issue: https://github.com/ballerina-platform/ballerina-lang/issues/39792
        List<Integer> analyzedParams = new ArrayList<>();
        if (resourceMethodOptional.isPresent()) {
            String accessor = resourceMethodOptional.get();
            if (Stream.of(GET, HEAD, OPTIONS).noneMatch(accessor::equals)) {
                analyzedParams = mockCodeModifier(ctx, typeSymbols, parametersOptional);
            }
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
                boolean annotated = false;
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
                    if (AZURE_FUNCTIONS_MODULE_NAME.equals(nameSymbolOptional.get())) {
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
                    switch (typeName) {
                        case HEADER_ANNOTATION_TYPE:
                            if (annotated) {
                                reportInvalidMultipleAnnotation(ctx, paramLocation, paramName);
                                continue;
                            }
                            validateHeaderParamType(ctx, param, paramLocation, paramName, typeDescriptor,
                                    typeSymbols, false);
                            annotated = true;
                            break;
                        case PAYLOAD_ANNOTATION_TYPE:
                            if (annotated) { // multiple annotations
                                reportInvalidMultipleAnnotation(ctx, paramLocation, paramName);
                                continue;
                            }
                            validatePayloadParamType(ctx, typeSymbols, paramLocation,
                                    resourceMethodOptional.orElse(null), param, typeDescriptor);
                            annotated = true;
                            break;
                        case QUERY_ANNOTATION_TYPE: {
                            if (annotated) {
                                reportInvalidMultipleAnnotation(ctx, paramLocation, paramName);
                                continue;
                            }
                            annotated = true;
                            validateQueryParamType(ctx, paramLocation, paramName, typeDescriptor, typeSymbols);
                            break;
                        }
                        default:
                            reportInvalidParameterAnnotation(ctx, paramLocation, paramName);
                            break;
                    }
                }
            } else {
//                //Query params
//                TypeSymbol typeSymbol = param.typeDescriptor();
//                TypeDescKind kind = typeSymbol.typeKind();
//                if (isAllowedQueryParamType(kind, typeSymbol)) {
//                    continue;
//                }
//                if (kind == TypeDescKind.MAP) {
//                    TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) typeSymbol).typeParam();
//                    TypeDescKind constrainedType = Util.getReferencedTypeDescKind(constrainedTypeSymbol);
//                    if (constrainedType != TypeDescKind.JSON) {
//                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
//                        continue;
//                    }
//                } else if (kind == TypeDescKind.ARRAY) {
//                    // Allowed query param array types
//                    TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
//                    TypeDescKind elementKind = Util.getReferencedTypeDescKind(arrTypeSymbol);
//                    if (elementKind == TypeDescKind.MAP) {
//                        TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) arrTypeSymbol).typeParam();
//                        TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
//                        if (constrainedType != TypeDescKind.JSON) {
//                            updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
//                        }
//                        continue;
//                    }
//                    if (!isAllowedQueryParamType(elementKind, arrTypeSymbol)) {
//                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
//                        continue;
//                    }
//                } else if (kind == TypeDescKind.UNION) {
//                    // Allowed query param union types
//                    List<TypeSymbol> symbolList = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
//                    int size = symbolList.size();
//                    if (size > 2) {
//                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_011, paramName);
//                        continue;
//                    }
//                    if (symbolList.stream().noneMatch(type -> type.typeKind() == TypeDescKind.NIL)) {
//                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_011, paramName);
//                        continue;
//                    }
//                    for (TypeSymbol type : symbolList) {
//                        TypeDescKind elementKind = Util.getReferencedTypeDescKind(type);
//                        if (elementKind == TypeDescKind.ARRAY) {
//                            TypeSymbol arrTypeSymbol = ((ArrayTypeSymbol) type).memberTypeDescriptor();
//                            TypeDescKind arrElementKind = Util.getReferencedTypeDescKind(arrTypeSymbol);
//                            if (arrElementKind == TypeDescKind.MAP) {
//                                TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) arrTypeSymbol).typeParam();
//                                TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
//                                if (constrainedType == TypeDescKind.JSON) {
//                                    continue;
//                                }
//                            }
//                            if (isAllowedQueryParamType(arrElementKind, arrTypeSymbol)) {
//                                continue;
//                            }
//                        } else if (elementKind == TypeDescKind.MAP) {
//                            TypeSymbol constrainedTypeSymbol = ((MapTypeSymbol) type).typeParam();
//                            TypeDescKind constrainedType = constrainedTypeSymbol.typeKind();
//                            if (constrainedType == TypeDescKind.JSON) {
//                                continue;
//                            }
//                        } else {
//                            if (elementKind == TypeDescKind.NIL || isAllowedQueryParamType(elementKind, type)) {
//                                continue;
//                            }
//                        }
//                        updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
//                    }
//                }  else {
//                    updateDiagnostic(ctx, paramLocation, AzureDiagnosticCodes.AF_010, paramName);
//                }
                TypeSymbol typeSymbol = param.typeDescriptor();
                if (!analyzedParams.contains(param.hashCode())) {
                    validateQueryParamType(ctx, paramLocation, paramName, typeSymbol, typeSymbols);
                }
            }
        }
    }

    public static void validateQueryParamType(SyntaxNodeAnalysisContext ctx, Location paramLocation, String paramName,
                                              TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        typeSymbol = getEffectiveTypeFromNilableSingletonType(typeSymbol, typeSymbols);
        if (typeSymbol == null) {
            reportInvalidUnionQueryType(ctx, paramLocation, paramName);
            return;
        }
        if (isValidBasicQueryParameterType(typeSymbol, typeSymbols)) {
            return;
        }
        TypeDescKind typeDescKind = typeSymbol.typeKind();
        if (typeDescKind == TypeDescKind.INTERSECTION) {
            reportInvalidIntersectionType(ctx, paramLocation, paramName);
            return;
        }
        reportInvalidQueryParameterType(ctx, paramLocation, paramName);
    }

    private static void reportInvalidQueryParameterType(SyntaxNodeAnalysisContext ctx, Location location,
                                                        String paramName) {

        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_010, paramName);
    }

    private static boolean isValidBasicQueryParameterType(TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        return isValidBasicParamType(typeSymbol, typeSymbols) || isMapOfJsonType(typeSymbol, typeSymbols) ||
                isArrayOfMapOfJsonType(typeSymbol, typeSymbols);
    }

    private static boolean isMapOfJsonType(TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        return subtypeOf(typeSymbols, typeSymbol, MAP_OF_JSON);
    }

    private static boolean isArrayOfMapOfJsonType(TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        return subtypeOf(typeSymbols, typeSymbol, ARRAY_OF_MAP_OF_JSON);
    }

    private static void reportInvalidUnionQueryType(SyntaxNodeAnalysisContext ctx, Location location,
                                                    String paramName) {

        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_011, paramName);
    }

    private static void validatePayloadParamType(SyntaxNodeAnalysisContext ctx, Map<String, TypeSymbol> typeSymbols,
                                                 Location paramLocation, String resourceMethodOptional,
                                                 ParameterSymbol param, TypeSymbol typeDescriptor) {

        if (resourceMethodOptional != null) {
            validatePayloadAnnotationUsage(ctx, paramLocation, resourceMethodOptional);
        }
        if (subtypeOf(typeSymbols, typeDescriptor, ANYDATA)) {
            return;
        }
        if (isMimeEntity(typeDescriptor)) {
            return;
        }
        reportInvalidPayloadParameterType(ctx, paramLocation, param.typeDescriptor().signature());
    }

    private static boolean isMimeEntity(TypeSymbol typeDescriptor) {

        Optional<String> name = typeDescriptor.getName();
        if (name.isEmpty()) {
            return false;
        }
        if (!name.get().equals(MIME_ENTITY_OBJECT)) {
            return false;
        }
        Optional<ModuleSymbol> module = typeDescriptor.getModule();
        if (module.isEmpty()) {
            return false;
        }
        String org = module.get().id().orgName();
        return org.equals(BALLERINA_ORG);

    }
    private static void validatePayloadAnnotationUsage(SyntaxNodeAnalysisContext ctx, Location location,
                                                       String methodName) {

        if (methodName.equals(GET) || methodName.equals(HEAD) || methodName.equals(OPTIONS)) {
            reportInvalidUsageOfPayloadAnnotation(ctx, location, methodName, AzureDiagnosticCodes.AF_019);
        }
    }

    public static void reportInvalidPayloadParameterType(SyntaxNodeAnalysisContext ctx, Location location,
                                                          String typeName) {

        updateDiagnostic(ctx, location, AzureDiagnosticCodes.AF_020, typeName);
    }

    private static void reportInvalidUsageOfPayloadAnnotation(SyntaxNodeAnalysisContext ctx, Location location,
                                                              String name, AzureDiagnosticCodes code) {

        updateDiagnostic(ctx, location, code, name);
    }

    public static boolean subtypeOf(Map<String, TypeSymbol> typeSymbols, TypeSymbol typeSymbol,
                                    String targetTypeName) {

        TypeSymbol targetTypeSymbol = typeSymbols.get(targetTypeName);
        if (targetTypeSymbol != null) {
            return typeSymbol.subtypeOf(targetTypeSymbol);
        }
        return false;
    }

    public static void validateHeaderParamType(SyntaxNodeAnalysisContext ctx, ParameterSymbol param,
                                               Location paramLocation, String paramName, TypeSymbol typeSymbol,
                                               Map<String, TypeSymbol> typeSymbols, boolean isRecordField) {

        typeSymbol = getEffectiveTypeFromNilableSingletonType(typeSymbol, typeSymbols);
        if (typeSymbol == null) {
            reportInvalidUnionHeaderType(ctx, paramLocation, paramName);
            return;
        }
        if (isValidBasicParamType(typeSymbol, typeSymbols)) {
            return;
        }
        typeSymbol = getEffectiveTypeFromTypeReference(typeSymbol);
        TypeDescKind typeDescKind = typeSymbol.typeKind();
        if (!isRecordField && typeDescKind == TypeDescKind.RECORD) {
            validateRecordFieldsOfHeaderParam(ctx, param, paramLocation, paramName, typeSymbol, typeSymbols);
            return;
        }
        reportInvalidHeaderParameterType(ctx, paramLocation, paramName, param);
    }

    private static void validateRecordFieldsOfHeaderParam(SyntaxNodeAnalysisContext ctx, ParameterSymbol param,
                                                          Location paramLocation, String paramName,
                                                          TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        Optional<TypeSymbol> restTypeSymbol = ((RecordTypeSymbol) typeSymbol).restTypeDescriptor();
        if (restTypeSymbol.isPresent()) {
            reportInvalidHeaderRecordRestFieldType(ctx, paramLocation);
        }
        Collection<RecordFieldSymbol> recordFields = ((RecordTypeSymbol) typeSymbol).fieldDescriptors().values();
        for (RecordFieldSymbol recordField : recordFields) {
            validateHeaderParamType(ctx, param, paramLocation, recordField.getName().orElse(paramName),
                    recordField.typeDescriptor(), typeSymbols, true);
        }
    }

    private static TypeSymbol getEffectiveTypeFromTypeReference(TypeSymbol typeSymbol) {

        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return getEffectiveTypeFromTypeReference(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        }
        return typeSymbol;
    }

    private static TypeSymbol getEffectiveTypeFromNilableSingletonType(TypeSymbol typeSymbol,
                                                                       Map<String, TypeSymbol> typeSymbols) {

        if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            typeSymbol = getEffectiveTypeFromReadonlyIntersection((IntersectionTypeSymbol) typeSymbol);
        }
        TypeDescKind typeDescKind = typeSymbol.typeKind();
        if (typeDescKind == TypeDescKind.UNION) {
            List<TypeSymbol> symbolList = ((UnionTypeSymbol) typeSymbol).userSpecifiedMemberTypes();
            int size = symbolList.size();
            if (size > 2) {
                return null;
            }
            if (isNilableType(typeSymbols, symbolList.get(0))) {
                typeSymbol = symbolList.get(1);
            } else if (isNilableType(typeSymbols, symbolList.get(1))) {
                typeSymbol = symbolList.get(0);
            } else {
                return null;
            }
        }
        return typeSymbol;
    }

    private static boolean isValidBasicParamType(TypeSymbol typeSymbol, Map<String, TypeSymbol> typeSymbols) {

        return subtypeOf(typeSymbols, typeSymbol, STRING) ||
                subtypeOf(typeSymbols, typeSymbol, INT) ||
                subtypeOf(typeSymbols, typeSymbol, FLOAT) ||
                subtypeOf(typeSymbols, typeSymbol, DECIMAL) ||
                subtypeOf(typeSymbols, typeSymbol, BOOLEAN) ||
                subtypeOf(typeSymbols, typeSymbol, STRING_ARRAY) ||
                subtypeOf(typeSymbols, typeSymbol, INT_ARRAY) ||
                subtypeOf(typeSymbols, typeSymbol, FLOAT_ARRAY) ||
                subtypeOf(typeSymbols, typeSymbol, DECIMAL_ARRAY) ||
                subtypeOf(typeSymbols, typeSymbol, BOOLEAN_ARRAY);
    }

    public static boolean isNilableType(Map<String, TypeSymbol> typeSymbols, TypeSymbol typeSymbol) {

        return subtypeOf(typeSymbols, typeSymbol, NIL);
    }

    public static boolean isStructuredType(Map<String, TypeSymbol> typeSymbols, TypeSymbol typeSymbol) {
        // Special cased byte[]
        if (subtypeOf(typeSymbols, typeSymbol, BYTE_ARRAY)) {
            return true;
        }

        // If the type is a basic type or basic array type, then it is not considered as a structured type
        if (isValidBasicParamType(typeSymbol, typeSymbols)) {
            return false;
        }

        return subtypeOf(typeSymbols, typeSymbol, MAP_OF_ANYDATA) ||
                subtypeOf(typeSymbols, typeSymbol, TABLE_OF_ANYDATA_MAP) ||
                subtypeOf(typeSymbols, typeSymbol, TUPLE_OF_ANYDATA) ||
                subtypeOf(typeSymbols, typeSymbol, STRUCTURED_ARRAY) ||
                subtypeOf(typeSymbols, typeSymbol, XML);
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


