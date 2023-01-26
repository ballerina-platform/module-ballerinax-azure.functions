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
package org.ballerinax.azurefunctions.service.cosmosdb;

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
public class CosmosDBInputBinding extends InputBinding {

    private String connectionStringSetting;
    private String databaseName;
    private String collectionName;
    private String id;
    private String partitionKey;
    private String sqlQuery;

    public CosmosDBInputBinding(AnnotationNode queueTrigger, String varName) {
        super("cosmosDB");
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
            case "connectionStringSetting":
                value.ifPresent(this::setConnectionStringSetting);
                break;
            case "databaseName":
                value.ifPresent(this::setDatabaseName);
                break;
            case "collectionName":
                value.ifPresent(this::setCollectionName);
                break;
            case "sqlQuery":
                value.ifPresent(this::setSqlQuery);
                break;
            case "id":
                value.ifPresent(this::setId);
                break;
            case "partitionKey":
                value.ifPresent(this::setPartitionKey);
                break;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject inputTrigger = new JsonObject();
        inputTrigger.addProperty("type", this.getTriggerType());
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("name", this.getVarName());
        inputTrigger.addProperty("connectionStringSetting", this.getConnectionStringSetting());
        inputTrigger.addProperty("databaseName", this.getDatabaseName());
        inputTrigger.addProperty("collectionName", this.getCollectionName());
        if (this.partitionKey != null) {
            inputTrigger.addProperty("partitionKey", this.getPartitionKey());
        }
        if (this.id != null) {
            inputTrigger.addProperty("id", this.getId());
        }
        if (this.sqlQuery != null) {
            inputTrigger.addProperty("sqlQuery", this.getSqlQuery());
        }
        return inputTrigger;
    }
}
