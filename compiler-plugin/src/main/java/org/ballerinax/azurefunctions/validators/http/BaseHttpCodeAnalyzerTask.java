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
package org.ballerinax.azurefunctions.validators.http;

import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.Constants;

import java.util.List;
import java.util.Optional;

import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_MODULE_NAME;
import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_PACKAGE_ORG;

/***
 * Code analyzer for azure function specific validations.
 *
 * @since 2.0.0
 */
public abstract class BaseHttpCodeAnalyzerTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    protected boolean isHttpListener(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        List<Diagnostic> diagnostics = syntaxNodeAnalysisContext.semanticModel().diagnostics();
        boolean erroneousCompilation = diagnostics.stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        if (erroneousCompilation) {
            return false;
        }

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        Optional<Symbol> serviceSymOptional = syntaxNodeAnalysisContext.semanticModel().symbol(serviceDeclarationNode);

        if (serviceSymOptional.isEmpty()) {
            return false;
        }
        List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceSymOptional.get()).listenerTypes();
        if (listenerTypes.stream().noneMatch(this::isListenerBelongsToAzureFuncModule)) {
            return false;
        }
        return listenerTypes.stream().anyMatch(this::isHTTPListener);
    }

    private boolean isHTTPListener(TypeSymbol listenerType) {
        if (listenerType.nameEquals(Constants.AZURE_HTTP_LISTENER)) {
            return true;
        }
        if (listenerType.typeKind() != TypeDescKind.UNION) {
            return false;
        }
        List<TypeSymbol> typeSymbols = ((UnionTypeSymbol) listenerType).memberTypeDescriptors();
        for (TypeSymbol typeSymbol : typeSymbols) {
            if (!typeSymbol.nameEquals(Constants.AZURE_HTTP_LISTENER)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isListenerBelongsToAzureFuncModule(TypeSymbol listenerType) {
        if (TypeDescKind.UNION == listenerType.typeKind()) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isAzureFuncModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (TypeDescKind.TYPE_REFERENCE == listenerType.typeKind()) {
            return isAzureFuncModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private boolean isAzureFuncModule(ModuleSymbol moduleSymbol) {
        return AZURE_FUNCTIONS_MODULE_NAME.equals(moduleSymbol.getName().get()) &&
                AZURE_FUNCTIONS_PACKAGE_ORG.equals(moduleSymbol.id().orgName());
    }

}
