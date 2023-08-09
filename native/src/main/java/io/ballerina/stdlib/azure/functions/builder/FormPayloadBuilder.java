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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.azure.functions.Constants;
import io.ballerina.stdlib.mime.util.EntityHeaderHandler;
import io.ballerina.stdlib.mime.util.MimeUtil;
import io.ballerina.stdlib.mime.util.MultipartDecoder;
import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import org.jvnet.mimepull.MIMEConfig;
import org.jvnet.mimepull.MIMEMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static io.ballerina.runtime.api.utils.StringUtils.fromStringArray;
import static io.ballerina.runtime.api.utils.StringUtils.fromStringSet;
import static io.ballerina.stdlib.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static io.ballerina.stdlib.mime.util.MimeConstants.HEADERS_MAP_FIELD;
import static io.ballerina.stdlib.mime.util.MimeConstants.HEADER_NAMES_ARRAY_FIELD;

/**
 * The form blob type payload builder.
 *
 * @since SwanLake update 1
 */
public class FormPayloadBuilder extends AbstractPayloadBuilder {

    private final Type payloadType;
    private final String contentType;

    public FormPayloadBuilder(Type payloadType, String contentType) {
        this.payloadType = payloadType;
        this.contentType = contentType;
    }

    @Override
    public Object getValue(BString entity, boolean readonly) {
        if (payloadType.getTag() == TypeTags.ARRAY_TAG) {
            Type elementType = ((ArrayType) payloadType).getElementType();
            if (elementType.getTag() == TypeTags.BYTE_TAG) {
                return getByteArrFromForm(entity);
            }
        } else if (payloadType.getTag() == TypeTags.UNION_TAG) {
            List<Type> memberTypes = ((UnionType) payloadType).getMemberTypes();
            for (Type memberType : memberTypes) {
                if (memberType.getTag() == TypeTags.ARRAY_TAG) {
                    Type elementType = ((ArrayType) memberType).getElementType();
                    if (elementType.getTag() == TypeTags.BYTE_TAG) {
                        return getByteArrFromForm(entity);
                    }
                }
            }
        } else if (payloadType.getTag() == TypeTags.OBJECT_TYPE_TAG) {
            Module objPackage = payloadType.getPackage();
            if (objPackage.getOrg().equals(Constants.BALLERINA_PACKAGE) &&
                    objPackage.getName().equals(Constants.MIME_PACKAGE_NAME) &&
                    payloadType.getName().equals(Constants.ENTITY)) {
                return getEntity(entity);
            }
        }
        throw ErrorCreator.createError(StringUtils.fromString("incompatible type found: '" + payloadType.toString()));
    }

    private BArray getByteArrFromForm(BString entity) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                Base64.getDecoder().decode(entity.getValue()));
        try {
            MimeType mimeType = new MimeType(contentType);
            MIMEMessage mimeMessage =
                    new MIMEMessage(byteArrayInputStream, mimeType.getParameter("boundary"), new MIMEConfig());
            InputStream read = mimeMessage.getAttachments().get(0).read();
            byte[] bytes = read.readAllBytes();
            return ValueCreator.createArrayValue(bytes);
        } catch (IOException | MimeTypeParseException ignored) {
            return ValueCreator.createArrayValue(new byte[0]);
        }
    }

    private BObject getEntity(BString encodedBody) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                Base64.getDecoder().decode(encodedBody.getValue()));
        BObject entityObject = createEntityObject();
        MultipartDecoder.parseBody(entityObject, contentType, byteArrayInputStream);
        //TODO Generalize mime entity creation for all cases

        BMap<BString, Object> headers = EntityHeaderHandler.getNewHeaderMap();

        headers.put(StringUtils.fromString("content-type"), fromStringArray(new String[]{ this.contentType }));
        entityObject.set(HEADERS_MAP_FIELD, headers);

        Set<String> distinctNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> headerNames = new ArrayList<>();
        headerNames.add("content-type");

        distinctNames.addAll(headerNames);
        entityObject.set(HEADER_NAMES_ARRAY_FIELD, fromStringSet(distinctNames));
        return entityObject;
    }

    public static BObject createEntityObject() {
        BObject entity = createObjectValue(MimeUtil.getMimePackage(), Constants.ENTITY);
        entity.addNativeData(ENTITY_BYTE_CHANNEL, null);
        return entity;
    }

    /**
     * Method that creates a runtime object value using the given package id and object type name.
     *
     * @param module         value creator specific for the package.
     * @param objectTypeName name of the object type.
     * @param fieldValues    values to be used for fields when creating the object value instance.
     * @return value of the object.
     */
    private static BObject createObjectValue(Module module, String objectTypeName,
                                             Object... fieldValues) {

        Object[] fields = new Object[fieldValues.length * 2];

        // Adding boolean values for each arg
        for (int i = 0, j = 0; i < fieldValues.length; i++) {
            fields[j++] = fieldValues[i];
            fields[j++] = true;
        }

        // passing scheduler, strand and properties as null for the moment, but better to expose them via this method
        return ValueCreator.createObjectValue(module, objectTypeName, null, null, null, fields);
    }
}
