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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import org.ballerinax.azurefunctions.service.ServiceHandler;
import org.ballerinax.azurefunctions.service.TriggerBinding;

import java.util.ArrayList;
import java.util.List;
/**
 * Visitor for extracting Azure functions from a ballerina document.
 *
 * @since 2.0.0
 */
public class AzureFunctionServiceVisitor extends NodeVisitor {

    //    private String moduleName;
    private List<FunctionContext> functionContexts;
    private SemanticModel semanticModel;

    public AzureFunctionServiceVisitor(SemanticModel semanticModel) {
        this.functionContexts = new ArrayList<>();
        this.semanticModel = semanticModel;
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        super.visit(modulePartNode);
    }

//    @Override
//    public void visit(ImportDeclarationNode importDeclarationNode) {
//        if (importDeclarationNode.orgName().isEmpty()) {
//            return;
//        }
//        String orgName = importDeclarationNode.orgName().get().orgName().text();
//        if (!Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(orgName)) {
//            return;
//        }
//        if (importDeclarationNode.moduleName().size() != 1) {
//            return;
//        }
//        String moduleName = importDeclarationNode.moduleName().get(0).text();
//        if (Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(moduleName)) {
//            this.moduleName = moduleName;
//        }
//        if (importDeclarationNode.prefix().isEmpty()) {
//            return;
//        }
//        this.moduleName = importDeclarationNode.prefix().get().prefix().text();
//    }

    @Override
    public void visit(ServiceDeclarationNode serviceDeclarationNode) {
        TriggerBinding builder = ServiceHandler.getBuilder(serviceDeclarationNode, semanticModel);
        List<FunctionContext> contexts = builder.getBindings();
        functionContexts.addAll(contexts);
    }

    public List<FunctionContext> getFunctionContexts() {
        return functionContexts;
    }
}
