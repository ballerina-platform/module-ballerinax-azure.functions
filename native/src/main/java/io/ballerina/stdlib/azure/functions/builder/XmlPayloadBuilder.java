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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.azure.functions.builder;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
/**
 * The xml type payload builder.
 *
 * @since SwanLake update 1
 */
public class XmlPayloadBuilder extends AbstractPayloadBuilder {
    private final Type payloadType;

    public XmlPayloadBuilder(Type payloadType) {
        this.payloadType = payloadType;
    }

    @Override
    public Object getValue(BString entity, boolean readonly) {
        if (isSubtypeOfAllowedType(payloadType, TypeTags.XML_TAG)) {
//            BXml bxml = EntityBodyHandler.constructXmlDataSource(entity);
//            EntityBodyHandler.addMessageDataSource(entity, bxml);
            BXml bxml = XmlUtils.parse(entity);
            if (readonly) {
                bxml.freezeDirect();
            }
            return bxml;
        }
        throw ErrorCreator.createError(StringUtils.fromString("incompatible type found: '" + payloadType.toString()));
    }
}
