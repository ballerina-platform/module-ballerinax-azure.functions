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
package org.ballerinax.azurefunctions.handlers.timer;

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation for the input parameter handler annotation "@TimerTrigger".
 */
public class TimerTriggerHandler extends AbstractParameterHandler {

    public TimerTriggerHandler(ParameterSymbol variableSymbol, RequiredParameterNode param) {
        super(variableSymbol, param, BindingType.TRIGGER);
    }

    @Override
    public ExpressionNode invocationProcess() throws AzureFunctionsException {
        if (STUtil.isJsonType(this.variableSymbol)) {
            PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                    NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("params")));
            PositionalArgumentNode stringArg =
                    NodeFactory.createPositionalArgumentNode(STUtil.createStringLiteral(this.name));
            return STUtil.createAfFunctionInvocationNode("getJsonFromInputData", true, paramsArg, stringArg);
        } else {
            throw new AzureFunctionsException(STUtil.getAFDiagnostic(this.param.typeName().location(), "AZ0008",
                    "unsupported.param.type", DiagnosticSeverity.ERROR,
                    "type '" + this.param.typeName().toString() + "'" +
                            " is not supported"));
        }
    }

    @Override
    public void postInvocationProcess() throws AzureFunctionsException {
    }

    @Override
    public Map<String, Object> generateBinding() throws AzureFunctionsException {
        Map<String, Object> binding = new LinkedHashMap<>();
        Optional<AnnotationNode> annotationNode = STUtil.extractAzureFunctionAnnotation(param.annotations());
        Map<String, Object> annonMap = STUtil.extractAnnotationKeyValues(annotationNode.orElseThrow());
        binding.put("type", "timerTrigger");
        binding.put("schedule", annonMap.get("schedule"));
        Boolean runOnStartup = (Boolean) annonMap.get("runOnStartup");
        if (runOnStartup == null) {
            runOnStartup = Constants.DEFAULT_TIMER_TRIGGER_RUNONSTARTUP;
        }
        binding.put("runOnStartup", runOnStartup);
        return binding;
    }

}
