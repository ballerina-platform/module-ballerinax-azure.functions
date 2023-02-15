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
package org.ballerinax.azurefunctions.service.timer;

import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import org.ballerinax.azurefunctions.Constants;
import org.ballerinax.azurefunctions.Util;
import org.ballerinax.azurefunctions.service.RemoteTriggerBinding;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a timer trigger binding in function.json.
 *
 * @since 2.0.0
 */
public class TimerTriggerBinding extends RemoteTriggerBinding {

    private String schedule;
    private boolean runOnStartup = true;

    public TimerTriggerBinding(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel,
                               Map<String, Node> types) {
        super("timerTrigger", "onTrigger", Constants.ANNOTATION_TIMER_TRIGGER, 
                serviceDeclarationNode, semanticModel, types);
    }

    @Override
    protected void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
        String text = ((IdentifierToken) fieldNode.fieldName()).text();
        Optional<String> value = Util.extractValueFromAnnotationField(fieldNode);
        switch (text) {
            case "schedule":
                value.ifPresent(this::setSchedule);
                break;
            case "runOnStartup":
                value.ifPresent(runOnStartup1 -> setRunOnStartup(Boolean.parseBoolean(runOnStartup1)));
                break;
        }
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject inputTrigger = new JsonObject();
        inputTrigger.addProperty("type", this.getTriggerType());
        inputTrigger.addProperty("schedule", this.getSchedule());
        inputTrigger.addProperty("runOnStartup", this.isRunOnStartup());
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("name", this.getVarName());
        return inputTrigger;
    }
}
