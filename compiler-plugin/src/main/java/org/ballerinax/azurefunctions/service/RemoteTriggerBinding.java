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
import java.util.Optional;

/**
 * Represents a queue trigger binding in function.json.
 *
 * @since 2.0.0
 */
public abstract class RemoteTriggerBinding extends TriggerBinding {
    private String methodName;
    private String annotationName;

    public RemoteTriggerBinding(String triggerType, String methodName,
                                String annotationName, ServiceDeclarationNode serviceDeclarationNode,
                                SemanticModel semanticModel) {
        super(triggerType);
        this.serviceDeclarationNode = serviceDeclarationNode;
        this.semanticModel = semanticModel;
        this.methodName = methodName;
        this.annotationName = annotationName;
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
            if (node.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
                continue;
            }
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String method = functionDefinitionNode.functionName().text();
            if (!method.equals(this.methodName)) {
                continue;
            }

            for (ParameterNode parameterNode : functionDefinitionNode.functionSignature().parameters()) {
                if (parameterNode.kind() != SyntaxKind.REQUIRED_PARAM) {
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
                if (inputBinding.isPresent()) {
                    bindings.add(inputBinding.get());
                    continue;
                }

            }
            bindings.add(this);
//                    ParameterNode parameterNode = functionDefinitionNode.functionSignature().parameters().get(0);
//                    //TODO valid
//                    if (parameterNode.kind() != SyntaxKind.REQUIRED_PARAM) {
//                        continue;
//                    }
//                    RequiredParameterNode reqParam = (RequiredParameterNode) parameterNode;
//                    String paramName = reqParam.paramName().orElseThrow().text();
//                    bindings.add(new QueueTriggerBinding(paramName, queueName));

            ReturnTypeDescriptorNode returnTypeDescriptorNode =
                    functionDefinitionNode.functionSignature().returnTypeDesc().get(); //TODO recheck if return is must
            OutputBindingBuilder outputBuilder = new OutputBindingBuilder();
            Optional<Binding> returnBinding  = outputBuilder.getOutputBinding(returnTypeDescriptorNode.annotations());
            bindings.add(returnBinding.orElseThrow()); //TODO handle in code analyzer
            functionContexts.add(new FunctionContext(servicePath.replace("/", ""), bindings)); //TODO remove /
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
