package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.service.blob.BlobOutputBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBOutputBinding;
import org.ballerinax.azurefunctions.service.http.HTTPOutputBinding;
import org.ballerinax.azurefunctions.service.queue.QueueOutputBinding;
import org.ballerinax.azurefunctions.service.twilio.TwilioSmsOutputBinding;

import java.util.Optional;

/**
 * Represents an Output Binding builder for Azure Function services.
 *
 * @since 2.0.0
 */
public class OutputBindingBuilder {

    public Optional<Binding> getOutputBinding(NodeList<AnnotationNode> nodes) {
        for (AnnotationNode annotationNode : nodes) {
            Node node = annotationNode.annotReference();
            if (node.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                continue;
            }
            QualifiedNameReferenceNode qualifiedNameReferenceNode = (QualifiedNameReferenceNode) node;
            String annotationName = qualifiedNameReferenceNode.identifier().text();
            switch (annotationName) {
                case "QueueOutput":
                    return Optional.of(new QueueOutputBinding(annotationNode));
                case "HttpOutput":
                    return Optional.of(new HTTPOutputBinding(annotationNode));
                case "CosmosDBOutput":
                    return Optional.of(new CosmosDBOutputBinding(annotationNode));
                case "TwilioSmsOutput":
                    return Optional.of(new TwilioSmsOutputBinding(annotationNode));
                case "BlobOutput":
                    return Optional.of(new BlobOutputBinding(annotationNode));
                default:
                    throw new RuntimeException("Unexpected property in the annotation");
            }
        }
        return Optional.empty();
    }
}
