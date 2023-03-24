/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_MODULE_NAME;
import static org.ballerinax.azurefunctions.Constants.AZURE_FUNCTIONS_PACKAGE_ORG;
import static org.ballerinax.azurefunctions.Constants.HTTP;

/**
 * Contains the utilities required for the compiler extension.
 *
 * @since 2.0.0
 */
public class Util {

    public static Optional<String> extractValueFromAnnotationField(SpecificFieldNode fieldNode) {

        Optional<ExpressionNode> expressionNode = fieldNode.valueExpr();
        if (expressionNode.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expressionNode1 = expressionNode.get();
        if (expressionNode1.kind() == SyntaxKind.STRING_LITERAL) {
            String text1 = ((BasicLiteralNode) expressionNode1).literalToken().text();
            return Optional.of(text1.substring(1, text1.length() - 1));
        } else if (expressionNode1.kind() == SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN) {
            String text1 = ((BasicLiteralNode) expressionNode1).literalToken().text();
            return Optional.of(text1);
        }
        return Optional.empty();
    }

    /**
     * Find node of this symbol.
     *
     * @param symbol {@link Symbol}
     * @return {@link NonTerminalNode}
     */
    public static NonTerminalNode findNode(ServiceDeclarationNode serviceDeclarationNode, Symbol symbol) {

        if (symbol.getLocation().isEmpty()) {
            return null;
        }
        SyntaxTree syntaxTree = serviceDeclarationNode.syntaxTree();
        TextDocument textDocument = syntaxTree.textDocument();
        LineRange symbolRange = symbol.getLocation().get().lineRange();
        int start = textDocument.textPositionFrom(symbolRange.startLine());
        int end = textDocument.textPositionFrom(symbolRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }

    public static String resourcePathToString(NodeList<Node> nodes) {

        StringBuilder out = new StringBuilder();
        for (Node node : nodes) {
            if (node.kind() == SyntaxKind.STRING_LITERAL) {
                String value = ((BasicLiteralNode) node).literalToken().text();
                out.append(value, 1, value.length() - 1);
            } else if (node.kind() == SyntaxKind.SLASH_TOKEN) {
                Token token = (Token) node;
                out.append(token.text());
            } else if (node.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                out.append(((IdentifierToken) node).text());
            }
        }
        String finalPath = out.toString();
        if (finalPath.startsWith("/")) {
            return finalPath.substring(1);
        }
        return finalPath;
    }

    public static Diagnostic getDiagnostic(Location location, AzureDiagnosticCodes diagnosticCode, Object... argName) {

        DiagnosticInfo diagnosticInfo = getDiagnosticInfo(diagnosticCode, argName);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
    }

    public static void updateDiagnostic(SyntaxNodeAnalysisContext ctx, Location location,
                                        AzureDiagnosticCodes httpDiagnosticCodes) {

        DiagnosticInfo diagnosticInfo = getDiagnosticInfo(httpDiagnosticCodes);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, location));
    }

