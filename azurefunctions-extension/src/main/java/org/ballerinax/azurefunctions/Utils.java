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

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.wso2.ballerinalang.compiler.desugar.ASTBuilderUtil;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNilType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Utility functions for Azure Functions.
 */
public class Utils {

    public static BLangFunction extractMainFunction(BLangPackage myPkg) {
        for (BLangFunction func : myPkg.getFunctions()) {
            if (Costants.MAIN_FUNC_NAME.equals(func.getName().value)) {
                return func;
            }
        }
        return null;
    }
    
    public static  BPackageSymbol extractAzureFuncsPackageSymbol(BLangPackage myPkg) {
        for (BLangImportPackage pi : myPkg.imports) {
            if (Costants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(pi.orgName.value)
                    && pi.pkgNameComps.size() == 1
                    && Costants.AZURE_FUNCTIONS_PACKAGE_NAME.equals(pi.pkgNameComps.get(0).value)) {
                return pi.symbol;
            }
        }
        return null;
    }
    
    public static void addRegisterCall(SymbolTable symTable, DiagnosticPos pos, BPackageSymbol pkgSymbol, 
                                       BLangBlockFunctionBody blockStmt, String name, BLangFunction func) {
        List<BLangExpression> exprs = new ArrayList<>();
        exprs.add(createStringLiteral(symTable, pos, name));
        exprs.add(createVariableRef(pos, func.symbol));
        BLangInvocation inv = createInvocationNode(pkgSymbol,
                Costants.AZURE_FUNCS_REG_FUNCTION_NAME, exprs);
        BLangExpressionStmt stmt = new BLangExpressionStmt(inv);
        stmt.pos = pos;
        blockStmt.addStatement(stmt);
    }
    
    public static BLangLiteral createStringLiteral(SymbolTable symTable, DiagnosticPos pos, String value) {
        BLangLiteral stringLit = new BLangLiteral();
        stringLit.pos = pos;
        stringLit.value = value;
        stringLit.type = symTable.stringType;
        return stringLit;
    }
    
    public static BLangSimpleVarRef createVariableRef(DiagnosticPos pos, BSymbol varSymbol) {
        final BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pos = pos;
        varRef.variableName = ASTBuilderUtil.createIdentifier(pos, varSymbol.name.value);
        varRef.symbol = varSymbol;
        varRef.type = varSymbol.type;
        return varRef;
    }
        
    public static BLangInvocation createInvocationNode(BPackageSymbol pkgSymbol, String functionName,
            List<BLangExpression> args) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(functionName);
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        invocationNode.symbol = pkgSymbol.scope.lookup(new Name(functionName)).symbol;
        invocationNode.type = new BNilType();
        invocationNode.requiredArgs = args;
        return invocationNode;
    }
    
    public static BLangFunction createFunction(DiagnosticPos pos, String name, BLangPackage packageNode) {
        final BLangFunction bLangFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        final IdentifierNode funcName = ASTBuilderUtil.createIdentifier(pos, name);
        bLangFunction.setName(funcName);
        bLangFunction.flagSet = EnumSet.of(Flag.PUBLIC);
        bLangFunction.pos = pos;
        bLangFunction.type = new BInvokableType(new ArrayList<>(), new BNilType(), null);
        bLangFunction.body = createBlockStmt(pos);
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(bLangFunction.flagSet),
                new Name(bLangFunction.name.value), packageNode.packageID, 
                bLangFunction.type, packageNode.symbol, true);
        functionSymbol.scope = new Scope(functionSymbol);
        bLangFunction.symbol = functionSymbol;
        return bLangFunction;
    }
    
    public static BLangFunctionBody createBlockStmt(DiagnosticPos pos) {
        final BLangFunctionBody blockNode = (BLangFunctionBody) TreeBuilder.createBlockFunctionBodyNode();
        blockNode.pos = pos;
        return blockNode;
    }

    public static boolean hasAzureFunctionsAnnotation(AnnotationAttachmentNode attachmentNode) {
        BAnnotationSymbol symbol = ((BLangAnnotationAttachment) attachmentNode).annotationSymbol;
        return Costants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(symbol.pkgID.orgName.value)
                && Costants.AZURE_FUNCTIONS_PACKAGE_NAME.equals(symbol.pkgID.name.value)
                && "Function".equals(symbol.name.value);
    }

    public static boolean isAzureFunction(BLangFunction fn, DiagnosticLog dlog) {
        List<BLangAnnotationAttachment> annotations = fn.annAttachments;
        boolean hasAzureFuncsAnnon = false;
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            hasAzureFuncsAnnon = Utils.hasAzureFunctionsAnnotation(attachmentNode);
            if (hasAzureFuncsAnnon) {
                break;
            }
        }
        if (hasAzureFuncsAnnon) {
            BLangFunction bfn = (BLangFunction) fn;
            if (!validateAzureFunction(bfn)) {
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, fn.getPosition(), 
                        "Invalid function signature for an Azure Functions function: " + 
                        bfn + ", it should be 'public function (json) returns json|error'");
                return false;
            } else {
                return true;
            }
        } else {        
            return false;
        }
    }

    private static boolean validateAzureFunction(BLangFunction node) {
        return true;
    }
    
}
