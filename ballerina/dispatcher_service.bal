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
import ballerina/log;

isolated service class DispatcherService {
    *http:Service;

    private final AzureRemoteAdapter adaptor;
    private final string remoteMethodName;

    isolated function init(AzureRemoteAdapter adaptor, string remoteMethodName) {
        self.adaptor = adaptor;
        self.remoteMethodName = remoteMethodName;
    }

    isolated resource function post .(http:Caller caller, http:Request request) returns error? {
        http:Response response = new;
        json platformPayload = check request.getJsonPayload();
        map<anydata>|error callRemoteFunction = self.adaptor.callRemoteFunction(<map<json>>platformPayload, self.remoteMethodName);
        if (callRemoteFunction is error) {
            log:printError(callRemoteFunction.message());
            return;
        }
        response.setJsonPayload({Outputs: callRemoteFunction.toJson(), Logs: [], ReturnValue: null});
        check caller->respond(response);
    }
}
