/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Responsible for generating intermediate handler functions for each azure function.
 *
 * @since 2.0.0
 */
public class FunctionHandlerGenerator {

    private final SemanticModel semanticModel;
    private final Map<String, TypeDefinitionNode> typeDefinitions;

    public FunctionHandlerGenerator(SemanticModel semanticModel,
                                    Map<String, TypeDefinitionNode> generatedTypeDefinitions) {
        this.semanticModel = semanticModel;
        this.typeDefinitions = generatedTypeDefinitions;
    }

    public FunctionDeploymentContext generateHandlerFunction(FunctionDefinitionNode sourceFunc)
            throws AzureFunctionsException {
        FunctionDeploymentContext ctx = this.createFuncDeplContext(sourceFunc);
        List<PositionalArgumentNode> positionalArgumentNodes = new ArrayList<>();
        for (ParameterHandler ph : ctx.getParameterHandlers()) {
            positionalArgumentNodes.add(NodeFactory.createPositionalArgumentNode(ph.invocationProcess()));
        }
        TypeDescriptorNode originalFunctionTypeDesc =
                STUtil.getCheckedReturnTypeDescOfOriginalFunction(ctx.getSourceFunction());

        boolean isCheckRequired = STUtil.isCheckingRequiredForOriginalFunction(ctx.getSourceFunction());
        String returnName = STUtil.addFunctionCallStatement(originalFunctionTypeDesc, ctx,
                STUtil.createFunctionInvocationNode(sourceFunc.functionName().text(),
                        positionalArgumentNodes.toArray(new PositionalArgumentNode[0])), isCheckRequired);

        for (ParameterHandler ph : ctx.getParameterHandlers()) {
            ph.postInvocationProcess();
        }
        ReturnHandler returnHandler = ctx.getReturnHandler();
        if (returnHandler != null) {
            returnHandler.postInvocationProcess(STUtil.createVariableRef(returnName));
        }
        return ctx;
    }

    private FunctionDeploymentContext createFuncDeplContext(FunctionDefinitionNode sourceFunc)
            throws AzureFunctionsException {
        FunctionDeploymentContext ctx = new FunctionDeploymentContext();
        ctx.setSourceFunction(sourceFunc);
        ctx.setFunction(STUtil.createHandlerFunction(sourceFunc.functionName().text()));
        
        for (ParameterNode parameter : sourceFunc.functionSignature().parameters()) {
            ctx.getParameterHandlers()
                    .add(HandlerFactory.createParameterHandler(parameter, semanticModel, typeDefinitions));
        }

        for (ParameterHandler ph : ctx.getParameterHandlers()) {
            ph.init(ctx);
        }
        FunctionTypeSymbol functionTypeSymbol =
                ((FunctionSymbol) semanticModel.symbol(sourceFunc.functionName()).orElseThrow()).typeDescriptor();

        Optional<TypeSymbol> returnSymbolOptional = functionTypeSymbol.returnTypeDescriptor();
        if (returnSymbolOptional.isPresent() && !(returnSymbolOptional.get().typeKind() == TypeDescKind.NIL)) {
            ctx.setReturnHandler(HandlerFactory.createReturnHandler(returnSymbolOptional.get(),
                    sourceFunc.functionSignature().returnTypeDesc().orElseThrow()));
            if (ctx.getReturnHandler() != null) {
                ctx.getReturnHandler().init(ctx);
            }
        }
        return ctx;
    }
}
