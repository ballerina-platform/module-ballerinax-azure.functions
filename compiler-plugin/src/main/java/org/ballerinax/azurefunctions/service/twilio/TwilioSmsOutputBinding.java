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
package org.ballerinax.azurefunctions.service.twilio;

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
 * Represents Twilio SMS output binding in functions.json.
 *
 * @since 2.0.0
 */
public class TwilioSmsOutputBinding extends OutputBinding {

    private String accountSidSetting = "AzureWebJobsTwilioAccountSid";
    private String authTokenSetting = "AzureWebJobsTwilioAuthToken";
    private String from;
    private String to;

    public TwilioSmsOutputBinding(AnnotationNode annotationNode, int index) {
        super("twilioSms", index);
        SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().orElseThrow().fields();
        for (MappingFieldNode fieldNode : fields) {
            extractValueFromAnnotation((SpecificFieldNode) fieldNode);
        }
    }

    private void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
        String text = ((IdentifierToken) fieldNode.fieldName()).text();
        Optional<String> value = Util.extractValueFromAnnotationField(fieldNode);
        switch (text) {
            case "accountSidSetting":
                value.ifPresent(this::setAccountSidSetting);
                break;
            case "authTokenSetting":
                value.ifPresent(this::setAuthTokenSetting);
                break;
            case "'from":
                value.ifPresent(this::setFrom);
                break;
            case "to":
                value.ifPresent(this::setTo);
                break;
            default:
                throw new RuntimeException("Unexpected property in the annotation");
        }
    }

    public String getAccountSidSetting() {
        return accountSidSetting;
    }

    public void setAccountSidSetting(String accountSidSetting) {
        this.accountSidSetting = accountSidSetting;
    }

    public String getAuthTokenSetting() {
        return authTokenSetting;
    }

    public void setAuthTokenSetting(String authTokenSetting) {
        this.authTokenSetting = authTokenSetting;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("accountSidSetting", this.accountSidSetting);
        output.addProperty("authTokenSetting", this.authTokenSetting);
        output.addProperty("from", this.from);
        output.addProperty("to", this.to);
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        return output;
    }
}
