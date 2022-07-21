package org.ballerinax.azurefunctions.service.blob;

import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.service.InputBinding;

import java.util.Optional;

/**
 * Represents CosmosDB Binding in functions.json.
 */
public class BlobInputBinding extends InputBinding {

    private String path;
    private String connection = "AzureWebJobsStorage";
    private String dataType = "binary";

    public BlobInputBinding(AnnotationNode queueTrigger, String varName) {
        super("blob");
        this.setVarName(varName);
        SeparatedNodeList<MappingFieldNode> fields = queueTrigger.annotValue().orElseThrow().fields();
        for (MappingFieldNode fieldNode : fields) {
            extractValueFromAnnotation((SpecificFieldNode) fieldNode);
        }
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

    @Override
    public JsonObject getJsonObject() {
        JsonObject inputTrigger = new JsonObject();
        inputTrigger.addProperty("type", this.getTriggerType());
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("name", this.getVarName());
        inputTrigger.addProperty("path", this.path);
        inputTrigger.addProperty("connection", this.connection);
        inputTrigger.addProperty("dataType", this.dataType);
        return inputTrigger;
    }
}
