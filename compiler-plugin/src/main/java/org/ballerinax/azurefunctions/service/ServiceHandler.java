package org.ballerinax.azurefunctions.service;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import org.ballerinax.azurefunctions.service.blob.BlobTriggerBinding;
import org.ballerinax.azurefunctions.service.cosmosdb.CosmosDBTriggerBinding;
import org.ballerinax.azurefunctions.service.http.HTTPTriggerBinding;
import org.ballerinax.azurefunctions.service.queue.QueueTriggerBinding;
import org.ballerinax.azurefunctions.service.timer.TimerTriggerBinding;

import java.util.Optional;

/**
 * Represents the base handler for each azure service.
 *
 * @since 2.0.0
 */
public abstract class ServiceHandler {

    public static TriggerBinding getBuilder(ServiceDeclarationNode svcDeclarationNode, SemanticModel semanticModel) {
        SeparatedNodeList<ExpressionNode> expressions = svcDeclarationNode.expressions();
        for (ExpressionNode expressionNode : expressions) {
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
            if (typeSymbol.isEmpty()) {
                continue;
            }
            TypeReferenceTypeSymbol typeSymbol1;
            if (typeSymbol.get().typeKind() == TypeDescKind.UNION) {
                UnionTypeSymbol union = (UnionTypeSymbol) typeSymbol.get();
                typeSymbol1 = (TypeReferenceTypeSymbol) union.memberTypeDescriptors().get(0);

            } else {
                typeSymbol1 = (TypeReferenceTypeSymbol) typeSymbol.get();
            }
            Optional<String> name = typeSymbol1.definition().getName();
            if (name.isEmpty()) {
                continue;
            }

            String serviceTypeName = name.get();
            switch (serviceTypeName) {
                case "HttpListener":
                    return new HTTPTriggerBinding(svcDeclarationNode, semanticModel);
                case "QueueListener":
                    return new QueueTriggerBinding(svcDeclarationNode, semanticModel);
                case "CosmosDBListener":
                    return new CosmosDBTriggerBinding(svcDeclarationNode, semanticModel);
                case "TimerListener":
                    return new TimerTriggerBinding(svcDeclarationNode, semanticModel);
                case "BlobListener": 
                    return new BlobTriggerBinding(svcDeclarationNode, semanticModel);
                default:
                    //TODO Change
                    throw new RuntimeException("Unsupported Listener type");
            }
        }
        //TODO Change
        throw new RuntimeException("Unsupported Listener type");
    }

//    protected boolean isPayloadAnnotationExist(NodeList<AnnotationNode> nodes) {
//        for (AnnotationNode annotation : nodes) {
//            Node annotRef = annotation.annotReference();
//            if (annotRef.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
//                QualifiedNameReferenceNode annotationRef = (QualifiedNameReferenceNode) annotRef;
//                if (annotationRef.identifier().text().equals("Payload")) { //Add other stuff
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//    public Optional<Binding> getReturnBinding(ReturnTypeDescriptorNode returnTypeDescriptorNode) {
//        NodeList<AnnotationNode> annotations = returnTypeDescriptorNode.annotations();
//        for (AnnotationNode annotationNode : annotations) {
//            Node node = annotationNode.annotReference();
//            if (node.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
//                continue;
//            }
//            QualifiedNameReferenceNode qualifiedNameReferenceNode = (QualifiedNameReferenceNode) node;
//            String annotationName = qualifiedNameReferenceNode.identifier().text();
//            switch (annotationName) {
//                case "QueueOutput":
//                    MappingFieldNode mappingFieldNode = annotationNode.annotValue().orElseThrow().fields().get(0);
//                    if (mappingFieldNode.kind() == SyntaxKind.SPECIFIC_FIELD) {
//                        SpecificFieldNode specificFieldNode = (SpecificFieldNode) mappingFieldNode;
//                        Optional<String> value = Util.extractValueFromAnnotationField(specificFieldNode);
//                        if (value.isPresent()) {
//                            String text = ((IdentifierToken) specificFieldNode.fieldName()).text();
//                            if (text.equals("queueName")) {
//                                return Optional.of(new QueueOutputBinding(value.get()));
//                            }
//                        }
//                    }
//                    break;
//                case "HttpOutput":
//                    return Optional.of(new HTTPOutputBinding());
//                case "CosmosDBOutput":
//                    CosmosDBOutputBinding cosmosDBOutputBinding = new CosmosDBOutputBinding();
//                    SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().orElseThrow().fields();
//                    for (MappingFieldNode mappingField : fields) {
//                        if (mappingField.kind() == SyntaxKind.SPECIFIC_FIELD) {
//                            SpecificFieldNode specificFieldNode = (SpecificFieldNode) mappingField;
//                            Optional<String> value = Util.extractValueFromAnnotationField(specificFieldNode);
//                            if (value.isPresent()) {
//                                String text = ((IdentifierToken) specificFieldNode.fieldName()).text();
//                                switch (text) {
//                                    case "connectionStringSetting":
//                                        cosmosDBOutputBinding.setConnectionStringSetting(value.get());
//                                        break;
//                                    case "databaseName":
//                                        cosmosDBOutputBinding.setDatabaseName(value.get());
//                                        break;
//                                    case "collectionName":
//                                        cosmosDBOutputBinding.setCollectionName(value.get());
//                                        break;
//                                    default:
//                                        throw new RuntimeException("Unexpected property in the annotation");
//                                }
//                            }
//                        }
//                    }
//                    return Optional.of(cosmosDBOutputBinding);
//            }
//        }
//        return Optional.empty();
//    }
 
//    public Optional<AnnotationNode> getListenerAnnotation(ServiceDeclarationNode svcDeclNode, String annotationName) {
//        //TODO handle inline decl
//        for (ExpressionNode expression : svcDeclNode.expressions()) {
//            Optional<Symbol> symbol = this.semanticModel.symbol(expression);
//            if (symbol.isEmpty()) {
//                continue;
//            }
//            Symbol listenerSymbol = symbol.get();
//            if (listenerSymbol.kind() != SymbolKind.VARIABLE) {
//                continue;
//            }
//            VariableSymbol variableSymbol = (VariableSymbol) listenerSymbol;
//            ListenerDeclarationNode listenerDeclarationNode = (ListenerDeclarationNode) findNode(variableSymbol);
//            Optional<MetadataNode> metadata = listenerDeclarationNode.metadata();
//            if (metadata.isEmpty()) {
//                continue;
//            }
//            NodeList<AnnotationNode> annotations = metadata.get().annotations();
//            for (AnnotationNode annotationNode : annotations) {
//                Optional<Symbol> typeSymbol = this.semanticModel.symbol(annotationNode);
//                if (typeSymbol.isEmpty()) {
//                    continue;
//                }
//                Symbol annotationType = typeSymbol.get();
//                Optional<String> name = annotationType.getName();
//                if (name.isEmpty()) {
//                    continue;
//                }
//                if (name.get().equals(annotationName)) {
//                    return Optional.of(annotationNode);
//                }
//            }
//
////            List<AnnotationSymbol> annotations = variableSymbol.annotations();
////            for (AnnotationSymbol annotationSymbol : annotations) {
////                Optional<String> name = annotationSymbol.getName();
////                if (name.isEmpty()) {
////                    continue;
////                }
////                if (name.get().equals(annotationName)) {
////                    return Optional.of(annotationSymbol);
////                }
////            }
//        }
//
//        return Optional.empty();
//    }
}
