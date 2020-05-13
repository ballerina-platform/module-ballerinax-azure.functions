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
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.wso2.ballerinalang.compiler.desugar.ASTBuilderUtil;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNilType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Utility functions for Azure Functions.
 */
public class Utils {

    public static BLangFunction extractMainFunction(BLangPackage myPkg) {
        for (BLangFunction func : myPkg.getFunctions()) {
            if (Constants.MAIN_FUNC_NAME.equals(func.getName().value)) {
                return func;
            }
        }
        return null;
    }
    
    public static BPackageSymbol extractAzureFuncsPackageSymbol(BLangPackage myPkg) {
        for (BLangImportPackage pi : myPkg.imports) {
            if (Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(pi.orgName.value)
                    && pi.pkgNameComps.size() == 1
                    && Constants.AZURE_FUNCTIONS_PACKAGE_NAME.equals(pi.pkgNameComps.get(0).value)) {
                return pi.symbol;
            }
        }
        return null;
    }
    
    public static void addRegisterCall(GlobalContext ctx, DiagnosticPos pos, BPackageSymbol pkgSymbol, 
                                       BLangBlockFunctionBody blockStmt, String name, BLangFunction func) {
        List<BLangExpression> exprs = new ArrayList<>();
        exprs.add(createStringLiteral(ctx.getSymTable(), pos, name));
        exprs.add(createVariableRef(pos, func.symbol));
        BLangInvocation inv = createInvocationNode(pkgSymbol, Constants.AZURE_FUNCS_REG_FUNCTION_NAME, exprs);
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

    public static BLangExpression createEmptyJsonObjectLiteral(SymbolTable symTable, DiagnosticPos pos) {
        BLangRecordLiteral jsonLit = new BLangRecordLiteral();
        jsonLit.type = symTable.mapJsonType;
        return jsonLit;
    }
    
    public static BLangSimpleVarRef createVariableRef(DiagnosticPos pos, BSymbol varSymbol) {
        BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pos = pos;
        varRef.variableName = ASTBuilderUtil.createIdentifier(pos, varSymbol.name.value);
        varRef.symbol = varSymbol;
        varRef.type = varSymbol.type;
        return varRef;
    }
    
    public static BLangSimpleVariable createVariable(DiagnosticPos pos, BType type, String name, BSymbol owner) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        var.pos = pos;
        var.name = ASTBuilderUtil.createIdentifier(pos, name);
        var.type = type;
        var.symbol = new BVarSymbol(0, new Name(name), type.tsymbol.pkgID, type, owner);
        return var;
    }

    public static BLangSimpleVariable addJSONVarDef(DiagnosticPos pos, GlobalContext ctx, 
            String name, BSymbol owner, BLangBlockFunctionBody body) {
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = ctx.getSymTable().jsonType;
        varDef.var = createVariable(pos, varDef.type, name, owner);
        varDef.var.expr = createEmptyJsonObjectLiteral(ctx.getSymTable(), pos);
        varDef.pos = pos;
        body.addStatement(varDef);
        return varDef.var;
    }
    
    public static BLangType createJsonTypeNode(DiagnosticPos pos, GlobalContext ctx) {
        BLangType nillType = new BLangValueType(TypeKind.JSON);
        nillType.type = ctx.getSymTable().jsonType;
        return nillType;
    }

    public static BLangType createNillTypeNode(DiagnosticPos pos, GlobalContext ctx) {
        BLangType nillType = new BLangValueType(TypeKind.NIL);
        nillType.type = ctx.getSymTable().nilType;
        return nillType;
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

    private static String generateHandlerFuncName(String baseName) {
        return baseName + "_" + Constants.GEN_FUNC_SUFFIX;
    }

    public static BType extractHTTPRequestType(GlobalContext ctx) {
        PackageID pkgId = new PackageID(new Name(Constants.BALLERINA_ORG), new Name(Constants.HTTP_MODULE_NAME), 
                new Name(Constants.HTTP_MODULE_VERSION));
        return ctx.getPkgCache().getSymbol(pkgId).getType().tsymbol.scope
                .lookup(new Name(Constants.HTTP_REQUEST_TYPE_NAME)).symbol.type;
    }

    public static BLangFunction createHandlerFunction(GlobalContext ctx, DiagnosticPos pos, 
            String baseName, BLangPackage packageNode) {
        List<String> paramNames = Arrays.asList(Constants.HTTP_REQUEST_PARAM_NAME);
        List<BType> paramTypes = Arrays.asList(extractHTTPRequestType(ctx));
        BLangType retType = createJsonTypeNode(pos, ctx);
        BLangFunction handlerFunc = createFunction(pos, generateHandlerFuncName(baseName), paramNames, paramTypes,
                retType, packageNode);
        return handlerFunc;
    }

    public static void addReturnStatement(GlobalContext ctx, DiagnosticPos pos, BSymbol var,
            BLangBlockFunctionBody body) {
        BLangReturn ret = new BLangReturn();
        ret.pos = pos;
        ret.type = var.type;
        ret.expr = createVariableRef(pos, var);
        body.addStatement(ret);
    }

    public static BLangFunction createFunction(GlobalContext ctx, DiagnosticPos pos, String name,
            BLangPackage packageNode) {
        return createFunction(pos, name, new ArrayList<>(), new ArrayList<>(), createNillTypeNode(pos, ctx),
                packageNode);
    }

    public static BLangFunction createFunction(DiagnosticPos pos, String name, List<String> paramNames, 
            List<BType> paramTypes, BLangType retType, BLangPackage packageNode) {
        final BLangFunction bLangFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        final IdentifierNode funcName = ASTBuilderUtil.createIdentifier(pos, name);
        bLangFunction.setName(funcName);
        bLangFunction.flagSet = EnumSet.of(Flag.PUBLIC);
        bLangFunction.pos = pos;
        bLangFunction.type = new BInvokableType(paramTypes, retType.type, null);
        bLangFunction.body = createBlockStmt(pos);
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(bLangFunction.flagSet),
                new Name(bLangFunction.name.value), packageNode.packageID, 
                bLangFunction.type, packageNode.symbol, true);
        functionSymbol.type = bLangFunction.type;
        functionSymbol.retType = retType.type;
        functionSymbol.scope = new Scope(functionSymbol);
        bLangFunction.symbol = functionSymbol;
        for (int i = 0; i < paramNames.size(); i++) {
            bLangFunction.addParameter(createVariable(pos, paramTypes.get(i), paramNames.get(i), 
                    bLangFunction.symbol));
        }
        bLangFunction.setReturnTypeNode(retType);
        return bLangFunction;
    }
    
    public static BLangFunctionBody createBlockStmt(DiagnosticPos pos) {
        final BLangFunctionBody blockNode = (BLangFunctionBody) TreeBuilder.createBlockFunctionBodyNode();
        blockNode.pos = pos;
        return blockNode;
    }

    public static boolean hasAzureFunctionsAnnotation(AnnotationAttachmentNode attachmentNode) {
        BAnnotationSymbol symbol = ((BLangAnnotationAttachment) attachmentNode).annotationSymbol;
        return Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(symbol.pkgID.orgName.value)
                && Constants.AZURE_FUNCTIONS_PACKAGE_NAME.equals(symbol.pkgID.name.value)
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
        return hasAzureFuncsAnnon;
    }
    
}
