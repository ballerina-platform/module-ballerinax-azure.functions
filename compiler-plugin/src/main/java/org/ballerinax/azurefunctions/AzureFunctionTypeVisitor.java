/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor for extracting types defined in the document.
 *
 * @since 2.0.0
 */
public class AzureFunctionTypeVisitor extends NodeVisitor {
    
    private Map<String, Node> types;
    public AzureFunctionTypeVisitor() {
        this.types = new HashMap<>();
    }
    
    @Override
    public void visit(TypeDefinitionNode typeDefinitionNode) {
        String variableName = typeDefinitionNode.typeName().text();
        this.types.put(variableName, typeDefinitionNode.typeDescriptor());
    }

    public Map<String, Node> getTypes() {
        return types;
    }
}
