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

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MemberTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.service.blob.BlobOutputBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBOutputBinding;
import org.ballerinax.azurefunctions.service.http.HTTPOutputBinding;
import org.ballerinax.azurefunctions.service.queue.QueueOutputBinding;
import org.ballerinax.azurefunctions.service.twilio.TwilioSmsOutputBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an Output Binding builder for Azure Function services.
 *
 * @since 2.0.0
 */
public class OutputBindingBuilder {

    private Optional<Binding> getOutputBinding(NodeList<AnnotationNode> nodes, int index) {
        for (AnnotationNode annotationNode : nodes) {
            Node node = annotationNode.annotReference();
            if (SyntaxKind.QUALIFIED_NAME_REFERENCE != node.kind()) {
                continue;
            }
            QualifiedNameReferenceNode qualifiedNameReferenceNode = (QualifiedNameReferenceNode) node;
            String annotationName = qualifiedNameReferenceNode.identifier().text();
            switch (annotationName) {
                case Constants.QUEUE_OUTPUT_BINDING:
                    return Optional.of(new QueueOutputBinding(annotationNode, index));
                case Constants.HTTP_OUTPUT_BINDING:
                    return Optional.of(new HTTPOutputBinding(annotationNode, index));
                case Constants.COSMOS_OUTPUT_BINDING:
                    return Optional.of(new CosmosDBOutputBinding(annotationNode, index));
                case Constants.TWILIO_OUTPUT_BINDING:
                    return Optional.of(new TwilioSmsOutputBinding(annotationNode, index));
                case Constants.BLOB_OUTPUT_BINDING:
                    return Optional.of(new BlobOutputBinding(annotationNode, index));
                default:
                    throw new RuntimeException("Unexpected property in the annotation");
            }
        }
        return Optional.empty();
    }

    public List<Binding> getOutputBinding(ReturnTypeDescriptorNode returnTypeDescriptorNode, Map<String, Node> types) {
        List<Binding> outputBindings = new ArrayList<>();
        Node type = returnTypeDescriptorNode.type();
        if (type.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            type = types.get(((SimpleNameReferenceNode) type).name().text());
        }
        
        if (type.kind() == SyntaxKind.TUPLE_TYPE_DESC) {
            TupleTypeDescriptorNode tupleTypeDesc = (TupleTypeDescriptorNode) type;
            SeparatedNodeList<Node> tupleMembers = tupleTypeDesc.memberTypeDesc();
            for (int i = 0; i < tupleMembers.size(); i++) {
                Node node = tupleMembers.get(i);
                if (node.kind() == SyntaxKind.MEMBER_TYPE_DESC) {
                    MemberTypeDescriptorNode typeDescMember = (MemberTypeDescriptorNode) node;
                    Optional<Binding> outputBinding = getOutputBinding(typeDescMember.annotations(), i);
                    outputBinding.ifPresent(outputBindings::add);
                }
            }
        } else {
            Optional<Binding> outputBinding = getOutputBinding(returnTypeDescriptorNode.annotations(), 0);
            outputBinding.ifPresent(outputBindings::add);
        }

        return outputBindings;
    }
}
