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

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;

import java.util.List;
import java.util.Optional;

/***
 * Code analyzer for azure function specific validations.
 *
 * @since 2.0.0
 */
public abstract class BaseHttpCodeAnalyzerTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    protected boolean isHttpListener(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        Optional<Symbol> serviceSymOptional = syntaxNodeAnalysisContext.semanticModel().symbol(serviceDeclarationNode);

        if (serviceSymOptional.isEmpty()) {
            return false;
        }
        List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceSymOptional.get()).listenerTypes();

        if (!Util.isAzureFunctionsService(syntaxNodeAnalysisContext.semanticModel(), serviceDeclarationNode)) {
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
}
