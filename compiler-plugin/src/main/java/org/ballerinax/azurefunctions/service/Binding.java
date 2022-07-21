package org.ballerinax.azurefunctions.service;

import com.google.gson.JsonObject;

/**
 * Represents a binding in the functions.json. 
 * 
 * @since 2.0.0
 */
public abstract class Binding {
    private String triggerType;
    private String varName;
    private String direction;

    public Binding(String triggerType, String direction) {
        this.triggerType = triggerType;
        this.direction = direction;
    }

    public String getTriggerType() {
        return triggerType;
    }
    
    public String getVarName() {
        return varName;
    }

    public String getDirection() {
        return direction;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public abstract JsonObject getJsonObject();
}
