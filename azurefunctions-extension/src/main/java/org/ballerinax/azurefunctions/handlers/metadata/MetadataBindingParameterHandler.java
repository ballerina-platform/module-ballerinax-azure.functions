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
package org.ballerinax.azurefunctions.handlers.metadata;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.AzureFunctionsException;
import org.ballerinax.azurefunctions.BindingType;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.STUtil;
import org.ballerinax.azurefunctions.handlers.AbstractParameterHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation for the input parameter handler annotation "@BindingName".
 */
public class MetadataBindingParameterHandler extends AbstractParameterHandler {

    public MetadataBindingParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param)
            throws AzureFunctionsException {
        super(variableSymbol, param, BindingType.METADATA);
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        Object name = annonMap.get("name");
        if (name != null) {
            this.name = name.toString();
        }
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.PARAMS)));
        if (STUtil.isJsonType(this.variableSymbol)) {
            PositionalArgumentNode stringArg =
                    NodeFactory.createPositionalArgumentNode(STUtil.createStringLiteral(this.name));
            return STUtil.createAfFunctionInvocationNode("getJsonFromMetadata", true, paramsArg, stringArg);
        } else if (STUtil.isStringType(this.variableSymbol)) {
            PositionalArgumentNode stringArg =
                    NodeFactory.createPositionalArgumentNode(STUtil.createStringLiteral(this.name));
            return STUtil.createAfFunctionInvocationNode("getStringFromMetadata", true, paramsArg, stringArg);
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
    public Map<String, Object> generateBinding() {
        return null;
    }

}
