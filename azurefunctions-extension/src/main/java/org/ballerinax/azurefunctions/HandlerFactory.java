/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.azurefunctions;

import org.ballerinax.azurefunctions.handlers.ContextParameterHandler;
import org.ballerinax.azurefunctions.handlers.HTTPOutputParameterHandler;
import org.ballerinax.azurefunctions.handlers.HTTPTriggerParameterHandler;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

/**
 * Factory class to create parameter and return handlers.
 */
public class HandlerFactory {
    
    public static ParameterHandler createParameterHandler(BLangSimpleVariable param) throws AzureFunctionsException {
        if (Utils.isContextType(param.type)) {
            return new ContextParameterHandler(param);
        }
        BLangAnnotationAttachment ann = Utils.extractAzureFunctionAnnotation(param);
        if (ann == null) {
            throw new AzureFunctionsException("Parameter '" + param.getName().getValue()
                    + "' does not have a valid annotation or a type for an Azure Function");
        }
        String name = ann.getAnnotationName().getValue();
        if ("HTTPOutput".equals(name)) {
            return new HTTPOutputParameterHandler(param, ann);
        } else if ("HTTPTrigger".equals(name)) {
            return new HTTPTriggerParameterHandler(param, ann);
        }
        throw new AzureFunctionsException("Parameter handler not found for the name: " + name);
    }

    public static ReturnHandler createReturnHandler(String name) throws AzureFunctionsException {
        throw new AzureFunctionsException("Return handler not found for the name: " + name);
    }

}
