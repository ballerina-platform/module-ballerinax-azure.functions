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
package org.ballerinax.azurefunctions.generator.handlers.http;

import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.azurefunctions.generator.AzureFunctionsException;
import org.ballerinax.azurefunctions.generator.BindingType;
import org.ballerinax.azurefunctions.generator.Constants;
import org.ballerinax.azurefunctions.generator.GeneratorUtil;
import org.ballerinax.azurefunctions.generator.handlers.AbstractParameterHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for the output parameter handler annotation "@HTTPOutput".
 */
public class HTTPOutputParameterHandler extends AbstractParameterHandler {

    private SimpleNameReferenceNode var;

    public HTTPOutputParameterHandler(ParameterSymbol variableSymbol, RequiredParameterNode param) {
        super(variableSymbol, param, BindingType.OUTPUT);
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        if (!GeneratorUtil.isAzurePkgType(this.variableSymbol, "HTTPBinding")) {
            throw new AzureFunctionsException(GeneratorUtil.getAFDiagnostic(param.typeName().location(), "AZ0007",
                    "required.type", DiagnosticSeverity.ERROR, "type must be HTTPBinding"));
        }
        this.var = GeneratorUtil.addAzurePkgRecordVarDef(this.ctx, "HTTPBinding", this.ctx.getNextVarName());
        return this.var;
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException {
        PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                GeneratorUtil.createVariableRef(Constants.PARAMS));

        PositionalArgumentNode stringArg =
                NodeFactory.createPositionalArgumentNode(GeneratorUtil.createStringLiteral(this.name));

        PositionalArgumentNode varArg = NodeFactory.createPositionalArgumentNode(
                GeneratorUtil.createVariableRef(var.name().text()));

        GeneratorUtil.addAzurePkgFunctionCallStatement(this.ctx, "setHTTPOutput", true, paramsArg, stringArg, varArg);
    }

    @Override
    public Map<String, Object> generateBinding() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("type", "http");
        return binding;
    }

}
