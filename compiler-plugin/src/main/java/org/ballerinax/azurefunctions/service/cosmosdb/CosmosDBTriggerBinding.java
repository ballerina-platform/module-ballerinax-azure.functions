package org.ballerinax.azurefunctions.service.cosmosdb;

import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.service.RemoteTriggerBinding;

import java.util.Optional;

/**
 * Represents a HTTP Trigger binding in functions.json.
 *
 * @since 2.0.0
 */
public class CosmosDBTriggerBinding extends RemoteTriggerBinding {

    private String connectionStringSetting;
    private String databaseName;
    private String collectionName;
    private boolean createLeaseCollectionIfNotExists = true;
    private int leasesCollectionThroughput = 400;

    public CosmosDBTriggerBinding(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel) {
        super("cosmosDBTrigger", "onUpdated", Constants.ANNOTATION_COSMOS_TRIGGER, serviceDeclarationNode,
                semanticModel);
    }

    @Override
    protected void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
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
            case "createLeaseCollectionIfNotExists":
                value.ifPresent(s -> this.setCreateLeaseCollectionIfNotExists(Boolean.parseBoolean(s)));
                break;
            case "leasesCollectionThroughput":
                value.ifPresent(s -> this.setLeasesCollectionThroughput(Integer.parseInt(s)));
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

    public boolean getCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    public void setCreateLeaseCollectionIfNotExists(boolean createLeaseCollectionIfNotExists) {
        this.createLeaseCollectionIfNotExists = createLeaseCollectionIfNotExists;
    }

    public boolean isCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    public int getLeasesCollectionThroughput() {
        return leasesCollectionThroughput;
    }

    public void setLeasesCollectionThroughput(int leasesCollectionThroughput) {
        this.leasesCollectionThroughput = leasesCollectionThroughput;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject inputTrigger = new JsonObject();
        inputTrigger.addProperty("type", this.getTriggerType());
        inputTrigger.addProperty("connectionStringSetting", this.connectionStringSetting);
        inputTrigger.addProperty("databaseName", databaseName);
        inputTrigger.addProperty("collectionName", this.collectionName);
        inputTrigger.addProperty("name", this.getVarName());
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("createLeaseCollectionIfNotExists", this.createLeaseCollectionIfNotExists);
        inputTrigger.addProperty("leasesCollectionThroughput", this.leasesCollectionThroughput);
        return inputTrigger;
    }
}
