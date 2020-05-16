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

    public static boolean isAzureFuncsPackage(PackageID pkgId) {
        return Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(pkgId.orgName.value)
                && Constants.AZURE_FUNCTIONS_PACKAGE_NAME.equals(pkgId.name.value);
    }
    
    public static BPackageSymbol extractAzureFuncsPackageSymbol(BLangPackage myPkg) {
        for (BLangImportPackage pi : myPkg.imports) {
            if (isAzureFuncsPackage(pi.symbol.pkgID)) {
                return pi.symbol;
            }
        }
        return null;
    }
    
    public static void addRegisterCall(GlobalContext ctx, DiagnosticPos pos, BPackageSymbol pkgSymbol, 
                                       BLangBlockFunctionBody blockStmt, String name, BLangFunction func) {
        BLangInvocation inv = createInvocationNode(pkgSymbol, Constants.AZURE_FUNCS_REG_FUNCTION_NAME,
                createStringLiteral(ctx, name), createVariableRef(ctx, func.symbol));
        BLangExpressionStmt stmt = new BLangExpressionStmt(inv);
        stmt.pos = pos;
        blockStmt.addStatement(stmt);
    }

    public static void addAzurePkgFunctionCall(FunctionDeploymentContext ctx, String name, BLangExpression... exprs) {
        addFunctionCall(ctx, createAzurePkgInvocationNode(ctx, name, exprs));
    }

    public static void addFunctionCall(FunctionDeploymentContext ctx, BSymbol funcSymbol, BLangExpression... exprs) {
        addFunctionCall(ctx, createInvocationNode(funcSymbol, exprs));
    }

    public static void addFunctionCall(FunctionDeploymentContext ctx, BPackageSymbol pkgSymbol, String name, 
            BLangExpression... exprs) {
        addFunctionCall(ctx, createInvocationNode(pkgSymbol, name, exprs));
    }

    public static void addFunctionCall(FunctionDeploymentContext ctx, BLangInvocation inv) {
        BLangExpressionStmt stmt = new BLangExpressionStmt(inv);
        stmt.pos = ctx.globalCtx.pos;
        ((BLangBlockFunctionBody) ctx.function.body).addStatement(stmt);
    }
    
    public static BLangLiteral createStringLiteral(GlobalContext ctx, String value) {
        BLangLiteral stringLit = new BLangLiteral();
        stringLit.pos = ctx.pos;
        stringLit.value = value;
        stringLit.type = ctx.symTable.stringType;
        return stringLit;
    }

    public static BLangExpression createEmptyRecordLiteral(BType type) {
        BLangRecordLiteral recordLit = new BLangRecordLiteral();
        recordLit.type = type;
        return recordLit;
    }
    
    public static BLangSimpleVarRef createVariableRef(GlobalContext ctx, BVarSymbol varSymbol) {
        BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pos = ctx.pos;
        varRef.variableName = ASTBuilderUtil.createIdentifier(ctx.pos, varSymbol.name.value);
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

    public static BLangSimpleVariable addJSONVarDef(FunctionDeploymentContext ctx, String name, BSymbol owner, 
            BLangBlockFunctionBody body) {
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = ctx.globalCtx.symTable.jsonType;
        varDef.var = createVariable(ctx.globalCtx.pos, varDef.type, name, owner);
        varDef.var.expr = createEmptyRecordLiteral(ctx.globalCtx.symTable.mapJsonType);
        varDef.pos = ctx.globalCtx.pos;
        body.addStatement(varDef);
        return varDef.var;
    }

    public static BVarSymbol addRecordVarDef(FunctionDeploymentContext ctx, String type, String name) {
        GlobalContext globalCtx = ctx.globalCtx;
        BLangFunction func = ctx.function;
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = globalCtx.azureFuncsPkgSymbol.scope.lookup(new Name(type)).symbol.type;
        varDef.var = createVariable(globalCtx.pos, varDef.type, name, func.symbol);
        varDef.var.expr = createEmptyRecordLiteral(varDef.type);
        varDef.pos = globalCtx.pos;
        ((BLangBlockFunctionBody) func.getBody()).addStatement(varDef);
        return varDef.var.symbol;
    }
    
    public static BLangType createJsonTypeNode(DiagnosticPos pos, GlobalContext ctx) {
        BLangType nillType = new BLangValueType(TypeKind.JSON);
        nillType.type = ctx.symTable.jsonType;
        return nillType;
    }

    public static BLangType createNillTypeNode(GlobalContext ctx, DiagnosticPos pos) {
        BLangType nillType = new BLangValueType(TypeKind.NIL);
        nillType.type = ctx.symTable.nilType;
        return nillType;
    }

    public static BLangType createErrorNillTypeNode(GlobalContext ctx, DiagnosticPos pos) {
        BLangType errorNillType = new BLangValueType(TypeKind.UNION);
        errorNillType.type = ctx.symTable.errorOrNilType;
        return errorNillType;
    }

    public static BLangInvocation createAzurePkgInvocationNode(FunctionDeploymentContext ctx, String functionName,
            BLangExpression... args) {
        return createInvocationNode(ctx.globalCtx.azureFuncsPkgSymbol.scope.lookup(new Name(functionName)).symbol,
                args);
    }
        
    public static BLangInvocation createInvocationNode(BPackageSymbol pkgSymbol, String functionName,
            BLangExpression... args) {
        return createInvocationNode(pkgSymbol.scope.lookup(new Name(functionName)).symbol, args);
    }

    public static BLangInvocation createInvocationNode(BSymbol funcSymbol, BLangExpression... args) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(funcSymbol.name.value);
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        invocationNode.symbol = funcSymbol;
        invocationNode.type = new BNilType();
        invocationNode.requiredArgs = Arrays.asList(args);
        return invocationNode;
    }

    private static String generateHandlerFuncName(String baseName) {
        return baseName + "_" + Constants.GEN_FUNC_SUFFIX;
    }

    public static BType extractRequestParamsType(GlobalContext ctx) {
        return ctx.azureFuncsPkgSymbol.scope.lookup(new Name(Constants.REQUEST_PARAMS_TYPE)).symbol.type;
    }

    public static BLangFunction createHandlerFunction(GlobalContext ctx, DiagnosticPos pos, 
            String baseName, BLangPackage packageNode) {
        List<String> paramNames = Arrays.asList(Constants.REQUEST_PARAMS_NAME);
        List<BType> paramTypes = Arrays.asList(extractRequestParamsType(ctx));
        BLangType retType = createErrorNillTypeNode(ctx, pos);
        BLangFunction handlerFunc = createFunction(pos, generateHandlerFuncName(baseName), paramNames, paramTypes,
                retType, packageNode);
        return handlerFunc;
    }

    public static void addReturnStatement(GlobalContext ctx, DiagnosticPos pos, BVarSymbol var,
            BLangBlockFunctionBody body) {
        BLangReturn ret = new BLangReturn();
        ret.pos = pos;
        ret.type = var.type;
        ret.expr = createVariableRef(ctx, var);
        body.addStatement(ret);
    }

    public static BLangFunction createFunction(GlobalContext ctx, DiagnosticPos pos, String name,
            BLangPackage packageNode) {
        return createFunction(pos, name, new ArrayList<>(), new ArrayList<>(), createNillTypeNode(ctx, pos),
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
            BLangSimpleVariable var = createVariable(pos, paramTypes.get(i), paramNames.get(i), bLangFunction.symbol);
            bLangFunction.addParameter(var);
            functionSymbol.params.add(var.symbol);
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
