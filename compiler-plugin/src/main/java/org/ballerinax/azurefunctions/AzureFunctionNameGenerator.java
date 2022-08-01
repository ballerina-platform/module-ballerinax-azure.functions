package org.ballerinax.azurefunctions;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ballerinax.azurefunctions.Util.resourcePathToString;

public class AzureFunctionNameGenerator {
    private ServiceDeclarationNode serviceDeclarationNode;
    private List<String> functionNames = new ArrayList<>();

    public AzureFunctionNameGenerator(ServiceDeclarationNode serviceDeclarationNode) {
        this.serviceDeclarationNode = serviceDeclarationNode;
        NodeList<Node> members = serviceDeclarationNode.members();
        String servicePath = resourcePathToString(serviceDeclarationNode.absoluteResourcePath());
        for (Node node : members) {
            if (node.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                continue;
            }
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String method = functionDefinitionNode.functionName().text();
            StringBuilder resourcePath = new StringBuilder();
            resourcePath.append(servicePath);
            for (Node pathBlock : functionDefinitionNode.relativeResourcePath()) {
                if (pathBlock.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                    resourcePath.append("/").append(((IdentifierToken) pathBlock).text());
                    continue;
                }
            }
            String functionName = method + "-" + resourcePath.toString().replace("/", "-");
            this.functionNames.add(functionName);
        }
    }
    
    
    
    public String getUniqueFunctionName(String servicePath, FunctionDefinitionNode functionDefinitionNode) {
        int index = 0;
        String method = functionDefinitionNode.functionName().text();
        StringBuilder resourcePath = new StringBuilder();
        resourcePath.append(servicePath);
        for (Node pathBlock : functionDefinitionNode.relativeResourcePath()) {
            if (pathBlock.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                resourcePath.append("/" + ((IdentifierToken) pathBlock).text());
                continue;
            }
            if (pathBlock.kind() == SyntaxKind.RESOURCE_PATH_SEGMENT_PARAM) {
                resourcePath.append("/" + index);
//                    resourcePath.append("{").append(pathParamNode.paramName().text()).append("}");
                continue;
            }
        }
        String functionName = method + "-" + resourcePath.toString().replace("/", "-");
    }
    
}
