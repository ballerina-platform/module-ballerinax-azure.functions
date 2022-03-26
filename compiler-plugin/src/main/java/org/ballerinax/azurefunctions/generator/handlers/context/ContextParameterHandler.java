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
package org.ballerinax.azurefunctions.generator.handlers.context;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.generator.AzureFunctionsException;
import org.ballerinax.azurefunctions.generator.BindingType;
import org.ballerinax.azurefunctions.generator.GeneratorUtil;
import org.ballerinax.azurefunctions.generator.handlers.AbstractParameterHandler;

import java.util.Map;

/**
 * Implementation for the input parameter handler for the Context object.
 */
public class ContextParameterHandler extends AbstractParameterHandler {

    public ContextParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param) {
        super(variableSymbol, param, BindingType.CONTEXT);
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        PositionalArgumentNode params = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("params")));
        PositionalArgumentNode trueArg = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createBasicLiteralNode(SyntaxKind.BOOLEAN_LITERAL,
                        NodeFactory.createToken(SyntaxKind.TRUE_KEYWORD)));

        return GeneratorUtil.createAfFunctionInvocationNode("createContext", true, params, trueArg);
    }

    @Override
    public void postInvocationProcess() {
    }

    @Override
    public Map<String, Object> generateBinding() {
        return null;
    }

}
