/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.azurefunctions;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.plugins.CodeModifier;
import io.ballerina.projects.plugins.CodeModifierContext;
import org.ballerinax.azurefunctions.context.DocumentContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code AzureCodeModifier} handles required code-modification for Azure Function Services.
 */
public class AzureCodeModifier extends CodeModifier {

    private final Map<DocumentId, DocumentContext> payloadParamContextMap;

    public AzureCodeModifier() {
        this.payloadParamContextMap = new HashMap<>();
    }
    @Override
    public void init(CodeModifierContext codeModifierContext) {
        codeModifierContext.addSyntaxNodeAnalysisTask(
                new HttpPayloadParamIdentifier(this.payloadParamContextMap), List.of(SyntaxKind.SERVICE_DECLARATION));
        codeModifierContext.addSourceModifierTask(new FunctionUpdaterTask(this.payloadParamContextMap));
    }
}
