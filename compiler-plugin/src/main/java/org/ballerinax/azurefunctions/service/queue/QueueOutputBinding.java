package org.ballerinax.azurefunctions.service.queue;

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
public class QueueOutputBinding extends OutputBinding {

    private String connection = "AzureWebJobsStorage";
    private String queueName;

    public QueueOutputBinding(AnnotationNode annotationNode) {
        super("queue");
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
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("connection", this.connection);
        output.addProperty("queueName", this.queueName);
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        return output;
    }
}
