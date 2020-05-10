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
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
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
import java.util.List;
import java.util.Map;

/**
 * Compiler plugin to process Azure Functions function annotations.
 */
@SupportedAnnotationPackages(value = "ballerinax/azurefunctions:0.0.0")
public class AzureFunctionsPlugin extends AbstractCompilerPlugin {

    private static final PrintStream OUT = System.out;

    private static List<String> azureFuncNames = new ArrayList<>();

    private DiagnosticLog dlog;

    private Context ctx = new Context();

    @Override
    public void init(DiagnosticLog dlog) {
        this.dlog = dlog;
    }

    public void setCompilerContext(CompilerContext cctx) {
        this.ctx.init(cctx);
    }

    @Override
    public void process(PackageNode packageNode) {
        List<BLangFunction> azureFunctions;
        BLangPackage bpn = (BLangPackage) packageNode;
        try {
            azureFunctions = this.generateHandlerFunctions(bpn);
            this.registerHandlerFunctions(bpn, azureFunctions);
        } catch (AzureFunctionsException e) {
            this.dlog.logDiagnostic(Diagnostic.Kind.ERROR, packageNode.getPosition(), e.getMessage());
        }
    }

    private BLangFunction generateHandlerFunction(BLangPackage packageNode, BLangFunction sourceFunc)
            throws AzureFunctionsException {
        return Utils.createHandlerFunction(ctx, sourceFunc.pos, sourceFunc.name.value, packageNode);
    }

    private List<BLangFunction> generateHandlerFunctions(BLangPackage packageNode) throws AzureFunctionsException {
        List<BLangFunction> handlerFunctions = new ArrayList<>();
        for (FunctionNode fn : packageNode.getFunctions()) {
            BLangFunction bfn = (BLangFunction) fn;
            if (Utils.isAzureFunction(bfn, this.dlog)) {
                handlerFunctions.add(this.generateHandlerFunction(packageNode, bfn));
                azureFuncNames.add(bfn.name.value);
            }
        }
        for (BLangFunction func : handlerFunctions) {
            packageNode.addFunction(func);
        }
        return handlerFunctions;
    }

    private void registerHandlerFunctions(BLangPackage myPkg, List<BLangFunction> azureFunctions) {
        if (azureFunctions.isEmpty()) {
            return;
        }
        BPackageSymbol azureFuncsPkgSymbol = Utils.extractAzureFuncsPackageSymbol(myPkg);
        if (azureFuncsPkgSymbol == null) {
            // this symbol will always be there, since the import is needed to add the annotation
            throw new BallerinaException("Azure Functions package symbol cannot be found");
        }
        BLangFunction epFunc = Utils.extractMainFunction(myPkg);
        if (epFunc == null) {
            // main function is not there, lets create our own one
            epFunc = Utils.createFunction(myPkg.pos, Constants.MAIN_FUNC_NAME, myPkg);
            myPkg.addFunction(epFunc);
        } else {
            // clear out the existing statements
            ((BLangBlockFunctionBody) epFunc.body).stmts.clear();
        }
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) epFunc.body;
        for (BLangFunction func : azureFunctions) {
            String name = func.name.value;
            Utils.addRegisterCall(this.ctx, myPkg.pos, azureFuncsPkgSymbol, body, name, func);
        }
    }
        
    @Override
    public void codeGenerated(PackageID packageID, Path binaryPath) {
        if (azureFuncNames.isEmpty()) {
            // no azure functions, nothing else to do
            return;
        }
        OUT.println("\t@azurefunctions:Function: " + String.join(", ", azureFuncNames));
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
