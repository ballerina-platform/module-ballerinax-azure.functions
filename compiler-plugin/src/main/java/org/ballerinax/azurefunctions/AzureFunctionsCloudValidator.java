/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Code Analyzer responsible to validate the cloud build options.
 *
 * @since 3.1.0
 */
public class AzureFunctionsCloudValidator implements AnalysisTask<CompilationAnalysisContext> {

    @Override
    public void perform(CompilationAnalysisContext compilationAnalysisContext) {
        boolean erroneousCompilation = compilationAnalysisContext.compilation().diagnosticResult().hasErrors();
        if (erroneousCompilation) {
            return;
        }
        
        Package currentPackage = compilationAnalysisContext.currentPackage();
        String cloud = currentPackage.project().buildOptions().cloud();
        List<Location> locations = new ArrayList<>();
        Module module = currentPackage.getDefaultModule();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            ModulePartNode rootNode = document.syntaxTree().rootNode();
            AzureFunctionDocumentVisitor azureFunctionVisitor = new AzureFunctionDocumentVisitor();
            azureFunctionVisitor.visit(rootNode);
            locations.addAll(azureFunctionVisitor.getLocations());
        }
        
        for (DocumentId documentId : module.testDocumentIds()) {
            Document document = module.document(documentId);
            ModulePartNode rootNode = document.syntaxTree().rootNode();
            AzureFunctionDocumentVisitor azureFunctionVisitor = new AzureFunctionDocumentVisitor();
            azureFunctionVisitor.visit(rootNode);
            locations.addAll(azureFunctionVisitor.getLocations());
        }
        
        Optional<Diagnostic> diagnostics = validateCloudOptions(cloud, locations.get(0));
        diagnostics.ifPresent(compilationAnalysisContext::reportDiagnostic);
    }
    
    public Optional<Diagnostic> validateCloudOptions(String givenCloud, Location location) {
        if (givenCloud == null || givenCloud.isEmpty()) {
            return Optional.of(Util.getDiagnostic(location, AzureDiagnosticCodes.AF_016));
        }
        if (givenCloud.equals(Constants.AZURE_FUNCTIONS_BUILD_OPTION)) {
            return Optional.empty();
        }
        if (givenCloud.equals(Constants.AZURE_FUNCTIONS_LOCAL_BUILD_OPTION)) {
            return Optional.empty();
        }
        
        return Optional.of(Util.getDiagnostic(location, AzureDiagnosticCodes.AF_017, givenCloud));
    }
}
