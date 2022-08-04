package org.ballerinax.azurefunctions.service.blob;

import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.service.OutputBinding;

import java.util.Optional;

/**
 * Represents Queue output binding in functions.json.
 *
 * @since 2.0.0
 */
public class BlobOutputBinding extends OutputBinding {

    private String path;
    private String connection = "AzureWebJobsStorage";
    private String dataType = "binary";

    public BlobOutputBinding(AnnotationNode annotationNode) {
        super("blob");
        this.setVarName("outMsg");
        SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().orElseThrow().fields();
        for (MappingFieldNode fieldNode : fields) {
            extractValueFromAnnotation((SpecificFieldNode) fieldNode);
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    private void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
        String text = ((IdentifierToken) fieldNode.fieldName()).text();
        Optional<String> value = Util.extractValueFromAnnotationField(fieldNode);
        switch (text) {
            case "path":
                value.ifPresent(this::setPath);
                break;
            case "connection":
                value.ifPresent(this::setConnection);
                break;
            case "dataType":
                value.ifPresent(this::setDataType);
                break;
            default:
                throw new RuntimeException("Unexpected property in the annotation");
        }
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        output.addProperty("path", this.path);
        output.addProperty("connection", this.connection);
        output.addProperty("dataType", this.dataType);
        return output;
    }
}
