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
package org.ballerinax.azurefunctions.handlers.http;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.STUtil;
import org.ballerinax.azurefunctions.handlers.AbstractParameterHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation for the input parameter handler annotation "@HTTPTrigger".
 */
public class HTTPTriggerParameterHandler extends AbstractParameterHandler {

    public HTTPTriggerParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param) {
        super(variableSymbol, param, BindingType.TRIGGER);
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.PARAMS)));
        if (STUtil.isAzurePkgType(this.variableSymbol, "HTTPRequest")) {
            PositionalArgumentNode stringArg =
                    NodeFactory.createPositionalArgumentNode(STUtil.createStringLiteral(this.name));
            return STUtil.createAfFunctionInvocationNode("getHTTPRequestFromInputData", true, paramsArg, stringArg);
        } else if (STUtil.isStringType(this.variableSymbol)) {
            PositionalArgumentNode stringArg = NodeFactory.createPositionalArgumentNode(NodeFactory
                    .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                            .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + this.name + "\"",
                                    NodeFactory.createEmptyMinutiaeList(), NodeFactory.createEmptyMinutiaeList())));
            return STUtil.createAfFunctionInvocationNode("getBodyFromHTTPInputData", true, paramsArg, stringArg);
        } else {
            throw new AzureFunctionsException(STUtil.getAFDiagnostic(this.param.typeName().location(), "AZ0008",
                    "unsupported.param.type", DiagnosticSeverity.ERROR,
                    "type '" + this.param.typeName().toString() + "'" +
                            " is not supported"));
        }
    }

    @Override
    public void postInvocationProcess() {
    }

    @Override
    public Map<String, Object> generateBinding() throws AzureFunctionsException {
        Map<String, Object> binding = new LinkedHashMap<>();
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        binding.put("type", "httpTrigger");
        binding.put("authLevel", annonMap.get("authLevel"));
        binding.put("route", annonMap.get("route"));
        binding.put("methods", new String[]{ "get", "post", "put", "delete" });
        return binding;
    }

}
