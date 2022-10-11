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
import org.ballerinax.azurefunctions.Constants;
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
        this.setVarName(Constants.RETURN_VAR_NAME);
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
