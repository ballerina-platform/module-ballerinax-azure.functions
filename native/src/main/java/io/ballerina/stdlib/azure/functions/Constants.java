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
    String PACKAGE_ORG = "ballerinax";
    String PACKAGE_NAME = "azure_functions";

    String SERVICE_OBJECT = "AZURE_FUNCTION_SERVICE_OBJECT";

     String QUEUE_OUTPUT = "QueueOutput";
     String COSMOS_DBOUTPUT = "CosmosDBOutput";
     String OUT_MSG = "outMsg";
     String HTTP_OUTPUT = "HTTPOutput";
     String BLOB_OUTPUT = "BlobOutput";
     String PAYLOAD_ANNOTATAION = "Payload";
     String STATUS = "status";
     String CODE = "code";
     String STATUS_CODE = "statusCode";
     String BODY = "body";
     String HEADERS = "headers";
     String CONTENT_TYPE = "content-type";
     String MEDIA_TYPE = "mediaType";
     String RESP = "resp";
}
