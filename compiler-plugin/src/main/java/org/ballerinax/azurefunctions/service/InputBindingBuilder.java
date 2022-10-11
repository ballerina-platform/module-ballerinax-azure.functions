package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.service.blob.BlobInputBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBInputBinding;

import java.util.Optional;

/**
 * Represents an Input Binding builder for Azure Function services.
 *
 * @since 2.0.0
 */
public class InputBindingBuilder {
    
    public Optional<Binding> getInputBinding(NodeList<AnnotationNode> annotations, String varName) {
        for (AnnotationNode annotation : annotations) {
            Node annotRef = annotation.annotReference();
            if (annotRef.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                QualifiedNameReferenceNode annotationRef = (QualifiedNameReferenceNode) annotRef;
                String annotationText = annotationRef.identifier().text();
                if (annotationText.equals("CosmosDBInput")) { 
                    return Optional.of(new CosmosDBInputBinding(annotation, varName));
                }
                if (annotationText.equals("BlobInput")) {
                    return Optional.of(new BlobInputBinding(annotation, varName));
                }
                //TODO Add other stuff
            }
        }
        return Optional.empty();
    }
}
