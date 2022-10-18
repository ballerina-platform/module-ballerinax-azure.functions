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

isolated service class ResourceService {
    *http:Service;

    private final HttpToAzureAdaptor adaptor;

    isolated function init(HttpToAzureAdaptor adaptor) {
        self.adaptor = adaptor;
    }

    isolated resource function post .(http:Caller caller, http:Request request) returns error? {
        http:Response response = new;
        json message = check request.getJsonPayload();
        Payload payload = check message.cloneWithType(Payload);
        string functionName = payload.Metadata.sys.MethodName;
        map<anydata>|error callRegisterMethod = self.adaptor.callNativeMethod(payload.Data, functionName);
        response.setJsonPayload(getResponsePayload(callRegisterMethod));
        check caller->respond(response);
    }
}


isolated function getResponsePayload (map<anydata>|error nativeResponse) returns json {
    if (nativeResponse is PayloadNotFoundError || nativeResponse is InvalidPayloadError || nativeResponse is HeaderNotFoundError) {
        return {"Outputs": {"resp": {"statusCode": 400, "body": nativeResponse.message(),"headers": {"Content-Type": "text/plain"}}}, "Logs": [], "ReturnValue": null};
    } else if (nativeResponse is error) {
        return {"Outputs": {"resp": {"statusCode": 500}}, "Logs": [], "ReturnValue": null};
    } else {
        return {Outputs: nativeResponse.toJson(), Logs: [], ReturnValue: null};
    }
}
