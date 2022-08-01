package org.ballerinax.azurefunctions;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Contains the utilities required for the compiler extension.
 *
 * @since 2.0.0
 */
public class Util {

    public static Optional<String> extractValueFromAnnotationField(SpecificFieldNode fieldNode) {
        Optional<ExpressionNode> expressionNode = fieldNode.valueExpr();
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
                BasicLiteralNode basicLiteralNode = (BasicLiteralNode) node;
                out.append(basicLiteralNode.literalToken().text());
            }
        }
        return out.substring(1, out.toString().length() - 1);
    }

    public static void unzipFolder(Path source, Path target) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

            // list files in zip
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {

                boolean isDirectory = false;
                // example 1.1
                // some zip stored files and folders separately
                // e.g data/
                //     data/folder/
                //     data/folder/file.txt
                if (zipEntry.getName().endsWith(File.separator)) {
                    isDirectory = true;
                }

                Path newPath = zipSlipProtect(zipEntry, target);

                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {

                    // example 1.2
                    // some zip stored file path only, need create parent directories
                    // e.g data/folder/file.txt
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // copy files, nio
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy files, classic
                    /*try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }*/
                }

                zipEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        }

    }

    // protect zip slip attack
    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
            throws IOException {

        // test zip slip vulnerability
        // Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // make sure normalized file still has targetDir as its prefix
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }

        return normalizePath;
    }

}
