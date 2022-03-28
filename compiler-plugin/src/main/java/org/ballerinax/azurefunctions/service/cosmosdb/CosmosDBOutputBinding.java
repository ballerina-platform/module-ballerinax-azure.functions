package org.ballerinax.azurefunctions.service.cosmosdb;

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
public class CosmosDBOutputBinding extends OutputBinding {

    private String connectionStringSetting;
    private String databaseName;
    private String collectionName;

    public CosmosDBOutputBinding(AnnotationNode annotationNode) {
        super("cosmosDB");
        this.setVarName("outMsg");
        SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().orElseThrow().fields();
        for (MappingFieldNode fieldNode : fields) {
            extractValueFromAnnotation((SpecificFieldNode) fieldNode);
        }
    }

    private void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
        String text = ((IdentifierToken) fieldNode.fieldName()).text();
        Optional<String> value = Util.extractValueFromAnnotationField(fieldNode);
        switch (text) {
            case "connectionStringSetting":
                value.ifPresent(this::setConnectionStringSetting);
                break;
            case "databaseName":
                value.ifPresent(this::setDatabaseName);
                break;
            case "collectionName":
                value.ifPresent(this::setCollectionName);
                break;
            default:
                throw new RuntimeException("Unexpected property in the annotation");
        }
    }

    public String getConnectionStringSetting() {
        return connectionStringSetting;
    }

    public void setConnectionStringSetting(String connectionStringSetting) {
        this.connectionStringSetting = connectionStringSetting;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("connectionStringSetting", this.connectionStringSetting);
        output.addProperty("databaseName", this.databaseName);
        output.addProperty("collectionName", this.collectionName);
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        return output;
    }
}