    public static void updateDiagnostic(SyntaxNodeAnalysisContext ctx, Location location,
                                        AzureDiagnosticCodes azureDiagnosticCodes, Object... argName) {

        DiagnosticInfo diagnosticInfo = getDiagnosticInfo(azureDiagnosticCodes, argName);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, location));
    }

    public static void updateDiagnostic(SyntaxNodeAnalysisContext ctx, Location location,
                                        AzureDiagnosticCodes azureDiagnosticCodes,
                                        List<DiagnosticProperty<?>> diagnosticProperties, String argName) {

        DiagnosticInfo diagnosticInfo = getDiagnosticInfo(azureDiagnosticCodes, argName);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, location, diagnosticProperties));
    }

    public static DiagnosticInfo getDiagnosticInfo(AzureDiagnosticCodes diagnostic, Object... args) {

        return new DiagnosticInfo(diagnostic.getCode(), String.format(diagnostic.getMessage(), args),
                diagnostic.getSeverity());
    }

    public static boolean isSymbolAzureFunctions(Symbol definition) {

        Optional<ModuleSymbol> definitionModule = definition.getModule();
        if (definitionModule.isEmpty()) {
            return false;
        }
        ModuleID moduleID = definitionModule.get().id();
        return moduleID.orgName().equals(Constants.AZURE_FUNCTIONS_PACKAGE_ORG) &&
                moduleID.packageName().equals(Constants.AZURE_FUNCTIONS_MODULE_NAME);
    }

    public static void copyFolder(Path source, Path target, CopyOption... options)
            throws IOException {

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {

                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                Files.copy(file, target.resolve(source.relativize(file)), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteDirectory(Path azureFunctionsDir) throws IOException {

        if (azureFunctionsDir.toFile().exists()) {
            Files.walk(azureFunctionsDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public static String getExecutableExtension() {

        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return ".exe";
        } else {
            return "";
        }
    }

    public static boolean isRemoteFunction(FunctionSymbol methodSymbol) {

        for (Qualifier qualifier : methodSymbol.qualifiers()) {
            if (qualifier.getValue().equals(Constants.REMOTE_KEYWORD)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnalyzableFunction(Node member) {

        if (member.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
            FunctionDefinitionNode node = (FunctionDefinitionNode) member;
            NodeList<Token> tokens = node.qualifierList();
            if (tokens.isEmpty()) {
                // Object methods are allowed.
                return false;
            }
            return tokens.stream().anyMatch(token -> token.text().equals(Constants.REMOTE_KEYWORD));
        } else {
            return member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
        }
    }

    public static boolean isAzureFunctionsService(SemanticModel semanticModel, ServiceDeclarationNode serviceNode) {

        List<Diagnostic> diagnostics = semanticModel.diagnostics();
        boolean erroneousCompilation = diagnostics.stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        if (erroneousCompilation) {
            return false;
        }

        Optional<Symbol> serviceSymOptional = semanticModel.symbol(serviceNode);

        if (serviceSymOptional.isEmpty()) {
            return false;
        }
        List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceSymOptional.get()).listenerTypes();
        for (TypeSymbol typeSymbol : listenerTypes) {
            if (isListenerBelongsToAzureFuncModule(typeSymbol)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isListenerBelongsToAzureFuncModule(TypeSymbol listenerType) {

        if (TypeDescKind.UNION == listenerType.typeKind()) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isAzureFuncModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (TypeDescKind.TYPE_REFERENCE == listenerType.typeKind()) {
            return isAzureFuncModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private static boolean isAzureFuncModule(ModuleSymbol moduleSymbol) {

        return AZURE_FUNCTIONS_MODULE_NAME.equals(moduleSymbol.getName().get()) &&
                AZURE_FUNCTIONS_PACKAGE_ORG.equals(moduleSymbol.id().orgName());
    }

    public static String getCloudBuildOption(Project project) {

        String cloud = project.buildOptions().cloud();
        if (cloud == null || cloud.isEmpty()) {
            return Constants.AZURE_FUNCTIONS_BUILD_OPTION;
        }
        return cloud;
    }

    public static Path getTargetDir(Project project, Path jarPath) {

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return jarPath.getParent();
        }
        return project.targetDir();
    }

    public static Path getProjectDir(Project project, Path jarPath) {

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return jarPath.getParent();
        }
        return project.targetDir().getParent();
    }

    public static Path getAzureFunctionsDir(Project project, Path jarPath) {

        return getTargetDir(project, jarPath).resolve(Constants.FUNCTION_DIRECTORY);
    }

    public static String getAzureFunctionsRelative(Project project) {

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return Constants.FUNCTION_DIRECTORY;
        }
        return Constants.TARGET_DIRECTORY + Constants.FUNCTION_DIRECTORY;
    }

    public static boolean diagnosticContainsErrors(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {

        List<Diagnostic> diagnostics = syntaxNodeAnalysisContext.semanticModel().diagnostics();
        return diagnostics.stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
    }

    public static ServiceDeclarationNode getServiceDeclarationNode(SyntaxNodeAnalysisContext context) {

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> serviceSymOptional = context.semanticModel().symbol(serviceDeclarationNode);
        if (serviceSymOptional.isPresent()) {
            List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceSymOptional.get()).listenerTypes();
            if (listenerTypes.stream().noneMatch(Util::isListenerBelongsToAzfModule)) {
                return null;
            }
        }
        return serviceDeclarationNode;
    }

    private static boolean isListenerBelongsToAzfModule(TypeSymbol listenerType) {

        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isAzfModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isAzfModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private static boolean isAzfModule(ModuleSymbol moduleSymbol) {

        return Constants.AZURE_FUNCTIONS_MODULE_NAME.equals(moduleSymbol.getName().get())
                && Constants.AZURE_FUNCTIONS_PACKAGE_ORG.equals(moduleSymbol.id().orgName());
    }

    public static TypeDescKind getReferencedTypeDescKind(TypeSymbol typeSymbol) {

        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.TYPE_REFERENCE) {
            TypeSymbol typeDescriptor = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            kind = getReferencedTypeDescKind(typeDescriptor);
        }
        return kind;
    }

    public static TypeSymbol getEffectiveTypeFromReadonlyIntersection(IntersectionTypeSymbol intersectionTypeSymbol) {

        List<TypeSymbol> effectiveTypes = new ArrayList<>();
        for (TypeSymbol typeSymbol : intersectionTypeSymbol.memberTypeDescriptors()) {
            if (typeSymbol.typeKind() == TypeDescKind.READONLY) {
                continue;
            }
            effectiveTypes.add(typeSymbol);
        }
        if (effectiveTypes.size() == 1) {
            return effectiveTypes.get(0);
        }
        return null;
    }

    public static Map<String, TypeSymbol> getCtxTypes(SyntaxNodeAnalysisContext ctx) {
        Map<String, TypeSymbol> typeSymbols = new HashMap<>();
        populateBasicTypes(ctx, typeSymbols);
        populateHttpModuleTypes(ctx, typeSymbols);
        return typeSymbols;
    }

    private static void populateHttpModuleTypes(SyntaxNodeAnalysisContext ctx, Map<String, TypeSymbol> typeSymbols) {
        String[] requiredTypeNames = {Constants.RESOURCE_RETURN_TYPE, Constants.HEADER_OBJ_NAME};
        Optional<Map<String, Symbol>> optionalMap = ctx.semanticModel().types().typesInModule(Constants.BALLERINA_ORG,
                HTTP, Constants.EMPTY);
        if (optionalMap.isPresent()) {
            Map<String, Symbol> symbolMap = optionalMap.get();
            for (String typeName : requiredTypeNames) {
                Symbol symbol = symbolMap.get(typeName);
                if (symbol instanceof TypeSymbol) {
                    typeSymbols.put(typeName, (TypeSymbol) symbol);
                } else if (symbol instanceof TypeDefinitionSymbol) {
                    typeSymbols.put(typeName, ((TypeDefinitionSymbol) symbol).typeDescriptor());
                }
            }
        }
    }

    private static void populateBasicTypes(SyntaxNodeAnalysisContext ctx, Map<String, TypeSymbol> typeSymbols) {
        Types types = ctx.semanticModel().types();
        typeSymbols.put(Constants.ANYDATA, types.ANYDATA);
        typeSymbols.put(Constants.JSON, types.JSON);
        typeSymbols.put(Constants.ERROR, types.ERROR);
        typeSymbols.put(Constants.STRING, types.STRING);
        typeSymbols.put(Constants.BOOLEAN, types.BOOLEAN);
        typeSymbols.put(Constants.INT, types.INT);
        typeSymbols.put(Constants.FLOAT, types.FLOAT);
        typeSymbols.put(Constants.DECIMAL, types.DECIMAL);
        typeSymbols.put(Constants.NIL, types.NIL);
        typeSymbols.put(Constants.STRING_ARRAY, types.builder().ARRAY_TYPE.withType(types.STRING).build());
        typeSymbols.put(Constants.BOOLEAN_ARRAY, types.builder().ARRAY_TYPE.withType(types.BOOLEAN).build());
        typeSymbols.put(Constants.INT_ARRAY, types.builder().ARRAY_TYPE.withType(types.INT).build());
        typeSymbols.put(Constants.FLOAT_ARRAY, types.builder().ARRAY_TYPE.withType(types.FLOAT).build());
        typeSymbols.put(Constants.DECIMAL_ARRAY, types.builder().ARRAY_TYPE.withType(types.DECIMAL).build());
        typeSymbols.put(Constants.OBJECT, types.builder().OBJECT_TYPE.build());
        typeSymbols.put(Constants.MAP_OF_JSON, types.builder().MAP_TYPE.withTypeParam(types.JSON).build());
        typeSymbols.put(Constants.ARRAY_OF_MAP_OF_JSON, types.builder().ARRAY_TYPE.withType(
                types.builder().MAP_TYPE.withTypeParam(types.JSON).build()).build());
    }
}
