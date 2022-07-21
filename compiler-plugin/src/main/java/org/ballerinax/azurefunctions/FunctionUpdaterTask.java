package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.plugins.ModifierTask;
import io.ballerina.projects.plugins.SourceModifierContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * {@code FunctionUpdaterTask} modifies the source by adding required meta-info for the azure function service
 * declarations.
 */
public class FunctionUpdaterTask implements ModifierTask<SourceModifierContext> {

    @Override
    public void modify(SourceModifierContext context) {
        boolean erroneousCompilation = context.compilation().diagnosticResult()
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        // if the compilation already contains any error, do not proceed
        if (erroneousCompilation) {
            return;
        }

        Module module = context.currentPackage().getDefaultModule();
        SemanticModel semanticModel = module.getCompilation().getSemanticModel();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            ModulePartNode rootNode = document.syntaxTree().rootNode();
            AzureFunctionModifier azureFunctionVisitor = new AzureFunctionModifier(semanticModel);
            Node newNode = rootNode.apply(azureFunctionVisitor);
            SyntaxTree updatedSyntaxTree = document.syntaxTree().modifyWith(newNode);
            context.modifySourceFile(updatedSyntaxTree.textDocument(), documentId);
        }

        // for test files
        for (ModuleId modId : context.currentPackage().moduleIds()) {
            Module currentModule = context.currentPackage().module(modId);
            for (DocumentId docId : currentModule.testDocumentIds()) {
                Document document = module.document(docId);
                ModulePartNode rootNode = document.syntaxTree().rootNode();
                AzureFunctionModifier azureFunctionVisitor = new AzureFunctionModifier(semanticModel);
                Node newNode = rootNode.apply(azureFunctionVisitor);
                SyntaxTree updatedSyntaxTree = document.syntaxTree().modifyWith(newNode);
                context.modifyTestSourceFile(updatedSyntaxTree.textDocument(), docId);
            }
        }

    }
}
