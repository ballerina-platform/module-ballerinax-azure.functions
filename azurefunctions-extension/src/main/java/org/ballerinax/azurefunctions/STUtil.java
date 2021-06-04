/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TrapExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility functions for Azure Functions.
 *
 * @since 2.0.0
 */
public class STUtil {

    /**
     * Generates boilerplate Handler function for a specific azure function.
     *
     * @param baseName function name
     * @return boilerplate handler function
     */
    public static FunctionDefinitionNode createHandlerFunction(String baseName) {
        QualifiedNameReferenceNode azHandlerParamsType =
                NodeFactory
                        .createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS),
                                NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                                NodeFactory
                                        .createIdentifierToken("HandlerParams", NodeFactory.createEmptyMinutiaeList(),
                                                generateMinutiaeListWithWhitespace()));
        RequiredParameterNode requiredParameterNode =
                NodeFactory.createRequiredParameterNode(NodeFactory.createEmptyNodeList(), azHandlerParamsType,
                        NodeFactory.createIdentifierToken(Constants.REQUEST_PARAMS_NAME));
        OptionalTypeDescriptorNode optionalErrorTypeDescriptorNode =
                NodeFactory.createOptionalTypeDescriptorNode(NodeFactory
                                .createErrorTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.ERROR_KEYWORD), null),
                        NodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        ReturnTypeDescriptorNode returnTypeDescriptorNode =
                NodeFactory.createReturnTypeDescriptorNode(NodeFactory
                                .createToken(SyntaxKind.RETURNS_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()),
                        NodeFactory.createEmptyNodeList(), optionalErrorTypeDescriptorNode);
        FunctionSignatureNode functionSignatureNode =
                NodeFactory.createFunctionSignatureNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(requiredParameterNode),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);

        FunctionBodyBlockNode emptyFunctionBodyNode =
                NodeFactory.createFunctionBodyBlockNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()), null,
                        NodeFactory.createEmptyNodeList(), NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        return NodeFactory.createFunctionDefinitionNode(
                SyntaxKind.FUNCTION_DEFINITION, null,
                NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace())),
                NodeFactory.createToken(SyntaxKind.FUNCTION_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()), NodeFactory.createIdentifierToken(baseName +
                        "Handler", NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createEmptyNodeList(), functionSignatureNode, emptyFunctionBodyNode);
    }

    public static FunctionDefinitionNode createResourceFunction(String baseName) {
        QualifiedNameReferenceNode httpCallerType =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.HTTP_IMPORT),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("Caller", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));
        RequiredParameterNode callerParamNode =
                NodeFactory.createRequiredParameterNode(NodeFactory.createEmptyNodeList(), httpCallerType,
                        NodeFactory.createIdentifierToken(Constants.HTTP_CALLER_PARAMS_NAME));

        QualifiedNameReferenceNode httpRequestType =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.HTTP_IMPORT),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("Request", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));
        RequiredParameterNode requestParamNode =
                NodeFactory.createRequiredParameterNode(NodeFactory.createEmptyNodeList(), httpRequestType,
                        NodeFactory.createIdentifierToken(Constants.HTTP_REQUEST_PARAMS_NAME));

        OptionalTypeDescriptorNode optionalErrorTypeDescriptorNode =
                NodeFactory.createOptionalTypeDescriptorNode(NodeFactory
                                .createErrorTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.ERROR_KEYWORD), null),
                        NodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        ReturnTypeDescriptorNode returnTypeDescriptorNode =
                NodeFactory.createReturnTypeDescriptorNode(NodeFactory
                                .createToken(SyntaxKind.RETURNS_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()),
                        NodeFactory.createEmptyNodeList(), optionalErrorTypeDescriptorNode);
        FunctionSignatureNode functionSignatureNode =
                NodeFactory.createFunctionSignatureNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(callerParamNode,
                                NodeFactory.createToken(SyntaxKind.COMMA_TOKEN), requestParamNode),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);

        QualifiedNameReferenceNode httpResponseType =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.HTTP_IMPORT),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("Response", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        CaptureBindingPatternNode response =
                NodeFactory.createCaptureBindingPatternNode(
                        NodeFactory.createIdentifierToken("response", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        TypedBindingPatternNode responseTypeBindingNode =
                NodeFactory.createTypedBindingPatternNode(httpResponseType, response);

        ImplicitNewExpressionNode implicitNewExpressionNode =
                NodeFactory.createImplicitNewExpressionNode(NodeFactory.createToken(SyntaxKind.NEW_KEYWORD), null);

        VariableDeclarationNode responseDeclNode =
                NodeFactory.createVariableDeclarationNode(NodeFactory.createEmptyNodeList(), null,
                        responseTypeBindingNode, NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN),
                        implicitNewExpressionNode,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        QualifiedNameReferenceNode afHandlerParamType =
                NodeFactory
                        .createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS),
                                NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                                NodeFactory.createIdentifierToken(Constants.REQUEST_PARAMS_TYPE,
                                        NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()));

        CaptureBindingPatternNode params =
                NodeFactory.createCaptureBindingPatternNode(
                        NodeFactory.createIdentifierToken(Constants.REQUEST_PARAMS_NAME,
                                NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        TypedBindingPatternNode paramsTypeBindingNode =
                NodeFactory.createTypedBindingPatternNode(afHandlerParamType, params);

        SpecificFieldNode requestParam =
                NodeFactory.createSpecificFieldNode(null, NodeFactory.createIdentifierToken("request"), null, null);

        SpecificFieldNode responseParam =
                NodeFactory.createSpecificFieldNode(null, NodeFactory.createIdentifierToken("response"), null, null);

        MappingConstructorExpressionNode argList =
                NodeFactory.createMappingConstructorExpressionNode(NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                        NodeFactory
                                .createSeparatedNodeList(requestParam, NodeFactory.createToken(SyntaxKind.COMMA_TOKEN),
                                        responseParam), NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        VariableDeclarationNode paramDeclNode =
                NodeFactory.createVariableDeclarationNode(NodeFactory.createEmptyNodeList(), null,
                        paramsTypeBindingNode, NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN), argList,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        PositionalArgumentNode paramsArg = NodeFactory.createPositionalArgumentNode(
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.PARAMS)));
        TrapExpressionNode trappedHandlerCall = NodeFactory.createTrapExpressionNode(SyntaxKind.TRAP_EXPRESSION,
                NodeFactory.createToken(SyntaxKind.TRAP_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(),
                        STUtil.generateMinutiaeListWithWhitespace()), createMethodInvocationNode(
                        baseName + "Handler", paramsArg));
        PositionalArgumentNode handlerArg = NodeFactory.createPositionalArgumentNode(trappedHandlerCall);

        ExpressionStatementNode expressionStatementNode =
                NodeFactory.createExpressionStatementNode(SyntaxKind.CALL_STATEMENT, createAfFunctionInvocationNode(
                        "handleFunctionResposne", false, handlerArg, paramsArg),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        SimpleNameReferenceNode caller =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("caller"));
        SimpleNameReferenceNode respond =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("respond"));
        SimpleNameReferenceNode responseVar =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("response"));
        RemoteMethodCallActionNode remoteMethodCallActionNode = NodeFactory
                .createRemoteMethodCallActionNode(caller, NodeFactory.createToken(SyntaxKind.RIGHT_ARROW_TOKEN),
                        respond, NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(NodeFactory.createPositionalArgumentNode(responseVar)),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
        ExpressionStatementNode checkedResponseExpr =
                NodeFactory.createExpressionStatementNode(SyntaxKind.ACTION_STATEMENT,
                        NodeFactory.createCheckExpressionNode(SyntaxKind.CHECK_ACTION,
                                NodeFactory.createToken(SyntaxKind.CHECK_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                        STUtil.generateMinutiaeListWithWhitespace()), remoteMethodCallActionNode),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        FunctionBodyBlockNode functionBodyNode =
                NodeFactory.createFunctionBodyBlockNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()), null,
                        NodeFactory.createNodeList(responseDeclNode, paramDeclNode, expressionStatementNode,
                                checkedResponseExpr),
                        NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        NodeList<Node> relativeResPath = NodeFactory
                .createNodeList(NodeFactory.createIdentifierToken(baseName, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()));

        return NodeFactory.createFunctionDefinitionNode(
                SyntaxKind.FUNCTION_DEFINITION, null,
                NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.RESOURCE_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace())),
                NodeFactory.createToken(SyntaxKind.FUNCTION_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()), NodeFactory
                        .createIdentifierToken("'default", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()), relativeResPath, functionSignatureNode,
                functionBodyNode);
    }

    /**
     * Generates boilerplate main function definition.
     *
     * @return boilerplate main function definition
     */
    public static FunctionDefinitionNode createMainFunction() {
        OptionalTypeDescriptorNode optionalErrorTypeDescriptorNode =
                NodeFactory.createOptionalTypeDescriptorNode(NodeFactory
                                .createErrorTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.ERROR_KEYWORD), null),
                        NodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        ReturnTypeDescriptorNode returnTypeDescriptorNode =
                NodeFactory.createReturnTypeDescriptorNode(NodeFactory
                                .createToken(SyntaxKind.RETURNS_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()),
                        NodeFactory.createEmptyNodeList(), optionalErrorTypeDescriptorNode);
        FunctionSignatureNode functionSignatureNode =
                NodeFactory.createFunctionSignatureNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);

        FunctionBodyBlockNode emptyFunctionBodyNode =
                NodeFactory.createFunctionBodyBlockNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()), null,
                        NodeFactory.createEmptyNodeList(), NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        return NodeFactory.createFunctionDefinitionNode(
                SyntaxKind.FUNCTION_DEFINITION, null,
                NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace())),
                NodeFactory.createToken(SyntaxKind.FUNCTION_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()),
                NodeFactory.createIdentifierToken(Constants.MAIN_FUNC_NAME,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createEmptyNodeList(), functionSignatureNode, emptyFunctionBodyNode);
    }

    /**
     * Generates top level ballerina document node with imports, typedesc, main method and handler functions.
     *
     * @param functionDeploymentContexts list of contexts containing original and generated function information
     * @param generatedTypeDefinitions   type descriptors that needs to be generated
     * @return module part node
     */
    public static ModulePartNode createModulePartNode(Collection<FunctionDeploymentContext> functionDeploymentContexts,
                                                      Map<String, TypeDefinitionNode> generatedTypeDefinitions) {

        ImportDeclarationNode afImport =
                NodeFactory.createImportDeclarationNode(NodeFactory.createToken(SyntaxKind.IMPORT_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), STUtil.generateMinutiaeListWithWhitespace()),
                        NodeFactory.createImportOrgNameNode(
                                NodeFactory.createIdentifierToken(Constants.AZURE_FUNCTIONS_PACKAGE_ORG),
                                NodeFactory.createToken(SyntaxKind.SLASH_TOKEN)),
                        NodeFactory.createSeparatedNodeList(
                                NodeFactory.createIdentifierToken(Constants.AZURE_FUNCTIONS_MODULE_NAME)),
                        NodeFactory.createImportPrefixNode(NodeFactory.createToken(SyntaxKind.AS_KEYWORD,
                                STUtil.generateMinutiaeListWithWhitespace(),
                                STUtil.generateMinutiaeListWithWhitespace()),
                                NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS)),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()));

        ImportDeclarationNode httpImport =
                NodeFactory.createImportDeclarationNode(NodeFactory.createToken(SyntaxKind.IMPORT_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), STUtil.generateMinutiaeListWithWhitespace()),
                        NodeFactory.createImportOrgNameNode(NodeFactory.createIdentifierToken(Constants.BALLERINA_ORG),
                                NodeFactory.createToken(SyntaxKind.SLASH_TOKEN)),
                        NodeFactory.createSeparatedNodeList(NodeFactory.createIdentifierToken("http")),
                        null, NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()));
        List<ModuleMemberDeclarationNode> memberDeclarationNodeList = new ArrayList<>();
        QualifiedNameReferenceNode httpListener =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken("http"),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("Listener", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));
        QualifiedNameReferenceNode listenerRef =
                NodeFactory
                        .createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS),
                                NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                                NodeFactory.createIdentifierToken("hl", NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()));

        ListenerDeclarationNode listener =
                NodeFactory.createListenerDeclarationNode(null, NodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), STUtil.generateMinutiaeListWithWhitespace()),
                        NodeFactory.createToken(SyntaxKind.LISTENER_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithWhitespace()), httpListener,
                        NodeFactory.createIdentifierToken(
                                "__testListener"), NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN), listenerRef,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
        memberDeclarationNodeList.add(listener);

        memberDeclarationNodeList.addAll(generatedTypeDefinitions.values());

        QualifiedNameReferenceNode httpService =
                NodeFactory.createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken("http"),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken("Service", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        NodeList<Node> absResPathList = NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.SLASH_TOKEN));

        SeparatedNodeList<ExpressionNode> listenerReff = NodeFactory.createSeparatedNodeList(NodeFactory
                .createSimpleNameReferenceNode(
                        NodeFactory.createIdentifierToken("__testListener", NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace())));

        List<Node> resourceFunctions = new ArrayList<>();
        for (FunctionDeploymentContext context : functionDeploymentContexts) {
            resourceFunctions.add(createResourceFunction(context.getSourceFunction().functionName().text()));
            resourceFunctions.add(context.getFunction());
        }

        ServiceDeclarationNode serviceDeclarationNode =
                NodeFactory.createServiceDeclarationNode(null, NodeFactory.createEmptyNodeList(),
                        NodeFactory.createToken(SyntaxKind.SERVICE_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()), httpService, absResPathList,
                        NodeFactory.createToken(SyntaxKind.ON_KEYWORD, generateMinutiaeListWithWhitespace(),
                                generateMinutiaeListWithWhitespace()), listenerReff,
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                        NodeFactory.createNodeList(resourceFunctions),
                        NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        memberDeclarationNodeList.add(serviceDeclarationNode);
        
        NodeList<ModuleMemberDeclarationNode> nodeList = NodeFactory.createNodeList(memberDeclarationNodeList);
        Token eofToken = NodeFactory.createToken(SyntaxKind.EOF_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                STUtil.generateMinutiaeListWithNewline());
        return NodeFactory.createModulePartNode(NodeFactory.createNodeList(afImport, httpImport), nodeList, eofToken);
    }

    /**
     * Generates main function to register handler functions.
     *
     * @param functionDeploymentContexts list of Function deployment contexts
     * @return generated main function
     */
    public static FunctionDefinitionNode createMainFunction(
            Collection<FunctionDeploymentContext> functionDeploymentContexts) {
        FunctionDefinitionNode mainFunction = STUtil.createMainFunction();
        for (FunctionDeploymentContext functionDeploymentContext : functionDeploymentContexts) {
            String functionHandlerName = functionDeploymentContext.getFunction().functionName().text();
            PositionalArgumentNode handler = NodeFactory.createPositionalArgumentNode(
                    NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(functionHandlerName)));
            PositionalArgumentNode functionName =
                    NodeFactory.createPositionalArgumentNode(
                            STUtil.createStringLiteral(
                                    functionDeploymentContext.getSourceFunction().functionName().text()));
            ExpressionNode register = createAfFunctionInvocationNode(Constants.AZURE_FUNCS_REG_FUNCTION_NAME, false,
                    functionName, handler);
            ExpressionStatementNode expressionStatementNode =
                    NodeFactory.createExpressionStatementNode(SyntaxKind.CALL_STATEMENT, register,
                            NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
            mainFunction = addStatementToFunctionBody(expressionStatementNode, mainFunction);
        }
        return mainFunction;
    }

    public static MinutiaeList generateMinutiaeListWithWhitespace() {
        return NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae(" "));
    }

    public static MinutiaeList generateMinutiaeListWithNewline() {
        return NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae("\n"));
    }

    public static boolean isContextType(ParameterSymbol symbol) {
        Optional<ModuleSymbol> module = symbol.typeDescriptor().getModule();
        if (module.isEmpty()) {
            return false;
        }
        ModuleID id = module.get().id();
        if (!(id.orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG) &&
                id.moduleName().equals(Constants.AZURE_FUNCTIONS_MODULE_NAME))) {
            return false;
        }
        Optional<String> name = symbol.typeDescriptor().getName();
        if (name.isEmpty()) {
            return false;
        }
        return name.get().equals(Constants.AZURE_FUNCTIONS_CONTEXT_NAME);
    }

    public static void addFunctionBinding(FunctionDeploymentContext ctx, Map<String, Object> binding) {
        if (binding == null) {
            return;
        }
        JsonArray bindings = (JsonArray) ctx.getFunctionDefinition().get(Constants.FUNCTION_BINDINGS_NAME);
        bindings.add(createBindingObject(binding));
    }

    public static JsonObject createBindingObject(Map<String, Object> binding) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : binding.entrySet()) {
            obj.add(entry.getKey(), objectToJson(entry.getValue()));
        }
        return obj;
    }

    public static JsonElement objectToJson(Object obj) {
        if (obj instanceof String) {
            return new JsonPrimitive((String) obj);
        } else if (obj instanceof Number) {
            return new JsonPrimitive((Number) obj);
        } else if (obj instanceof Boolean) {
            return new JsonPrimitive((Boolean) obj);
        } else if (obj instanceof String[]) {
            JsonArray array = new JsonArray();
            for (String item : (String[]) obj) {
                array.add(item);
            }
            return array;
        } else if (obj == null) {
            return JsonNull.INSTANCE;
        } else {
            throw new IllegalStateException("Unsupported type to convert to JSON: " + obj.getClass());
        }
    }

    //TODO remove when semantic api supports return type annotations
    //https://github.com/ballerina-platform/ballerina-lang/issues/27225
    public static Optional<AnnotationNode> extractAzureFunctionAnnotation(NodeList<AnnotationNode> annons) {
        for (AnnotationNode an : annons) {
            Node node = an.annotReference();
            if (node.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                QualifiedNameReferenceNode refNode = (QualifiedNameReferenceNode) node;
                String text = refNode.modulePrefix().text();
                if (text.equals(Constants.AF_IMPORT_ALIAS) || text.equals(Constants.AZURE_FUNCTIONS_MODULE_NAME)) {
                    return Optional.of(an);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<AnnotationSymbol> extractAzureFunctionAnnotation(List<AnnotationSymbol> annotations) {
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<ModuleSymbol> module = annotationSymbol.getModule();
            if (module.isEmpty()) {
                return Optional.empty();
            }
            ModuleID id = module.get().id();
            if (id.orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG) &&
                    id.moduleName().equals(Constants.AZURE_FUNCTIONS_MODULE_NAME)) {
                return Optional.of(annotationSymbol);
            }
        }
        return Optional.empty();
    }

    public static boolean isAzurePkgType(ParameterSymbol variableSymbol, String name) {
        Optional<ModuleSymbol> module = variableSymbol.typeDescriptor().getModule();
        if (module.isEmpty()) {
            return false;
        }
        ModuleID id = module.get().id();
        if (!(id.orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG) &&
                id.moduleName().equals(Constants.AZURE_FUNCTIONS_MODULE_NAME))) {
            return false;
        }
        Optional<String> name1 = variableSymbol.typeDescriptor().getName();
        if (name1.isEmpty()) {
            return false;
        }
        return name1.get().equals(name);
    }

    public static boolean isStringType(ParameterSymbol variableSymbol) {
        return variableSymbol.typeDescriptor().typeKind() == TypeDescKind.STRING;
    }

    public static boolean isOptionalStringType(ParameterSymbol variableSymbol) {
        if (variableSymbol.typeDescriptor().typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) variableSymbol.typeDescriptor();
            for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
                if (memberTypeDescriptor.typeKind() == TypeDescKind.STRING) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isOptionalByteArrayType(ParameterSymbol variableSymbol) {
        if (variableSymbol.typeDescriptor().typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) variableSymbol.typeDescriptor();
            for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
                if (memberTypeDescriptor.typeKind() == TypeDescKind.ARRAY) {
                    ArrayTypeSymbol arrayTypeDescriptor = (ArrayTypeSymbol) memberTypeDescriptor;
                    if (arrayTypeDescriptor.memberTypeDescriptor().typeKind() == TypeDescKind.BYTE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isByteArrayType(ParameterSymbol variableSymbol) {
        if (variableSymbol.typeDescriptor().typeKind() == TypeDescKind.ARRAY) {
            ArrayTypeSymbol arrayTypeDescriptor = (ArrayTypeSymbol) variableSymbol.typeDescriptor();
            return arrayTypeDescriptor.memberTypeDescriptor().typeKind() == TypeDescKind.BYTE;
        }
        return false;
    }

    public static boolean isOptionalRecordType(ParameterSymbol variableSymbol) {
        if (variableSymbol.typeDescriptor().typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) variableSymbol.typeDescriptor();
            for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
                if (memberTypeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isRecordArrayType(ParameterSymbol variableSymbol) {
        return isRecordArrayType(variableSymbol.typeDescriptor());
    }

    public static boolean isRecordArrayType(TypeSymbol variableSymbol) {
        if (variableSymbol.typeKind() == TypeDescKind.ARRAY) {
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) variableSymbol;
            return arrayTypeSymbol.memberTypeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE;
        }
        return false;
    }

    public static TypeDefinitionNode createArrayTypeDefinitionNode(ArrayTypeDescriptorNode arrayTypeDescriptorNode) {
        String typeName = ((SimpleNameReferenceNode) arrayTypeDescriptorNode.memberTypeDesc()).name().text();
        return NodeFactory.createTypeDefinitionNode(null, null, NodeFactory.createToken(SyntaxKind.TYPE_KEYWORD,
                NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createIdentifierToken(typeName + "ArrayGenerated", NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()), arrayTypeDescriptorNode,
                NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
    }

    public static TypeDefinitionNode createOptionalTypeDefinitionNode(
            OptionalTypeDescriptorNode optionalTypeDescriptorNode) {
        String typeName = ((SimpleNameReferenceNode) optionalTypeDescriptorNode.typeDescriptor()).name().text();
        return NodeFactory.createTypeDefinitionNode(null, null, NodeFactory.createToken(SyntaxKind.TYPE_KEYWORD,
                NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createIdentifierToken(typeName + "OptionalGenerated", NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()),
                optionalTypeDescriptorNode, NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
    }

    public static boolean isJsonType(ParameterSymbol variableSymbol) {
        return variableSymbol.typeDescriptor().typeKind() == TypeDescKind.JSON;
    }

    public static Map<String, Object> extractAnnotationKeyValues(AnnotationNode annotation)
            throws AzureFunctionsException {
        if (annotation.annotValue().isEmpty()) {
            return Collections.emptyMap();
        }
        MappingConstructorExpressionNode mappingConstructorExpressionNode = annotation.annotValue().get();
        Map<String, Object> annonMap = new HashMap<>();
        for (MappingFieldNode field : mappingConstructorExpressionNode.fields()) {
            if (field.kind() != SyntaxKind.SPECIFIC_FIELD) {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(field.location(), "AZ0009",
                        "specific.field.required", DiagnosticSeverity.ERROR, "specific field expected for " +
                                "annotation field"));
            }
            SpecificFieldNode keyValue = (SpecificFieldNode) field;
            Node node = keyValue.fieldName();
            if (node.kind() != SyntaxKind.IDENTIFIER_TOKEN) {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(field.location(), "AZ0010",
                        "identifier.token.expected", DiagnosticSeverity.ERROR, "identifier token expected for key"));
            }
            String key = ((IdentifierToken) node).text();
            if (keyValue.valueExpr().isEmpty()) {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(field.location(), "AZ0011",
                        "annotation.value.expected", DiagnosticSeverity.ERROR, "annotation value expected"));
            }
            ExpressionNode valueExpr = keyValue.valueExpr().get();
            if (valueExpr.kind() != SyntaxKind.STRING_LITERAL) {
                throw new AzureFunctionsException(STUtil.getAFDiagnostic(field.location(), "AZ0011",
                        "unsupported.annotation.value", DiagnosticSeverity.ERROR,
                        "unsupported annotation value " + valueExpr.kind()));
            }
            String value = ((BasicLiteralNode) valueExpr).literalToken().text();
            value = value.substring(1, value.length() - 1);

            annonMap.put(key, value);
        }
        return annonMap;
    }

    public static TypeSymbol getMainParamType(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
                if (!(memberTypeDescriptor.typeKind() == TypeDescKind.ERROR ||
                        memberTypeDescriptor.typeKind() == TypeDescKind.NIL)) {
                    return memberTypeDescriptor;
                }
            }
        }
        return typeSymbol;
    }

    public static SimpleNameReferenceNode addAzurePkgRecordVarDef(FunctionDeploymentContext ctx, String type,
                                                                  String name) {
        QualifiedNameReferenceNode typeDesc =
                NodeFactory
                        .createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS),
                                NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                                NodeFactory.createIdentifierToken(type));
        TypedBindingPatternNode typedBindingPatternNode = NodeFactory.createTypedBindingPatternNode(typeDesc,
                NodeFactory.createCaptureBindingPatternNode(NodeFactory.createIdentifierToken(name,
                        generateMinutiaeListWithWhitespace(), NodeFactory.createEmptyMinutiaeList())));
        MappingConstructorExpressionNode emptyInit =
                NodeFactory.createMappingConstructorExpressionNode(NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                        NodeFactory.createSeparatedNodeList(), NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        VariableDeclarationNode variableDeclarationNode = NodeFactory
                .createVariableDeclarationNode(NodeFactory.createEmptyNodeList(), null, typedBindingPatternNode,
                        NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN), emptyInit,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()));

        FunctionDefinitionNode function = ctx.getFunction();
        ctx.setFunction(addStatementToFunctionBody(variableDeclarationNode, function));
        return NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(name));
    }

    private static FunctionDefinitionNode addStatementToFunctionBody(StatementNode statementNode,
                                                                     FunctionDefinitionNode function) {
        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) function.functionBody();
        NodeList<StatementNode> newBodyStatements = functionBodyBlockNode.statements().add(statementNode);
        FunctionBodyBlockNode newFunctionBodyBlock =
                functionBodyBlockNode.modify().withStatements(newBodyStatements).apply();
        return function.modify().withFunctionBody(newFunctionBodyBlock).apply();
    }

    public static ExpressionNode createFunctionInvocationNode(String functionName, PositionalArgumentNode... args) {
        SimpleNameReferenceNode simpleNameReferenceNode =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(functionName));
        SeparatedNodeList<FunctionArgumentNode> separatedNodeList = getFunctionParamList(args);
        return NodeFactory.createFunctionCallExpressionNode(simpleNameReferenceNode,
                NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN), separatedNodeList,
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
    }

    public static ExpressionNode createMethodInvocationNode(String functionName, PositionalArgumentNode... args) {
        SimpleNameReferenceNode simpleNameReferenceNode =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(functionName));
        SimpleNameReferenceNode selfRef =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken("self"));
        SeparatedNodeList<FunctionArgumentNode> separatedNodeList = getFunctionParamList(args);
        return NodeFactory.createMethodCallExpressionNode(selfRef, NodeFactory.createToken(SyntaxKind.DOT_TOKEN),
                simpleNameReferenceNode, NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN), separatedNodeList,
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
    }

    public static ExpressionNode createAfFunctionInvocationNode(String functionName,
                                                                boolean checked, PositionalArgumentNode... args) {
        QualifiedNameReferenceNode qualifiedNameReferenceNode =
                NodeFactory
                        .createQualifiedNameReferenceNode(NodeFactory.createIdentifierToken(Constants.AF_IMPORT_ALIAS),
                                NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                                NodeFactory.createIdentifierToken(functionName));
        SeparatedNodeList<FunctionArgumentNode> separatedNodeList = getFunctionParamList(args);

        FunctionCallExpressionNode expression =
                NodeFactory.createFunctionCallExpressionNode(qualifiedNameReferenceNode,
                        NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN), separatedNodeList,
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));

        if (checked) {
            return NodeFactory.createCheckExpressionNode(SyntaxKind.CHECK_EXPRESSION,
                    NodeFactory.createToken(SyntaxKind.CHECK_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                            STUtil.generateMinutiaeListWithWhitespace()),
                    expression);
        }
        return expression;
    }

    private static SeparatedNodeList<FunctionArgumentNode> getFunctionParamList(PositionalArgumentNode... args) {
        List<Node> nodeList = new ArrayList<>();
        for (PositionalArgumentNode arg : args) {
            nodeList.add(arg);
            nodeList.add(NodeFactory.createToken(SyntaxKind.COMMA_TOKEN));
        }
        if (args.length > 0) {
            nodeList.remove(nodeList.size() - 1);
        }
        return NodeFactory.createSeparatedNodeList(nodeList);
    }

    /**
     * Calls internal function in Azure Functions module and adds into function body.
     *
     * @param ctx     Function Deployment Context
     * @param name    name of the internal function needs to be called
     * @param checked is checking required
     * @param exprs   parameter arguments
     */
    public static void addAzurePkgFunctionCallStatement(FunctionDeploymentContext ctx, String name,
                                                        boolean checked, PositionalArgumentNode... exprs) {
        NilTypeDescriptorNode nilTypeDescriptorNode =
                NodeFactory.createNilTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithWhitespace()));
        addFunctionCallStatement(nilTypeDescriptorNode, ctx, createAfFunctionInvocationNode(name, false, exprs),
                checked);
    }

    public static SimpleNameReferenceNode createVariableRef(String varName) {
        return NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(varName));
    }

    public static BasicLiteralNode createStringLiteral(String content) {
        return NodeFactory
                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + content + "\"",
                                NodeFactory.createEmptyMinutiaeList(),
                                NodeFactory.createEmptyMinutiaeList()));
    }

    /**
     * Adds Function call statement to the function body.
     *
     * @param typeDescriptorNode type of the left hand side
     * @param ctx                Function Deployment Context
     * @param inv                expression of the right side
     * @param checked            is checking required for the expression
     * @return variable name
     */
    public static String addFunctionCallStatement(TypeDescriptorNode typeDescriptorNode, FunctionDeploymentContext ctx,
                                                  ExpressionNode inv, boolean checked) {
        ExpressionNode expr;
        if (checked) {
            expr = NodeFactory.createCheckExpressionNode(SyntaxKind.CHECK_EXPRESSION,
                    NodeFactory.createToken(SyntaxKind.CHECK_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                            STUtil.generateMinutiaeListWithWhitespace()), inv);
        } else {
            expr = inv;
        }
        String varName = ctx.getNextVarName();
        CaptureBindingPatternNode captureBindingPatternNode =
                NodeFactory.createCaptureBindingPatternNode(NodeFactory.createIdentifierToken(varName,
                        generateMinutiaeListWithWhitespace(), NodeFactory.createEmptyMinutiaeList()));
        TypedBindingPatternNode typedBindingPatternNode =
                NodeFactory.createTypedBindingPatternNode(typeDescriptorNode, captureBindingPatternNode);
        VariableDeclarationNode variableDeclarationNode = NodeFactory
                .createVariableDeclarationNode(NodeFactory.createEmptyNodeList(), null, typedBindingPatternNode,
                        NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN), expr,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                STUtil.generateMinutiaeListWithNewline()));

        ctx.setFunction(addStatementToFunctionBody(variableDeclarationNode, ctx.getFunction()));
        return varName;
    }

    /**
     * Checks if a specific document exists in a module.
     *
     * @param module   module in the project
     * @param document newly added document
     * @return status of the document existence of the module
     */
    public static boolean isDocumentExistInModule(Module module, DocumentConfig document) {
        for (DocumentId documentId : module.documentIds()) {
            Document doc = module.document(documentId);
            if (document.name().equals(doc.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates Azure Function Diagnostic object for unexpected syntax in ballerina documents.
     *
     * @param location location of the node
     * @param code     diagnostic code
     * @param template diagnostic message template
     * @param severity severity of the diagnostic
     * @param message  message if the diagnostic
     * @return Azure Function diagnostic
     */
    public static Diagnostic getAFDiagnostic(Location location, String code,
                                             String template, DiagnosticSeverity severity, String message) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(code, template, severity);
        return new AzureFunctionDiagnostics(location, diagnosticInfo, message);
    }

    public static TypeDescriptorNode getCheckedReturnTypeDescOfOriginalFunction(FunctionDefinitionNode functionNode) {
        Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode =
                functionNode.functionSignature().returnTypeDesc();
        if (returnTypeDescriptorNode.isEmpty()) {
            return NodeFactory.createNilTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                    NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                            STUtil.generateMinutiaeListWithWhitespace()));
        }
        Node returnType = returnTypeDescriptorNode.get().type();
        if (!(returnType instanceof TypeDescriptorNode)) {
            return NodeFactory.createNilTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                    NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                            STUtil.generateMinutiaeListWithWhitespace()));
        }

        return getCheckedTypeDesc((TypeDescriptorNode) returnType);
    }

    public static boolean isCheckingRequiredForOriginalFunction(FunctionDefinitionNode functionDefinitionNode) {
        Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode =
                functionDefinitionNode.functionSignature().returnTypeDesc();
        if (returnTypeDescriptorNode.isEmpty()) {
            return false;
        }
        TypeDescriptorNode typeDescriptorNode =
                (TypeDescriptorNode) returnTypeDescriptorNode.get().type();

        if (typeDescriptorNode.kind() == SyntaxKind.UNION_TYPE_DESC) {
            UnionTypeDescriptorNode unionTypeDescriptorNode = (UnionTypeDescriptorNode) typeDescriptorNode;
            TypeDescriptorNode leftTypeDesc = unionTypeDescriptorNode.leftTypeDesc();
            if (leftTypeDesc.kind() == SyntaxKind.ERROR_TYPE_DESC) {
                return true;
            }
            TypeDescriptorNode rightTypeDesc = unionTypeDescriptorNode.rightTypeDesc();
            return rightTypeDesc.kind() == SyntaxKind.ERROR_TYPE_DESC;
        }
        return typeDescriptorNode.kind() == SyntaxKind.ERROR_TYPE_DESC;
    }

    public static TypeDescriptorNode getCheckedTypeDesc(TypeDescriptorNode typeDescriptorNode) {
        if (typeDescriptorNode.kind() == SyntaxKind.UNION_TYPE_DESC) {
            UnionTypeDescriptorNode unionTypeDescriptorNode = (UnionTypeDescriptorNode) typeDescriptorNode;
            TypeDescriptorNode leftTypeDesc = getCheckedTypeDesc(unionTypeDescriptorNode.leftTypeDesc());
            if (leftTypeDesc.kind() != SyntaxKind.NIL_TYPE_DESC) {
                return leftTypeDesc;
            }
            TypeDescriptorNode rightTypeDesc = getCheckedTypeDesc(unionTypeDescriptorNode.rightTypeDesc());
            if (rightTypeDesc.kind() != SyntaxKind.NIL_TYPE_DESC) {
                return rightTypeDesc;
            }

            return leftTypeDesc;
        }
        if (typeDescriptorNode.kind() == SyntaxKind.ERROR_TYPE_DESC) {
            return NodeFactory.createNilTypeDescriptorNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                    NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                            STUtil.generateMinutiaeListWithWhitespace()));
        }
        return typeDescriptorNode;
    }
}
