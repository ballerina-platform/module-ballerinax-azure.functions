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
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.FunctionNode;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Compiler plugin to process Azure Functions function annotations.
 */
@SupportedAnnotationPackages(value = "ballerinax/azurefunctions:0.0.0")
public class AzureFunctionsPlugin extends AbstractCompilerPlugin {

    private static final PrintStream OUT = System.out;

    private static Map<String, BLangFunction> generatedFunctions = new LinkedHashMap<>();

    private DiagnosticLog dlog;

    private GlobalContext globalCtx;

    @Override
    public void init(DiagnosticLog dlog) {
        this.dlog = dlog;
    }

    public void setCompilerContext(CompilerContext cctx) {
        this.globalCtx = new GlobalContext(cctx);
    }

    @Override
    public void process(PackageNode packageNode) {
        BLangPackage bpn = (BLangPackage) packageNode;
        this.globalCtx.pos = bpn.pos;
        this.globalCtx.azureFuncsPkgSymbol = Utils.extractAzureFuncsPackageSymbol(bpn);
        try {
            generatedFunctions.putAll(this.generateHandlerFunctions(bpn));
            this.registerHandlerFunctions(bpn, generatedFunctions);
        } catch (AzureFunctionsException e) {
            this.dlog.logDiagnostic(Diagnostic.Kind.ERROR, packageNode.getPosition(), e.getMessage());
        }
    }
    
    private FunctionDeploymentContext createFuncDeplContext(BLangPackage packageNode, BLangFunction sourceFunc)
            throws AzureFunctionsException {
        FunctionDeploymentContext ctx = new FunctionDeploymentContext();
        ctx.globalCtx = this.globalCtx;
        ctx.pkg = packageNode;
        BLangFunction func = Utils.createHandlerFunction(this.globalCtx, sourceFunc.pos, 
                sourceFunc.name.value, packageNode);
        ctx.function = func;
        ctx.handlerParams = func.getParameters().get(0).symbol;
        // all the parameter handlers needs to be created and put to the context first
        // before the init and other operations are called
        for (BLangSimpleVariable param : sourceFunc.getParameters()) {
            ctx.parameterHandlers.add(HandlerFactory.createParameterHandler(param));
        }
        ctx.returnHandler = HandlerFactory.createReturnHandler(ctx.globalCtx,
                Utils.extractNonErrorType(ctx.globalCtx, sourceFunc.getReturnTypeNode().type),
                sourceFunc.getReturnTypeAnnotationAttachments());
        for (ParameterHandler ph : ctx.parameterHandlers) {
            ph.init(ctx);
        }
        if (ctx.returnHandler != null) {
            ctx.returnHandler.init(ctx);
        }
        return ctx;
    }

    private BLangFunction generateHandlerFunction(BLangPackage packageNode, BLangFunction sourceFunc)
            throws AzureFunctionsException {
        FunctionDeploymentContext ctx = this.createFuncDeplContext(packageNode, sourceFunc);
        List<BLangExpression> args = new ArrayList<>();
        for (ParameterHandler ph : ctx.parameterHandlers) {
            args.add(Utils.createCheckedExpr(ctx.globalCtx, ph.invocationProcess()));
        }
        BLangSimpleVariable retVal = Utils.addFunctionCall(ctx, sourceFunc.symbol, true,
                args.toArray(new BLangExpression[0]));
        for (ParameterHandler ph : ctx.parameterHandlers) {
            ph.postInvocationProcess();
        }
        ReturnHandler retHandler = ctx.returnHandler;
        if (retHandler != null) {
            retHandler.postInvocationProcess(Utils.createVariableRef(ctx.globalCtx, (BVarSymbol) retVal.symbol));
        }
        return ctx.function;
    }

    private Map<String, BLangFunction> generateHandlerFunctions(BLangPackage packageNode)
            throws AzureFunctionsException {
        Map<String, BLangFunction> funcs = new LinkedHashMap<>();
        for (FunctionNode fn : packageNode.getFunctions()) {
            BLangFunction bfn = (BLangFunction) fn;
            if (Utils.isAzureFunction(bfn, this.dlog)) {
                funcs.put(bfn.name.value, this.generateHandlerFunction(packageNode, bfn));
            }
        }
        for (BLangFunction func : funcs.values()) {
            packageNode.addFunction(func);
        }
        return funcs;
    }

    private void registerHandlerFunctions(BLangPackage myPkg, Map<String, BLangFunction> azureFunctions) {
        if (azureFunctions.isEmpty()) {
            return;
        }
        BPackageSymbol azureFuncsPkgSymbol = this.globalCtx.azureFuncsPkgSymbol;
        if (azureFuncsPkgSymbol == null) {
            // this symbol will always be there, since the import is needed to add the annotation
            throw new BallerinaException("Azure Functions package symbol cannot be found");
        }
        BLangFunction epFunc = Utils.extractMainFunction(myPkg);
        if (epFunc == null) {
            // main function is not there, lets create our own one
            epFunc = Utils.createFunction(this.globalCtx, Constants.MAIN_FUNC_NAME, myPkg);
            myPkg.addFunction(epFunc);
        } else {
            // clear out the existing statements
            ((BLangBlockFunctionBody) epFunc.body).stmts.clear();
        }
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) epFunc.body;
        for (Entry<String, BLangFunction> entry : azureFunctions.entrySet()) {
            String name = entry.getKey();
            BLangFunction func = entry.getValue();
            Utils.addRegisterCall(this.globalCtx, myPkg.pos, azureFuncsPkgSymbol, body, name, func);
        }
    }
        
    @Override
    public void codeGenerated(PackageID packageID, Path binaryPath) {
        if (generatedFunctions.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        OUT.println("\t@azurefunctions:Function: " + String.join(", ", generatedFunctions.keySet()));
        try {
            this.generateZipFile(binaryPath);
        } catch (IOException e) {
            throw new BallerinaException("Error generating Azure Functions zip file: " + e.getMessage(), e);
        }
        OUT.println("\n\tRun the following command to deploy Ballerina Azure Functions:");
        OUT.println("\taz functionapp deployment source config-zip -g <resource_group> -n <function_app_name> --src " 
                + Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME);
    }
    
    private void generateZipFile(Path binaryPath) throws IOException {
        Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + binaryPath.toAbsolutePath().getParent()
                .resolve(Constants.AZURE_FUNCS_OUTPUT_ZIP_FILENAME).toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path pathInZipfile = zipfs.getPath("/" + binaryPath.getFileName());          
            Files.copy(binaryPath, pathInZipfile, StandardCopyOption.REPLACE_EXISTING); 
        }
    }

}
