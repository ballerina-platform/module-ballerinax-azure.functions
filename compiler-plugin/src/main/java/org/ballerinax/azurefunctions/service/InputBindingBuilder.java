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
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.service.blob.BlobInputBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBInputBinding;

import java.util.Optional;

/**
 * Represents an Input Binding builder for Azure Function services.
 *
 * @since 2.0.0
 */

public class InputBindingBuilder {
    
    public Optional<Binding> getInputBinding(NodeList<AnnotationNode> annotations, String varName) {
        for (AnnotationNode annotation : annotations) {
            Node annotRef = annotation.annotReference();
            if (SyntaxKind.QUALIFIED_NAME_REFERENCE == annotRef.kind()) {
                QualifiedNameReferenceNode annotationRef = (QualifiedNameReferenceNode) annotRef;
                String annotationText = annotationRef.identifier().text();
                if (Constants.COSMOS_INPUT_BINDING.equals(annotationText)) { 
                    return Optional.of(new CosmosDBInputBinding(annotation, varName));
                }
                if (Constants.BLOB_INPUT_BINDING.equals(annotationText)) {
                    return Optional.of(new BlobInputBinding(annotation, varName));
                }
            }
        }
        return Optional.empty();
    }
}
