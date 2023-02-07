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

import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.tools.diagnostics.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Visitor for extracting Azure functions from a ballerina document.
 *
 * @since 2.0.0
 */
public class AzureFunctionDocumentVisitor extends NodeVisitor {
    
    private List<Location> locations = new ArrayList<>();

    @Override
    public void visit(ImportDeclarationNode importDeclarationNode) {
        Optional<ImportOrgNameNode> importOrgNameNode = importDeclarationNode.orgName();
        if (importOrgNameNode.isEmpty()) {
            return;
        }
        if (!Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(importOrgNameNode.get().orgName().text())) {
            return;
        }
        SeparatedNodeList<IdentifierToken> identifierTokens = importDeclarationNode.moduleName();
        if (identifierTokens.size() != 1) {
            return;
        }
        if (!Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(identifierTokens.get(0).text())) {
            return;
        }
        locations.add(importDeclarationNode.location());
    }

    public List<Location> getLocations() {
        return locations;
    }
}
