package org.ballerinax.azurefunctions.service.queue;

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
 * Represents a queue trigger binding in function.json.
 *
 * @since 2.0.0
 */
public class QueueTriggerBinding extends RemoteTriggerBinding {

    private String connection = "AzureWebJobsStorage";
    private String queueName;

    public QueueTriggerBinding(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel) {
        super("queueTrigger", "onMessage", Constants.ANNOTATION_QUEUE_TRIGGER, serviceDeclarationNode, semanticModel);
    }

    @Override
    protected void extractValueFromAnnotation(SpecificFieldNode fieldNode) {
        String text = ((IdentifierToken) fieldNode.fieldName()).text();
        Optional<String> value = Util.extractValueFromAnnotationField(fieldNode);
        switch (text) {
            case "queueName":
                value.ifPresent(this::setQueueName);
                break;
            case "connection":
                value.ifPresent(this::setConnection);
                break;
            default:
                throw new RuntimeException("Unexpected property in the annotation");
        }
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject inputTrigger = new JsonObject();
        inputTrigger.addProperty("type", this.getTriggerType());
        inputTrigger.addProperty("connection", this.connection);
        if (this.queueName != null) {
            inputTrigger.addProperty("queueName", this.queueName);
        }
        inputTrigger.addProperty("direction", this.getDirection());
        inputTrigger.addProperty("name", this.getVarName());
        return inputTrigger;
    }
}
