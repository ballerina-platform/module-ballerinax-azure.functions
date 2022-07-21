package io.ballerina.stdlib.azure.functions;

import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.langlib.value.CloneWithType;
import org.ballerinalang.langlib.value.FromJsonString;

import static io.ballerina.runtime.api.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.TypeTags.JSON_TAG;
import static io.ballerina.runtime.api.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.runtime.api.TypeTags.STRING_TAG;
/**
 * Contains the utilities required for azure functions runtime.
 * 
 * @since 2.0.0
 */
public class Utilities {

//    public static final String ENTITY = "Entity";

//    public static BObject createEntityObject() {
//        return createObjectValue(MimeUtil.getMimePackage(), ENTITY);
//    }

//    private static BObject createObjectValue(Module module, String objectTypeName,
//                                             Object... fieldValues) {
//
//        Object[] fields = new Object[fieldValues.length * 2];
//
//        // Adding boolean values for each arg
//        for (int i = 0, j = 0; i < fieldValues.length; i++) {
//            fields[j++] = fieldValues[i];
//            fields[j++] = true;
//        }
//
//        // passing scheduler, strand and properties as null for the moment, but better to expose them via this method
//        return ValueCreator.createObjectValue(module, objectTypeName, null, null, null, fields);
//    }

    public static Object convertJsonToDataBoundParamValue(BString bodyValue, Type type) {
        Object bValue = bodyValue;
        if (type.getTag() == JSON_TAG) {
            bValue = FromJsonString.fromJsonString((BString) JsonUtils.parse(bodyValue));
        } else if (type.getTag() == RECORD_TYPE_TAG) {
            Object jsonValue = FromJsonString.fromJsonString((BString) JsonUtils.parse(bodyValue));
            bValue = CloneWithType.convert(type, jsonValue);
        } else if (type.getTag() == ARRAY_TAG) {
            bValue = CloneWithType.convert(type, JsonUtils.parse(bodyValue));
        } else if (type.getTag() == STRING_TAG) {
            try {
                bValue = CloneWithType.convert(type, JsonUtils.parse(bodyValue));
            } catch (BError error) {
                return bValue;
            }
        }
//        switch (type.getTag()) {
//            case TypeTags.STRING_TAG:
//                bValue = StringUtils.fromString(stringAggregator.getAggregateString());
//                break;
//            case TypeTags.XML_TAG:
//                bValue = XmlUtils.parse(stringAggregator.getAggregateString());
//                break;
//            case TypeTags.RECORD_TYPE_TAG:
//                bValue = CloneWithType.convert(targetType, JsonUtils.parse(
//                        stringAggregator.getAggregateString()));
//                break;
//            default:
//                bValue = FromJsonStringWithType.fromJsonStringWithType(bodyValue),
//                        ValueCreator.createTypedescValue(targetType));
//                break;
//        }
        return bValue;
    }
}
