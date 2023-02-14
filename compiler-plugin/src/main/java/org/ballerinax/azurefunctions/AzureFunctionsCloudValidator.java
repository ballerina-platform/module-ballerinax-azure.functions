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

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.toml.api.Toml;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;

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

        Optional<Diagnostic> diagnostics = validateCloudOptions(cloud, currentPackage.project());
        diagnostics.ifPresent(compilationAnalysisContext::reportDiagnostic);
    }

    public Location getLocation(Project project) {
        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            DocumentId documentId =
                    project.currentPackage().getDefaultModule().documentIds().stream().findFirst().orElseThrow();
            return project.currentPackage().getDefaultModule().document(documentId).syntaxTree().rootNode().location();
        } else if (project.kind() == ProjectKind.BUILD_PROJECT) {
            BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElseThrow();
            Toml toml = ballerinaToml.tomlDocument().toml();
            return toml.get("build-options.cloud").orElseThrow().location();
        }
        return null;
    }

    public Optional<Diagnostic> validateCloudOptions(String givenCloud, Project project) {
        if (givenCloud == null || givenCloud.isEmpty() || givenCloud.equals(Constants.AZURE_FUNCTIONS_BUILD_OPTION) ||
                givenCloud.equals(Constants.AZURE_FUNCTIONS_LOCAL_BUILD_OPTION)) {
            return Optional.empty();
        }
        Location location = getLocation(project);
        return Optional.of(Util.getDiagnostic(location, AzureDiagnosticCodes.AF_016, givenCloud));
    }
}
