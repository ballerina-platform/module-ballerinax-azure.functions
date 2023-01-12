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

import io.ballerina.compiler.api.symbols.Annotatable;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinax.azurefunctions.AzureDiagnosticCodes;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Validates the output bindings in return type descriptors.
 *
 * @since 2.0.0
 */
public class OutputBindingValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        PrintStream out = System.out;
        ReturnTypeDescriptorNode returnTypeDescriptorNode = (ReturnTypeDescriptorNode) syntaxNodeAnalysisContext.node();
        List<Diagnostic> diagnostics = new ArrayList<>();
        //TODO validate az func svc
        Optional<Symbol> functionSymbol =
                syntaxNodeAnalysisContext.semanticModel().symbol(returnTypeDescriptorNode.parent().parent());
        if (functionSymbol.isEmpty()) {
            return;
        }
        Symbol symbol = functionSymbol.get();
        if (symbol.kind() == SymbolKind.RESOURCE_METHOD) { //TODO remote method
            MethodSymbol methodSymbol = (MethodSymbol) symbol;
            Optional<TypeSymbol> returnTypeDescriptor = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDescriptor.isEmpty()) {
                return;
            }
            TypeSymbol typeSymbol = returnTypeDescriptor.get();
            if (typeSymbol.typeKind() == TypeDescKind.TUPLE) {
                TupleTypeSymbol tupleType = (TupleTypeSymbol) typeSymbol;
                List<TypeSymbol> typeSymbols = tupleType.memberTypeDescriptors();
                for (TypeSymbol member : typeSymbols) {
                    out.println(member);
                }
            } else {
                Optional<Annotatable> returnTypeAnnotations = methodSymbol.typeDescriptor().returnTypeAnnotations();
                if (returnTypeAnnotations.isEmpty()) {
                    return; //TODO Should this be ignored?
                }
                List<AnnotationSymbol> returnAnnotations = returnTypeAnnotations.get().annotations();
                for (AnnotationSymbol annotationSymbol : returnAnnotations) {
                    diagnostics.addAll(validateAnnotationTypes(annotationSymbol, typeSymbol));
                }
            }
        }
        for (Diagnostic diagnostic : diagnostics) {
            syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
        }
    }

    private List<Diagnostic> validateAnnotationTypes(AnnotationSymbol annotationSymbol, TypeSymbol typeSymbol) {
        Optional<String> name = annotationSymbol.getName();
        if (name.isEmpty()) {
            return Collections.emptyList();
        }
        if (name.get().equals(Constants.BLOB_OUTPUT_BINDING)) {
            if (typeSymbol.typeKind() != TypeDescKind.ARRAY ||
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor().typeKind() != TypeDescKind.BYTE) {
                Diagnostic diagnostic = Util.getDiagnostic(annotationSymbol.location(), AzureDiagnosticCodes.AF_009,
                        Constants.BLOB_OUTPUT_BINDING);
                return Collections.singletonList(diagnostic);
            }
        }
        return Collections.emptyList();
    }
}
