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
package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.FunctionContext;
import org.ballerinax.azurefunctions.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a queue trigger binding in function.json.
 *
 * @since 2.0.0
 */
public abstract class RemoteTriggerBinding extends TriggerBinding {
    private String methodName;
    private String annotationName;
    protected Map<String, Node> types;

    public RemoteTriggerBinding(String triggerType, String methodName,
                                String annotationName, ServiceDeclarationNode serviceDeclarationNode,
                                SemanticModel semanticModel, Map<String, Node> types) {
        super(triggerType);
        this.serviceDeclarationNode = serviceDeclarationNode;
        this.semanticModel = semanticModel;
        this.methodName = methodName;
        this.annotationName = annotationName;
        this.types = types;
    }

    @Override
    public List<FunctionContext> getBindings() {
        Optional<AnnotationNode> queueTrigger = getListenerAnnotation(this.serviceDeclarationNode, this.annotationName);
        List<FunctionContext> functionContexts = new ArrayList<>();
        if (queueTrigger.isEmpty()) {
            return functionContexts;
        }
        getAnnotation(queueTrigger.get());
        String servicePath = Util.resourcePathToString(serviceDeclarationNode.absoluteResourcePath());
        NodeList<Node> members = this.serviceDeclarationNode.members();
        for (Node node : members) {
            List<Binding> bindings = new ArrayList<>();
            if (SyntaxKind.OBJECT_METHOD_DEFINITION != node.kind()) {
                continue;
            }
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String method = functionDefinitionNode.functionName().text();
            if (!method.equals(this.methodName)) {
                continue;
            }

            for (ParameterNode parameterNode : functionDefinitionNode.functionSignature().parameters()) {
                if (SyntaxKind.REQUIRED_PARAM != parameterNode.kind()) {
                    continue;
                }
                RequiredParameterNode reqParam = (RequiredParameterNode) parameterNode;
                if (reqParam.paramName().isEmpty()) {
                    continue;
                }
                String variableName = reqParam.paramName().get().text();
                if (!isAzureFunctionsAnnotationExist(reqParam.annotations())) {
                    this.setVarName(variableName);
                    continue;
                }

                InputBindingBuilder inputBuilder = new InputBindingBuilder();
                Optional<Binding> inputBinding = inputBuilder.getInputBinding(reqParam.annotations(), variableName);
                inputBinding.ifPresent(bindings::add);
            }
            bindings.add(this);
            ReturnTypeDescriptorNode returnTypeDescriptorNode =
                    functionDefinitionNode.functionSignature().returnTypeDesc().get(); //TODO recheck if return is must
            OutputBindingBuilder outputBuilder = new OutputBindingBuilder();
            List<Binding> returnBinding  = outputBuilder.getOutputBinding(returnTypeDescriptorNode, types);
            bindings.addAll(returnBinding); //TODO handle in code analyzer
            functionContexts.add(new FunctionContext(servicePath.replace("/", ""), bindings));
        }
        return functionContexts;
    }
    
    private void getAnnotation(AnnotationNode queueTrigger) {
        SeparatedNodeList<MappingFieldNode> fields = queueTrigger.annotValue().orElseThrow().fields();
        for (MappingFieldNode fieldNode : fields) {
            extractValueFromAnnotation((SpecificFieldNode) fieldNode);
        }
    }
    
     protected abstract void extractValueFromAnnotation(SpecificFieldNode fieldNode);
}
