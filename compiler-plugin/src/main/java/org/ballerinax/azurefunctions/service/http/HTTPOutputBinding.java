package org.ballerinax.azurefunctions.service.http;

import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import org.ballerinax.azurefunctions.service.OutputBinding;

/**
 * Represents a HTTP Output binding in function.json.
 *
 * @since 2.0.0
 */
public class HTTPOutputBinding extends OutputBinding {

    public HTTPOutputBinding(AnnotationNode annotationNode) {
        super("http");
        this.setVarName("resp");
    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject output = new JsonObject();
        output.addProperty("type", this.getTriggerType());
        output.addProperty("direction", this.getDirection());
        output.addProperty("name", this.getVarName());
        return output;
    }
}
