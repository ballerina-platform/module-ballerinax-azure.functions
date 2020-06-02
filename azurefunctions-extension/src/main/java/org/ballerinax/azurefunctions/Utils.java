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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinax.azurefunctions.handlers.http.HTTPOutputParameterHandler;
import org.ballerinax.azurefunctions.handlers.http.HTTPReturnHandler;
import org.ballerinax.azurefunctions.handlers.http.HTTPTriggerParameterHandler;
import org.wso2.ballerinalang.compiler.desugar.ASTBuilderUtil;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BServiceType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValueField;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static boolean isAzureFuncsModule(PackageID pkgId) {
        return Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(pkgId.orgName.value)
                && Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(pkgId.name.value);
    }
    
    public static BPackageSymbol extractAzureFuncsPackageSymbol(BLangPackage myPkg) {
        for (BLangImportPackage pi : myPkg.imports) {
            if (isAzureFuncsModule(pi.symbol.pkgID)) {
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

    public static BLangSimpleVariable addAzurePkgFunctionCall(FunctionDeploymentContext ctx, String name,
            boolean checked, BLangExpression... exprs) {
        return addFunctionCall(ctx, createAzurePkgInvocationNode(ctx, name, exprs), checked);
    }

    public static BLangSimpleVariable addFunctionCall(FunctionDeploymentContext ctx, BSymbol funcSymbol,
            boolean checked, BLangExpression... exprs) {
        return addFunctionCall(ctx, createInvocationNode(funcSymbol, exprs), checked);
    }

    public static BLangSimpleVariable addFunctionCall(FunctionDeploymentContext ctx, BPackageSymbol pkgSymbol,
            String name, boolean checked, BLangExpression... exprs) {
        return addFunctionCall(ctx, createInvocationNode(pkgSymbol, name, exprs), checked);
    }

    public static BLangSimpleVariable addFunctionCall(FunctionDeploymentContext ctx, BLangInvocation inv,
            boolean checked) {
        BLangExpression expr;
        if (checked) {
            expr = createCheckedExpr(ctx.globalCtx, inv);
        } else {
            expr = inv;
        }
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = expr.type;
        varDef.var = createVariable(ctx.globalCtx, expr.type, ctx.getNextVarName(), ctx.function.symbol);
        varDef.var.expr = expr;
        ((BLangBlockFunctionBody) ctx.function.body).addStatement(varDef);
        return varDef.var;
    }

    public static BLangExpressionStmt createExpressionStmt(GlobalContext ctx, BLangExpression expr) {
        BLangExpressionStmt stmt = new BLangExpressionStmt(expr);
        stmt.pos = ctx.pos;
        return stmt;
    }
    
    public static BLangLiteral createStringLiteral(GlobalContext ctx, String value) {
        BLangLiteral stringLit = new BLangLiteral();
        stringLit.pos = ctx.pos;
        stringLit.value = value;
        stringLit.type = ctx.symTable.stringType;
        return stringLit;
    }

    public static BLangLiteral createBooleanLiteral(GlobalContext ctx, boolean value) {
        BLangLiteral stringLit = new BLangLiteral();
        stringLit.pos = ctx.pos;
        stringLit.value = value;
        stringLit.type = ctx.symTable.booleanType;
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
    
    public static BLangSimpleVariable createVariable(GlobalContext ctx, BType type, String name, BSymbol owner) {
        BLangSimpleVariable var = (BLangSimpleVariable) TreeBuilder.createSimpleVariableNode();
        var.pos = ctx.pos;
        var.name = ASTBuilderUtil.createIdentifier(ctx.pos, name);
        var.type = type;
        var.symbol = new BVarSymbol(0, new Name(name), type.tsymbol.pkgID, type, owner);
        return var;
    }

    public static BLangSimpleVariable addJSONVarDef(FunctionDeploymentContext ctx, String name, BSymbol owner, 
            BLangBlockFunctionBody body) {
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = ctx.globalCtx.symTable.jsonType;
        varDef.var = createVariable(ctx.globalCtx, varDef.type, name, owner);
        varDef.var.expr = createEmptyRecordLiteral(ctx.globalCtx.symTable.mapJsonType);
        varDef.pos = ctx.globalCtx.pos;
        body.addStatement(varDef);
        return varDef.var;
    }

    public static boolean isAzurePkgType(FunctionDeploymentContext ctx, String azTypeName, BType targetType) {
        BType sourceType = lookupAzurePkgType(ctx, azTypeName);
        return sourceType.equals(targetType);
    }

    public static BType lookupAzurePkgType(FunctionDeploymentContext ctx, String type) {
        GlobalContext globalCtx = ctx.globalCtx;
        return globalCtx.azureFuncsPkgSymbol.scope.lookup(new Name(type)).symbol.type;
    }

    public static BVarSymbol addAzurePkgRecordVarDef(FunctionDeploymentContext ctx, String type, String name) {
        GlobalContext globalCtx = ctx.globalCtx;
        BLangFunction func = ctx.function;
        BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) TreeBuilder.createSimpleVariableDefinitionNode();
        varDef.type = lookupAzurePkgType(ctx, type);
        varDef.var = createVariable(globalCtx, varDef.type, name, func.symbol);
        varDef.var.expr = createEmptyRecordLiteral(varDef.type);
        varDef.pos = globalCtx.pos;
        ((BLangBlockFunctionBody) func.getBody()).addStatement(varDef);
        return varDef.var.symbol;
    }
    
    public static BLangType createJsonTypeNode(GlobalContext ctx) {
        BLangType nillType = new BLangValueType(TypeKind.JSON);
        nillType.type = ctx.symTable.jsonType;
        return nillType;
    }

    public static BLangType createNillTypeNode(GlobalContext ctx) {
        BLangType nillType = new BLangValueType(TypeKind.NIL);
        nillType.type = ctx.symTable.nilType;
        return nillType;
    }

    public static BLangType createErrorNillTypeNode(GlobalContext ctx) {
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
        invocationNode.type = funcSymbol.getType().getReturnType();
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
        BLangType retType = createErrorNillTypeNode(ctx);
        BLangFunction handlerFunc = createFunction(ctx, generateHandlerFuncName(baseName), paramNames, paramTypes,
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

    public static BLangFunction createFunction(GlobalContext ctx, String name, BLangPackage packageNode) {
        return createFunction(ctx, name, new ArrayList<>(), new ArrayList<>(), createNillTypeNode(ctx), packageNode);
    }

    public static BLangFunction createFunction(GlobalContext ctx, String name, List<String> paramNames, 
            List<BType> paramTypes, BLangType retType, BLangPackage packageNode) {
        final BLangFunction bLangFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        final IdentifierNode funcName = ASTBuilderUtil.createIdentifier(ctx.pos, name);
        bLangFunction.setName(funcName);
        bLangFunction.flagSet = EnumSet.of(Flag.PUBLIC);
        bLangFunction.pos = ctx.pos;
        bLangFunction.type = new BInvokableType(paramTypes, retType.type, null);
        bLangFunction.body = createBlockStmt(ctx.pos);
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(bLangFunction.flagSet),
                new Name(bLangFunction.name.value), packageNode.packageID, 
                bLangFunction.type, packageNode.symbol, true);
        functionSymbol.type = bLangFunction.type;
        functionSymbol.retType = retType.type;
        functionSymbol.scope = new Scope(functionSymbol);
        bLangFunction.symbol = functionSymbol;
        for (int i = 0; i < paramNames.size(); i++) {
            BLangSimpleVariable var = createVariable(ctx, paramTypes.get(i), paramNames.get(i), bLangFunction.symbol);
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
                && Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(symbol.pkgID.name.value)
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

    public static BLangAnnotationAttachment extractAzureFunctionAnnotation(List<BLangAnnotationAttachment> annons) {
        for (AnnotationAttachmentNode an : annons) {
            BLangAnnotationAttachment ban = (BLangAnnotationAttachment) an;
            if (isAzureFuncsModule(ban.annotationSymbol.pkgID)) {
                return ban;
            }
        }
        return null;
    }

    public static boolean isErrorType(GlobalContext ctx, BType type) {
        return ctx.symTable.errorType.tsymbol.name.getValue().equals(type.tsymbol.name.getValue());
    }

    public static BType extractNonErrorType(GlobalContext ctx, BType type) {
        if (type instanceof BUnionType) {
            BUnionType uType = (BUnionType) type;
            LinkedHashSet<BType> newTypes = new LinkedHashSet<>();
            for (BType mt : uType.getMemberTypes()) {
                if (!isErrorType(ctx, mt)) {
                    newTypes.add(mt);
                }
            }
            if (newTypes.size() == 1) {
                type = newTypes.iterator().next();
            } else {
                type = BUnionType.create(uType.tsymbol, newTypes);
            }
        }
        return type;
    }

    public static BLangExpression createCheckedExpr(GlobalContext ctx, BLangExpression subexpr) {
        BLangCheckedExpr expr = new BLangCheckedExpr();
        expr.expr = subexpr;
        expr.type = extractNonErrorType(ctx, subexpr.type);
        expr.equivalentErrorTypeList = new ArrayList<>();
        return expr;
    }

    public static boolean isSingleInputBinding(FunctionDeploymentContext ctx) {
        int count = 0;
        for (ParameterHandler ph : ctx.parameterHandlers) {
            if (ph.getBindingType() == BindingType.INPUT || ph.getBindingType() == BindingType.TRIGGER) {
                count++;
            }
        }
        return count == 1;
    }

    public static boolean isSingleOutputBinding(FunctionDeploymentContext ctx) {
        return getOutputBindingCount(ctx) == 1;
    }

    public static int getOutputBindingCount(FunctionDeploymentContext ctx) {
        int count = 0;
        for (ParameterHandler ph : ctx.parameterHandlers) {
            if (ph.getBindingType() == BindingType.OUTPUT) {
                count++;
            }
        }
        return count;
    }

    public static boolean isHTTPTriggerAvailable(FunctionDeploymentContext ctx) {
        for (ParameterHandler ph : ctx.parameterHandlers) {
            if (ph.getBindingType() == BindingType.TRIGGER) {
                if (ph instanceof HTTPTriggerParameterHandler) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isHTTPOutputAvailable(FunctionDeploymentContext ctx) {
        for (ParameterHandler ph : ctx.parameterHandlers) {
            if (ph.getBindingType() == BindingType.OUTPUT) {
                if (ph instanceof HTTPOutputParameterHandler) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPureHTTPBinding(FunctionDeploymentContext ctx) {
        if (!isSingleInputBinding(ctx)) {
            return false;
        }
        if (!isHTTPTriggerAvailable(ctx)) {
            return false;
        }
        int outBindingCount = getOutputBindingCount(ctx);
        if (outBindingCount > 1) {
            return false;
        }
        if (outBindingCount == 1 && !isHTTPOutputAvailable(ctx)) {
            return false;
        }
        if (ctx.returnHandler != null && !(ctx.returnHandler instanceof HTTPReturnHandler)) {
            return false;
        }
        return true;
    }

    public static int getFunctionTriggerCount(FunctionDeploymentContext ctx) {
        int count = 0;
        for (ParameterHandler ph : ctx.parameterHandlers) {
            if (ph.getBindingType() == BindingType.TRIGGER) {
                count++;
            }
        }
        return count;
    }

    public static boolean isHTTPModule(PackageID pkgId) {
        return Constants.BALLERINA_ORG.equals(pkgId.orgName.value)
                && Constants.HTTP_MODULE_NAME.equals(pkgId.name.value);
    }

    public static boolean isHTTPRequestType(BType type) {
        String name = type.tsymbol.name.value;
        PackageID pkgId = type.tsymbol.pkgID;
        return Constants.HTTP_REQUEST_NAME.equals(name) && Utils.isHTTPModule(pkgId);
    }

    public static boolean isContextType(BType type) {
        String name = type.tsymbol.name.value;
        PackageID pkgId = type.tsymbol.pkgID;
        return Constants.AZURE_FUNCTIONS_CONTEXT_NAME.equals(name) && Utils.isAzureFuncsModule(pkgId);
    }

    public static boolean isStringType(GlobalContext ctx, BType type) {
        return ctx.symTable.stringType.equals(type);
    }

    public static boolean isJsonType(GlobalContext ctx, BType type) {
        return ctx.symTable.jsonType.equals(type);
    }

    public static boolean isByteArray(GlobalContext ctx, BType type) {
        if (type instanceof BArrayType) {
            BArrayType baType = (BArrayType) type;
            return ctx.symTable.byteType.equals(baType.eType);
        }
        return false;
    }

    public static boolean isOptionalByteArray(GlobalContext ctx, BType type) {
        if (!(type instanceof BUnionType)) {
            return false;
        }
        BUnionType unionType = (BUnionType) type;
        List<BType> memberTypes = new ArrayList<>(unionType.getMemberTypes());
        if (memberTypes.size() != 2) {
            return false;
        }
        return ((isByteArray(ctx, memberTypes.get(0)) && ctx.symTable.nilType.equals(memberTypes.get(1)))
                || (isByteArray(ctx, memberTypes.get(1)) && ctx.symTable.nilType.equals(memberTypes.get(0))));
    }

    public static JsonElement objectToJson(Object obj) {
        if (obj instanceof String) {
            return new JsonPrimitive((String) obj);
        } else if (obj instanceof Number) {
            return new JsonPrimitive((Number) obj);
        } else if (obj instanceof Boolean) {
            return new JsonPrimitive((Boolean) obj);
        } else if (obj instanceof String[]) {
            JsonArray array = new JsonArray();
            for (String item : (String[]) obj) {
                array.add(item);
            }
            return array;
        } else if (obj == null) {
            return JsonNull.INSTANCE;
        } else {
            throw new IllegalStateException("Unsupported type to convert to JSON: " + obj.getClass());
        }
    }

    public static JsonObject createBindingObject(Map<String, Object> binding) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : binding.entrySet()) {
            obj.add(entry.getKey(), objectToJson(entry.getValue()));
        }
        return obj;
    }

    public static void addFunctionBinding(FunctionDeploymentContext ctx, Map<String, Object> binding) {
        if (binding == null) {
            return;
        }
        JsonArray bindings = (JsonArray) ctx.functionDefinition.get(Constants.FUNCTION_BINDINGS_NAME);
        bindings.add(createBindingObject(binding));
    }

    public static Map<String, String> extractAnnotationKeyValues(BLangAnnotationAttachment annotation) {
        BLangRecordLiteral record = (BLangRecordLiteral) annotation.expr;
        List<BLangRecordKeyValueField> fields = record.getFields().stream().map(x -> (BLangRecordKeyValueField) x)
                .collect(Collectors.toList());
        Map<String, String> annonMap = new HashMap<>();
        for (BLangRecordKeyValueField field : fields) {
            annonMap.put(field.key.toString(), field.getValue().toString());
        }
        return annonMap;
    }

    public static void addDummyService(GlobalContext ctx, BLangPackage packageNode) {
        String name = Constants.DUMMY_SERVICE_NAME;
        BLangTypeDefinition typeDef = (BLangTypeDefinition) TreeBuilder.createTypeDefinition();
        BLangIdentifier serviceTypeID = ASTBuilderUtil.createIdentifier(ctx.pos, name);
        typeDef.setName(serviceTypeID);
        typeDef.flagSet.add(Flag.SERVICE);
        typeDef.pos = ctx.pos;
        BLangObjectTypeNode objectTypeNode = (BLangObjectTypeNode) TreeBuilder.createObjectTypeNode();
        objectTypeNode.generatedInitFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        objectTypeNode.generatedInitFunction.symbol = new BInvokableSymbol(SymTag.FUNCTION, 0, new Name(name),
                packageNode.symbol.pkgID,
                new BInvokableType(new ArrayList<>(), ctx.symTable.noType, ctx.symTable.noType, null),
                packageNode.symbol);
        typeDef.setTypeNode(objectTypeNode);
        BObjectTypeSymbol ts = new BObjectTypeSymbol(SymTag.SERVICE, 0, new Name(name), packageNode.symbol.pkgID, null,
                packageNode.symbol);
        BObjectType objectType = new BServiceType(ts);
        ts.type = objectType;
        objectTypeNode.symbol = ts;
        objectTypeNode.type = objectType;
        objectTypeNode.pos = ctx.pos;
        typeDef.symbol = ts;
        packageNode.addTypeDefinition(typeDef);
    }
    
}
