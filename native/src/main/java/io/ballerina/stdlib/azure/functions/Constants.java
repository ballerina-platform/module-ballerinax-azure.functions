/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.azure.functions;

/**
 * {@code Constants} contains the public constants to be used.
 */
public interface Constants {

    String SLASH = "/";

    String COMMA = ",";

    String PACKAGE_ORG = "ballerinax";
    String PACKAGE_NAME = "azure_functions";

    //TODO restrict "httpPayload" from the param names.
    String HTTP_TRIGGER_IDENTIFIER = "httpPayload";

    String PARAMETER_ANNOTATION = "$param$.";
    String RETURN_ANNOTATION = "$returns$";

    String PACKAGE_COMPLETE = PACKAGE_ORG + "/" + PACKAGE_NAME + ":3";
    String FUNCTION_ANNOTATION_COMPLETE = PACKAGE_COMPLETE + ":Function";
    String FUNCTION_ANNOTATION_NAME_FIELD = "name";
    
    String HTTP_PACKAGE_ORG = "ballerina";
    String HTTP_PACKAGE_NAME = "http";

    String HTTP_ANNOTATION_PREFIX = Constants.HTTP_PACKAGE_ORG + Constants.SLASH + Constants.HTTP_PACKAGE_NAME + ":" +
            Constants.HTTP_PACKAGE_VERSION + ":";

    String SERVICE_OBJECT = "AZURE_FUNCTION_SERVICE_OBJECT";

    String QUEUE_OUTPUT = "QueueOutput";
    String COSMOS_DBOUTPUT = "CosmosDBOutput";
    String HTTP_OUTPUT = "HttpOutput";
    String BLOB_OUTPUT = "BlobOutput";
    
    String OUT_MSG = "outMsg";
    
    String PAYLOAD_ANNOTATAION = "Payload";
    String HEADER_ANNOTATION = "Header";
    String SERVICE_CONF_ANNOTATION = "ServiceConfig";
    String STATUS = "status";
    String CODE = "code";
    String STATUS_CODE = "statusCode";
    String BODY = "body";
    String HEADERS = "headers";
    String CONTENT_TYPE = "Content-Type";
    String MEDIA_TYPE = "mediaType";
    String RESPONSE_FIELD = "resp";
    
    String POST = "post";
    String CREATED_201 = "201";
    String GET = "get";
    String PUT = "put";
    String PATCH = "patch";
    String DELETE = "delete";
    String HEAD = "head";
    String OPTIONS = "options";
    String DEFAULT = "default";
    
    String OK_200 = "200";
    String ACCEPTED = "202";
    
    String TEXT_PLAIN = "text/plain";
    String APPLICATION_XML = "application/xml";
    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String APPLICATION_JSON = "application/json";
    
    String BYTE_TYPE = "byte";
    String MAP_TYPE = "map";
    String JSON_TYPE = "json";
    String TABLE_TYPE = "table";

    String PAYLOAD_NOT_FOUND_ERROR = "PayloadNotFoundError";
    String FUNCTION_NOT_FOUND_ERROR = "FunctionNotFoundError";
    String INVALID_PAYLOAD_ERROR = "InvalidPayloadError";
    String HEADER_NOT_FOUND_ERROR = "HeaderNotFoundError";
    String HTTP_PACKAGE_VERSION = "2";

    String PATH_PARAM = "*";

    String REST_PATH_PARAM = "**";
    
    String AZURE_PAYLOAD_PARAMS = "Params";
    String AZURE_PAYLOAD_HEADERS = "Headers";
    String AZURE_QUERY_HEADERS = "Query";
    String AZURE_BODY_HEADERS = "Body";
}
