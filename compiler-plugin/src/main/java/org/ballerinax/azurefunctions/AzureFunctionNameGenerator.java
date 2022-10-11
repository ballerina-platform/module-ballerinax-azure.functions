package org.ballerinax.azurefunctions;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for generating Azure function name for each resource function.
 * 
 * @since 2.0.0
 */
public class AzureFunctionNameGenerator {

    private List<String> functionNames = new ArrayList<>();
    private List<String> generatedNames = new ArrayList<>();

    public AzureFunctionNameGenerator(ServiceDeclarationNode serviceDeclarationNode) {
        NodeList<Node> members = serviceDeclarationNode.members();
        String servicePath = Util.resourcePathToString(serviceDeclarationNode.absoluteResourcePath());
        for (Node node : members) {
            if (node.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                continue;
            }
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String functionName = getFunctionName(servicePath, functionDefinitionNode);
            this.functionNames.add(functionName);
        }
    }

    private String getFunctionName(String servicePath, FunctionDefinitionNode functionDefinitionNode) {
        String method = functionDefinitionNode.functionName().text();
        StringBuilder resourcePath = new StringBuilder();
        servicePath = servicePath.replace("\\", "");
        resourcePath.append(servicePath);
        for (Node pathBlock : functionDefinitionNode.relativeResourcePath()) {
            if (pathBlock.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                String specialCharReplacedPathBlock = (((IdentifierToken) pathBlock).text()).replace("\\", "");
                resourcePath.append("/").append(specialCharReplacedPathBlock);
            } else if (pathBlock.kind() == SyntaxKind.RESOURCE_PATH_SEGMENT_PARAM) {
                //TODO changed
                Token token = ((ResourcePathParameterNode) pathBlock).paramName().get();
                resourcePath.append("/").append(token.text());
            }
        }
        String functionName = resourcePath.toString().replace("/", "-");
        if (servicePath.equals("")) {
            return method + functionName;
        }
        return method + "-" + functionName;
    }

    public String getUniqueFunctionName(String servicePath, FunctionDefinitionNode functionDefinitionNode) {
        String functionName = getFunctionName(servicePath, functionDefinitionNode);
        functionName = generateUniqueName(functionName, 0);
        generatedNames.add(functionName);
        return functionName;
    }

    private String generateUniqueName(String initialName, int index) {
        String newName;
        if (index == 0) {
            newName = initialName;
        } else {
            newName = initialName + "-" + index;
        }

        if (!isDuplicateName(newName)) {
            return newName;
        }
        return generateUniqueName(initialName, index + 1);
    }

    private boolean isDuplicateName(String initialName) {
        int count = 0;
        for (String functionName : this.functionNames) {
            if (functionName.equals(initialName)) {
                count++;
            }
        }
        return generatedNames.contains(initialName) || count > 1;
    }
}
