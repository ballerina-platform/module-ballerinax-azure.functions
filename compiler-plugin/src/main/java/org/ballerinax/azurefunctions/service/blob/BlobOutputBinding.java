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

    public BlobOutputBinding(AnnotationNode annotationNode, int index) {
        super("blob", index);
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
        }
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        output.addProperty("path", this.getPath());
        output.addProperty("connection", this.getConnection());
        output.addProperty("dataType", "string");
        return output;
    }
}
