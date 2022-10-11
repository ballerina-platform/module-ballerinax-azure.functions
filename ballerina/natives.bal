// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import ballerina/jballerina.java;

isolated class HttpToAzureAdaptor {
    isolated function init(HttpService 'service) {
        externInit(self, 'service);
    }

    isolated function getAzureFunctionNames() returns string[] = @java:Method {
        'class: "io.ballerina.stdlib.azure.functions.NativeHttpToAzureAdaptor"
    } external;

    isolated function callNativeMethod(map<HttpPayload|string> body, string functionName) returns map<anydata>|error = 
    @java:Method {
        'class: "io.ballerina.stdlib.azure.functions.NativeHttpToAzureAdaptor"
    } external;
}

isolated function externInit(HttpToAzureAdaptor adaptor, HttpService serviceObj) = @java:Method {
    'class: "io.ballerina.stdlib.azure.functions.NativeHttpToAzureAdaptor"
} external;



isolated class AzureRemoteAdapter {
    isolated function init(RemoteService 'service) {
        externRemoteInit(self, 'service);
    }

    isolated function callRemoteFunction(map<json> body, string functionName) returns map<anydata>|error = 
    @java:Method {
        'class: "io.ballerina.stdlib.azure.functions.NativeRemoteAdapter"
    } external;
}

isolated function externRemoteInit(AzureRemoteAdapter adaptor, RemoteService serviceObj) = @java:Method {
    'class: "io.ballerina.stdlib.azure.functions.NativeRemoteAdapter"
} external;
