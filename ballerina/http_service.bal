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

import ballerina/http;
import ballerina/io;

isolated service class ResourceService {
    *http:Service;

    private final HttpToAzureAdaptor adaptor;

    isolated function init(HttpToAzureAdaptor adaptor) {
        self.adaptor = adaptor;
    }

    isolated resource function post .(http:Caller caller, http:Request request) returns error? {

        http:Response response = new;
        json message = check request.getJsonPayload();
        // map<json> body = {};
        io:println(message.toJsonString());
        //TODO conver to record instead of map
        Payload payload = check message.cloneWithType(Payload);  
        // io:println(payload);
        // body = <map<json>>check message.Data;
        // string functionName = check message.Metadata.sys.MethodName;
        string functionName = payload.Metadata.sys.MethodName;
        map<anydata> callRegisterMethod = check self.adaptor.callNativeMethod(payload.Data, functionName);
        json result = {Outputs: callRegisterMethod.toJson(), Logs: []};
        result = check result.mergeJson({ReturnValue: null});
        io:println(result);
        response.setJsonPayload(result);
        check caller->respond(response);
    }
}
