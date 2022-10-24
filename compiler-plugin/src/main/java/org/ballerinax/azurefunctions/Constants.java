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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.azurefunctions;

/**
 * Constants for Azure Functions.
 */
public class Constants {

    public static final String AZURE_FUNCTIONS_MODULE_NAME = "azure_functions";
    public static final String COLON = ":";

    public static final String HTTP = "http";
    public static final String HEADER_ANNOTATION_TYPE = "HttpHeader";
    public static final String AZURE_FUNCTIONS_PACKAGE_ORG = "ballerinax";
    public static final String CHARSET = "UTF-8";

    public static final String ANNOTATION_HTTP_TRIGGER = "HttpTrigger";
    public static final String ANNOTATION_QUEUE_TRIGGER = "QueueTrigger";
    public static final String ANNOTATION_COSMOS_TRIGGER = "CosmosDBTrigger";
    public static final String ANNOTATION_TIMER_TRIGGER = "TimerTrigger";
    public static final String ANNOTATION_BLOB_TRIGGER = "BlobTrigger";

    public static final String AZURE_HTTP_LISTENER = "HttpListener";
    public static final String AZURE_QUEUE_LISTENER = "QueueListener";
    public static final String AZURE_COSMOS_LISTENER = "CosmosDBListener";
    public static final String AZURE_TIMER_LISTENER = "TimerListener";
    public static final String AZURE_BLOB_LISTENER = "BlobListener";

    public static final String COSMOS_INPUT_BINDING = "CosmosDBInput";
    public static final String BLOB_INPUT_BINDING = "BlobInput";

    public static final String QUEUE_OUTPUT_BINDING = "QueueOutput";
    public static final String HTTP_OUTPUT_BINDING = "HttpOutput";
    public static final String COSMOS_OUTPUT_BINDING = "CosmosDBOutput";
    public static final String TWILIO_OUTPUT_BINDING = "TwilioSmsOutput";
    public static final String BLOB_OUTPUT_BINDING = "BlobOutput";

    public static final String DIRECTION_IN = "in";
    public static final String DIRECTION_OUT = "out";

    public static final String RETURN_VAR_NAME = "outMsg";

    public static final String FUNCTION_DIRECTORY = "azure_functions";
    public static final String LOCAL_FUNCTION_DIRECTORY = FUNCTION_DIRECTORY + "_local";

    public static final String TARGET_DIRECTORY = "target/";

    public static final String ARTIFACT_PATH = TARGET_DIRECTORY + FUNCTION_DIRECTORY;
    public static final String LOCAL_ARTIFACT_PATH = TARGET_DIRECTORY + LOCAL_FUNCTION_DIRECTORY;

    public static final String SETTINGS_LOCAL_FILE_NAME = "local.settings.json";
    public static final String EXTENSIONS_FILE_NAME = "extensions.json";
    public static final String SETTINGS_FILE_NAME = "settings.json";
    public static final String TASKS_FILE_NAME = "tasks.json";

    public static final String FUNCTION_ANNOTATION = "Function";
    public static final String SERVICE_CONFIG_ANNOTATION = "ServiceConfig";
    public static final String TREAT_NILABLE_AS_OPTIONAL = "treatNilableAsOptional";

}
