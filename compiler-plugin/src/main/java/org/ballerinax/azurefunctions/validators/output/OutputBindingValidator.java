/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.azurefunctions.validators.output;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Annotatable;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleMemberSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinax.azurefunctions.AzureDiagnosticCodes;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_MODULE_NAME;
import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_PACKAGE_ORG;

/**
 * Validates the output bindings in return type descriptors.
 *
 * @since 2.0.0
 */
public class OutputBindingValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ReturnTypeDescriptorNode returnTypeDescriptorNode = (ReturnTypeDescriptorNode) syntaxNodeAnalysisContext.node();
        Location location = returnTypeDescriptorNode.type().location();
        List<Diagnostic> diagnostics = new ArrayList<>();
        NonTerminalNode functionNode = returnTypeDescriptorNode.parent().parent();
        NonTerminalNode serviceNode = functionNode.parent();
        if (serviceNode.kind() != SyntaxKind.SERVICE_DECLARATION) {
            return;
        }
        SemanticModel semanticModel = syntaxNodeAnalysisContext.semanticModel();
        if (!Util.isAzureFunctionsService(semanticModel, (ServiceDeclarationNode) serviceNode)) {
            return;
        }
        if (!Util.isAnalyzableFunction(functionNode)) {
            return;
        }
        Optional<Symbol> functionSymbol = semanticModel.symbol(functionNode);
        if (functionSymbol.isEmpty()) {
            return;
        }
        Symbol symbol = functionSymbol.get();
        SymbolKind functionKind = symbol.kind();
        FunctionSymbol methodSymbol = (FunctionSymbol) symbol;
        Optional<TypeSymbol> returnTypeDescriptor = methodSymbol.typeDescriptor().returnTypeDescriptor();
        if (returnTypeDescriptor.isEmpty()) {
            return;
        }
        TypeSymbol typeSymbol = returnTypeDescriptor.get();
        if (typeSymbol.typeKind() == TypeDescKind.TUPLE) {
            TupleTypeSymbol tupleType = (TupleTypeSymbol) typeSymbol;
            List<TupleMemberSymbol> members = tupleType.members();
            for (TupleMemberSymbol member : members) {
                List<AnnotationSymbol> annotations = member.annotations();
                TypeSymbol memberType = member.typeDescriptor();
                Optional<Diagnostic> diagnostic =
                        validateOutputBindings(annotations, memberType, functionKind, true, location);
                diagnostic.ifPresent(diagnostics::add);
            }
        } else {
            Optional<Annotatable> returnTypeAnnotations = methodSymbol.typeDescriptor().returnTypeAnnotations();
            if (returnTypeAnnotations.isEmpty()) {
                if (Util.isRemoteFunction(methodSymbol)) {
                    Diagnostic diagnostic = Util.getDiagnostic(location, AzureDiagnosticCodes.AF_014);
                    diagnostics.add(diagnostic);
                } else {
                    return;
                }
            } else {
                Optional<Diagnostic> diagnostic = validateOutputBindings(returnTypeAnnotations.get().annotations(),
                        typeSymbol, functionKind, false, location);
                diagnostic.ifPresent(diagnostics::add);
            }
        }

        for (Diagnostic diagnostic : diagnostics) {
            syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
        }
    }

    private Optional<Diagnostic> validateOutputBindings(List<AnnotationSymbol> returnAnnotations,
                                                        TypeSymbol typeSymbol, SymbolKind functionSymbolKind,
                                                        boolean isTuple, Location location) {
        Optional<Diagnostic> azureAnnotationCountValid =
                isAzureAnnotationCountValid(functionSymbolKind, returnAnnotations, typeSymbol, isTuple, location);
        if (azureAnnotationCountValid.isPresent()) {
            return azureAnnotationCountValid;
        }

        for (AnnotationSymbol annotationSymbol : returnAnnotations) {
            Optional<Diagnostic> diagnostic = validateAnnotationTypes(annotationSymbol, typeSymbol, location);
            if (diagnostic.isPresent()) {
                return diagnostic;
            }
        }
        return Optional.empty();
    }

    private Optional<Diagnostic> isAzureAnnotationCountValid(SymbolKind functionSymbolKind,
                                                             List<AnnotationSymbol> annotations,
                                                             TypeSymbol typeSymbol, boolean isTuple,
                                                             Location location) {
        List<AnnotationSymbol> azureAnnotations = getAzureFunctionsAnnotations(annotations);

        if (azureAnnotations.isEmpty()) {
            if (isTuple || (functionSymbolKind != SymbolKind.RESOURCE_METHOD)) {
                Diagnostic diagnostic = Util.getDiagnostic(location, AzureDiagnosticCodes.AF_012,
                        typeSymbol.typeKind().getName());
                return Optional.of(diagnostic);
            }
        } else {
            if (azureAnnotations.size() > 1) {
                Diagnostic diagnostic = Util.getDiagnostic(location, AzureDiagnosticCodes.AF_013,
                        typeSymbol.typeKind().getName());
                return Optional.of(diagnostic);
            }
        }
        return Optional.empty();
    }

    private List<AnnotationSymbol> getAzureFunctionsAnnotations(List<AnnotationSymbol> annotations) {
        List<AnnotationSymbol> azureAnnotations = new ArrayList<>();
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<ModuleSymbol> module = annotationSymbol.getModule();
            if (module.isEmpty()) {
                continue;
            }
            ModuleSymbol moduleSymbol = module.get();
            Optional<String> name = moduleSymbol.getName();
            if (name.isEmpty()) {
                continue;
            }
            if (AZURE_FUNCTIONS_MODULE_NAME.equals(name.get()) &&
                    AZURE_FUNCTIONS_PACKAGE_ORG.equals(moduleSymbol.id().orgName())) {
                azureAnnotations.add(annotationSymbol);
            }
        }
        return azureAnnotations;
    }

    private Optional<Diagnostic> validateAnnotationTypes(AnnotationSymbol annotationSymbol, TypeSymbol typeSymbol,
                                                         Location location) {
        Optional<String> name = annotationSymbol.getName();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        if (name.get().equals(Constants.BLOB_OUTPUT_BINDING)) {
            if (typeSymbol.typeKind() == TypeDescKind.UNION) {
                //TODO extend support for union types
                return Optional.empty();
            }
            if (typeSymbol.typeKind() != TypeDescKind.ARRAY ||
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor().typeKind() != TypeDescKind.BYTE) {
                Diagnostic diagnostic = Util.getDiagnostic(location, AzureDiagnosticCodes.AF_009,
                        Constants.BLOB_OUTPUT_BINDING);
                return Optional.of(diagnostic);
            }
        }
        return Optional.empty();
    }
}
