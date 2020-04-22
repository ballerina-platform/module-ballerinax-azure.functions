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

import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportedAnnotationPackages;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.FunctionNode;
import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinalang.util.exceptions.BallerinaException;
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
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiler plugin to process Azure Functions function annotations.
 */
@SupportedAnnotationPackages(value = "ballerinax/azurefunctions:0.0.0")
public class AzureFunctionsPlugin extends AbstractCompilerPlugin {

    private static final String AZURE_FUNCS_OUTPUT_ZIP_FILENAME = "azure-functions.zip";

    private static final String AZURE_FUNCTIONS_PACKAGE_NAME = "azurefunctions";

    private static final String AZURE_FUNCTIONS_PACKAGE_ORG = "ballerinax"; 

    private static final String AZURE_FUNCS_REG_FUNCTION_NAME = "__register";

    private static final String MAIN_FUNC_NAME = "main";
    
    private static final PrintStream OUT = System.out;
    
    private static List<String> generatedFuncs = new ArrayList<>();
    
    private DiagnosticLog dlog;
    
    private SymbolTable symTable;
    
    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
    }
    
    public void setCompilerContext(CompilerContext context) {
        this.symTable = SymbolTable.getInstance(context);
    }
        
    @Override
    public void process(PackageNode packageNode) {
        List<BLangFunction> azureFunctions = new ArrayList<>();
        for (FunctionNode fn : packageNode.getFunctions()) {
            BLangFunction bfn = (BLangFunction) fn;
            if (this.isAzureFunction(bfn)) {
                azureFunctions.add(bfn);
            }
        }
        BLangPackage myPkg = (BLangPackage) packageNode;
        if (!azureFunctions.isEmpty()) {
            BPackageSymbol azureFuncsPkgSymbol = this.extractAzureFuncsPackageSymbol(myPkg);
            if (azureFuncsPkgSymbol == null) {
                // this symbol will always be there, since the import is needed to add the annotation
                throw new BallerinaException("Azure Functions package symbol cannot be found");
            }
            BLangFunction epFunc = this.extractMainFunction(myPkg);
            if (epFunc == null) {
                // main function is not there, lets create our own one
                epFunc = this.createFunction(myPkg.pos, MAIN_FUNC_NAME, myPkg);
                packageNode.addFunction(epFunc);
            } else {
                // clear out the existing statements
                ((BLangBlockFunctionBody) epFunc.body).stmts.clear();
            }
            BLangBlockFunctionBody body = (BLangBlockFunctionBody) epFunc.body;
            for (BLangFunction func : azureFunctions) {
                this.addRegisterCall(myPkg.pos, azureFuncsPkgSymbol, body, func);
                AzureFunctionsPlugin.generatedFuncs.add(func.name.value);
            }
        }
    }
    
    private BLangFunction extractMainFunction(BLangPackage myPkg) {
        for (BLangFunction func : myPkg.getFunctions()) {
            if (MAIN_FUNC_NAME.equals(func.getName().value)) {
                return func;
            }
        }
        return null;
    }
    
    private BPackageSymbol extractAzureFuncsPackageSymbol(BLangPackage myPkg) {
        for (BLangImportPackage pi : myPkg.imports) {
            if (AZURE_FUNCTIONS_PACKAGE_ORG.equals(pi.orgName.value) && pi.pkgNameComps.size() == 1 && 
                    AZURE_FUNCTIONS_PACKAGE_NAME.equals(pi.pkgNameComps.get(0).value)) {
                return pi.symbol;
            }
        }
        return null;
    }
    
    private void addRegisterCall(DiagnosticPos pos, BPackageSymbol lamdaPkgSymbol, BLangBlockFunctionBody blockStmt,
                                 BLangFunction func) {
        List<BLangExpression> exprs = new ArrayList<>();
        exprs.add(this.createStringLiteral(pos, func.name.value));
        exprs.add(this.createVariableRef(pos, func.symbol));
        BLangInvocation inv = this.createInvocationNode(lamdaPkgSymbol, AZURE_FUNCS_REG_FUNCTION_NAME, exprs);
        BLangExpressionStmt stmt = new BLangExpressionStmt(inv);
        stmt.pos = pos;
        blockStmt.addStatement(stmt);
    }
    
    private BLangLiteral createStringLiteral(DiagnosticPos pos, String value) {
        BLangLiteral stringLit = new BLangLiteral();
        stringLit.pos = pos;
        stringLit.value = value;
        stringLit.type = symTable.stringType;
        return stringLit;
    }
    
    private BLangSimpleVarRef createVariableRef(DiagnosticPos pos, BSymbol varSymbol) {
        final BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pos = pos;
        varRef.variableName = ASTBuilderUtil.createIdentifier(pos, varSymbol.name.value);
        varRef.symbol = varSymbol;
        varRef.type = varSymbol.type;
        return varRef;
    }
        
    private BLangInvocation createInvocationNode(BPackageSymbol pkgSymbol, String functionName,
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
    
    private BLangFunction createFunction(DiagnosticPos pos, String name, BLangPackage packageNode) {
        final BLangFunction bLangFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        final IdentifierNode funcName = ASTBuilderUtil.createIdentifier(pos, name);
        bLangFunction.setName(funcName);
        bLangFunction.flagSet = EnumSet.of(Flag.PUBLIC);
        bLangFunction.pos = pos;
        bLangFunction.type = new BInvokableType(new ArrayList<>(), new BNilType(), null);
        bLangFunction.body = this.createBlockStmt(pos);
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(bLangFunction.flagSet),
                new Name(bLangFunction.name.value), packageNode.packageID, 
                bLangFunction.type, packageNode.symbol, true);
        functionSymbol.scope = new Scope(functionSymbol);
        bLangFunction.symbol = functionSymbol;
        return bLangFunction;
    }
    
    private BLangFunctionBody createBlockStmt(DiagnosticPos pos) {
        final BLangFunctionBody blockNode = (BLangFunctionBody) TreeBuilder.createBlockFunctionBodyNode();
        blockNode.pos = pos;
        return blockNode;
    }
    
    private boolean isAzureFunction(BLangFunction fn) {
        List<BLangAnnotationAttachment> annotations = fn.annAttachments;
        boolean hasAzureFuncsAnnon = false;
        for (AnnotationAttachmentNode attachmentNode : annotations) {
            hasAzureFuncsAnnon = this.hasAzureFunctionsAnnotation(attachmentNode);
            if (hasAzureFuncsAnnon) {
                break;
            }
        }
        if (hasAzureFuncsAnnon) {
            BLangFunction bfn = (BLangFunction) fn;
            if (!this.validateAzureFunction(bfn)) {
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
    
    private boolean validateAzureFunction(BLangFunction node) {
        return true;
    }
    
    private boolean hasAzureFunctionsAnnotation(AnnotationAttachmentNode attachmentNode) {
        BAnnotationSymbol symbol = ((BLangAnnotationAttachment) attachmentNode).annotationSymbol;
        return AZURE_FUNCTIONS_PACKAGE_ORG.equals(symbol.pkgID.orgName.value) && 
                AZURE_FUNCTIONS_PACKAGE_NAME.equals(symbol.pkgID.name.value) && "Function".equals(symbol.name.value);
    }

    @Override
    public void codeGenerated(PackageID packageID, Path binaryPath) {
        if (AzureFunctionsPlugin.generatedFuncs.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        OUT.println("\t@azurefunctions:Function: " + String.join(", ", AzureFunctionsPlugin.generatedFuncs));
        try {
            this.generateZipFile(binaryPath);
        } catch (IOException e) {
            throw new BallerinaException("Error generating Azure Functions zip file: " + e.getMessage(), e);
        }
        OUT.println("\n\tRun the following command to deploy Ballerina Azure Functions:");
        OUT.println("\taz functionapp deployment source config-zip -g <resource_group> -n <function_app_name> --src " 
                    + AZURE_FUNCS_OUTPUT_ZIP_FILENAME);
    }
    
    private void generateZipFile(Path binaryPath) throws IOException {
        Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + binaryPath.toAbsolutePath().getParent().resolve(
                AZURE_FUNCS_OUTPUT_ZIP_FILENAME).toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path pathInZipfile = zipfs.getPath("/" + binaryPath.getFileName());          
            Files.copy(binaryPath, pathInZipfile, StandardCopyOption.REPLACE_EXISTING); 
        }
    }

}
