package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.LiteralValueToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TreeModifier;

import java.util.Optional;

/**
 * Responsible for generating annotations for each function.
 *
 * @since 2.0.0
 */
public class AzureFunctionModifier extends TreeModifier {

    private SemanticModel semanticModel;
    private UniqueIDHolder holder;
    private String modulePrefix;

    public AzureFunctionModifier(SemanticModel semanticModel) {
        super();
        this.semanticModel = semanticModel;
        this.holder = new UniqueIDHolder();
        this.modulePrefix = "af"; //TODO fixme
    }

    @Override
    public ServiceDeclarationNode transform(ServiceDeclarationNode serviceDeclarationNode) {
        String servicePath = resourcePathToString(serviceDeclarationNode.absoluteResourcePath());
        ExpressionNode listenerExpressionNode = serviceDeclarationNode.expressions().get(0);
        Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(listenerExpressionNode);
        if (typeSymbol.isEmpty()) {
            return super.transform(serviceDeclarationNode);
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
            return super.transform(serviceDeclarationNode);
        }
        NodeList<Node> members = serviceDeclarationNode.members();
        if (!name.get().equals("HTTPListener")) {
            return super.transform(serviceDeclarationNode);
        }
        NodeList<Node> newMembersList = NodeFactory.createNodeList();
        for (Node node : members) {
            Optional<Node> modifiedMember = getModifiedMember(node, servicePath);
            if (modifiedMember.isEmpty()) {
                newMembersList = newMembersList.add(node);
            } else {
                newMembersList = newMembersList.add(modifiedMember.get());
            }
        }
        return new ServiceDeclarationNode.ServiceDeclarationNodeModifier(serviceDeclarationNode)
                .withMembers(newMembersList).apply();
    }

    public Optional<Node> getModifiedMember(Node node, String servicePath) {
        if (node.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            return Optional.empty();
        }
        FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
        String method = functionDefinitionNode.functionName().text();
        StringBuilder resourcePath = new StringBuilder();
        resourcePath.append(servicePath);
        for (Node pathBlock : functionDefinitionNode.relativeResourcePath()) {
            if (pathBlock.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                resourcePath.append("/" + ((IdentifierToken) pathBlock).text());
                continue;
            }
            if (pathBlock.kind() == SyntaxKind.RESOURCE_PATH_SEGMENT_PARAM) {
                resourcePath.append("/" + holder.getNextValue());
//                    resourcePath.append("{").append(pathParamNode.paramName().text()).append("}");
                continue;
            }
        }
        String functionName = method + "-" + resourcePath.toString().replace("/", "-");
        Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
        NodeList<AnnotationNode> existingAnnotations = NodeFactory.createNodeList();
        MetadataNode metadataNode;
        if (metadata.isPresent()) {
            metadataNode = metadata.get();
            existingAnnotations = metadataNode.annotations();
        } else {
            metadataNode = NodeFactory.createMetadataNode(null, existingAnnotations);
        }

        //Create and add annotation

        NodeList<AnnotationNode> modifiedAnnotations = existingAnnotations.add(getFunctionNameAnnotation(functionName));
        MetadataNode modifiedMetadata =
                new MetadataNode.MetadataNodeModifier(metadataNode).withAnnotations(modifiedAnnotations).apply();
        FunctionDefinitionNode updatedFunctionNode =
                new FunctionDefinitionNode.FunctionDefinitionNodeModifier(functionDefinitionNode)
                        .withMetadata(modifiedMetadata).apply();
        return Optional.of(updatedFunctionNode);
    }

    public AnnotationNode getFunctionNameAnnotation(String functionName) {
        QualifiedNameReferenceNode azureFunctionAnnotRef =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(modulePrefix),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("AzureFunction"));
        LiteralValueToken literalValueToken =
                NodeFactory.createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + functionName +
                        "\"", NodeFactory.createEmptyMinutiaeList(), AbstractNodeFactory
                        .createEmptyMinutiaeList());
        BasicLiteralNode basicLiteralNode =
                NodeFactory.createBasicLiteralNode(SyntaxKind.STRING_LITERAL, literalValueToken);
        SpecificFieldNode name = NodeFactory.createSpecificFieldNode(null, NodeFactory.createIdentifierToken("name"),
                NodeFactory.createToken(SyntaxKind.COLON_TOKEN), basicLiteralNode);
        SeparatedNodeList<MappingFieldNode> updatedFields = NodeFactory.createSeparatedNodeList(name);
        MappingConstructorExpressionNode annotationValue =
                NodeFactory.createMappingConstructorExpressionNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN), updatedFields,
                        NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        return NodeFactory.createAnnotationNode(NodeFactory.createToken(SyntaxKind.AT_TOKEN), azureFunctionAnnotRef,
                annotationValue);
    }

    public String resourcePathToString(NodeList<Node> nodes) {
        StringBuilder out = new StringBuilder();
        for (Node node : nodes) {
            if (node.kind() == SyntaxKind.STRING_LITERAL) {
                BasicLiteralNode basicLiteralNode = (BasicLiteralNode) node;
                out.append(basicLiteralNode.literalToken().text());
            }
        }
        return out.substring(1, out.toString().length() - 1);
    }
}