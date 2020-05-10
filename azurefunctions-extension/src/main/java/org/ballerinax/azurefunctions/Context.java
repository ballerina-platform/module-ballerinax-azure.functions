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
package org.ballerinax.azurefunctions;

import com.google.gson.JsonElement;
import org.ballerinalang.model.VariableDef;
import org.ballerinalang.model.tree.statements.BlockStatementNode;
import org.wso2.ballerinalang.compiler.PackageCache;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.util.List;

/**
 * Represents the Azure functions context.
 */
public class Context {

    private VariableDef requestVar;

    private VariableDef resultVar;

    private BlockStatementNode functionBlockStmt;

    private List<ParameterHandler> parameterHandlers;

    private ReturnHandler returnHandler;

    private JsonElement functionDefinition;
    
    private SymbolTable symTable;
    
    private PackageCache pkgCache;
    
    public void init(CompilerContext cctx) {
        this.symTable = SymbolTable.getInstance(cctx);
        this.pkgCache = PackageCache.getInstance(cctx);
    }  

    public SymbolTable getSymTable() {
        return symTable;
    }

    public PackageCache getPkgCache() {
        return pkgCache;
    }

    public VariableDef getRequestVar() {
        return requestVar;
    }

    public void setRequestVar(VariableDef requestVar) {
        this.requestVar = requestVar;
    }

    public VariableDef getResultVar() {
        return resultVar;
    }

    public void setResultVar(VariableDef resultVar) {
        this.resultVar = resultVar;
    }

    public BlockStatementNode getFunctionBlockStmt() {
        return functionBlockStmt;
    }

    public void setFunctionBlockStmt(BlockStatementNode functionBlockStmt) {
        this.functionBlockStmt = functionBlockStmt;
    }

    public List<ParameterHandler> getParameterHandlers() {
        return parameterHandlers;
    }

    public void setParameterHandlers(List<ParameterHandler> parameterHandlers) {
        this.parameterHandlers = parameterHandlers;
    }

    public ReturnHandler getReturnHandler() {
        return returnHandler;
    }

    public void setReturnHandler(ReturnHandler returnHandler) {
        this.returnHandler = returnHandler;
    }

    public JsonElement getFunctionDefinition() {
        return functionDefinition;
    }

    public void setFunctionDefinition(JsonElement functionDefinition) {
        this.functionDefinition = functionDefinition;
    }
    
}
