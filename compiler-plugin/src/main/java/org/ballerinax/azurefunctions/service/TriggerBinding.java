package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.FunctionContext;
import org.ballerinax.azurefunctions.Util;

import java.util.List;
import java.util.Optional;

/**
 * Represents an Trigger Binding in Azure Functions.
 *
 * @since 2.0.0
 */
public abstract class TriggerBinding extends Binding {
    protected ServiceDeclarationNode serviceDeclarationNode;
    protected SemanticModel semanticModel;
    
    public TriggerBinding(String triggerType) {
        super(triggerType, Constants.DIRECTION_IN);
    }
    
    public abstract List<FunctionContext> getBindings();

    public Optional<AnnotationNode> getListenerAnnotation(ServiceDeclarationNode svcDeclNode, String annotationName) {
        //TODO handle inline decl
        for (ExpressionNode expression : svcDeclNode.expressions()) {
            Optional<Symbol> symbol = this.semanticModel.symbol(expression);
            if (symbol.isEmpty()) {
                continue;
            }
            Symbol listenerSymbol = symbol.get();
            if (listenerSymbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            VariableSymbol variableSymbol = (VariableSymbol) listenerSymbol;
            ListenerDeclarationNode listenerDeclarationNode =
                    (ListenerDeclarationNode) Util.findNode(svcDeclNode, variableSymbol);
            Optional<MetadataNode> metadata = listenerDeclarationNode.metadata();
            if (metadata.isEmpty()) {
                continue;
            }
            NodeList<AnnotationNode> annotations = metadata.get().annotations();
            for (AnnotationNode annotationNode : annotations) {
                Optional<Symbol> typeSymbol = this.semanticModel.symbol(annotationNode);
                if (typeSymbol.isEmpty()) {
                    continue;
                }
                Symbol annotationType = typeSymbol.get();
                Optional<String> name = annotationType.getName();
                if (name.isEmpty()) {
                    continue;
                }
                if (name.get().equals(annotationName)) {
                    return Optional.of(annotationNode);
                }
            }

//            List<AnnotationSymbol> annotations = variableSymbol.annotations();
//            for (AnnotationSymbol annotationSymbol : annotations) {
//                Optional<String> name = annotationSymbol.getName();
//                if (name.isEmpty()) {
//                    continue;
//                }
//                if (name.get().equals(annotationName)) {
//                    return Optional.of(annotationSymbol);
//                }
//            }
        }

        return Optional.empty();
    }

    protected boolean isPayloadAnnotationExist(NodeList<AnnotationNode> nodes) {
        for (AnnotationNode annotation : nodes) {
            Node annotRef = annotation.annotReference();
            if (annotRef.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                QualifiedNameReferenceNode annotationRef = (QualifiedNameReferenceNode) annotRef;
                if (annotationRef.identifier().text().equals("Payload")) { //Add other stuff
                    return true;
                }
            }
        }
        return false;
    }
}
