package org.ballerinax.azurefunctions.service.timer;

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
 * Represents a timer trigger binding in function.json.
 *
 * @since 2.0.0
 */
public class TimerTriggerBinding extends RemoteTriggerBinding {

    private String schedule;
    private boolean runOnStartup = true;

    public TimerTriggerBinding(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel) {
        super("timerTrigger", "onTrigger", Constants.ANNOTATION_TIMER_TRIGGER, serviceDeclarationNode, semanticModel);
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
            default:
                throw new RuntimeException("Unexpected property in the annotation");
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
        inputTrigger.addProperty("schedule", this.schedule);
        inputTrigger.addProperty("runOnStartup", this.runOnStartup);
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("name", this.getVarName());
        return inputTrigger;
    }
}
