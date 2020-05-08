// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/system;
import ballerina/lang.'int as ints;

type StringBinding object {

    public string? value = ();

    public function setValue(string value) {
        self.value = value;
    }

    public function getValue() returns string? {
        return self.value;
    }

};

type HTTPBinding object {

    public int statusCode = 200;

    public string payload = "";

    public function setStatusCode(int statusCode) {
        self.statusCode = statusCode;
    }

    public function setPayload(string payload) {
        self.payload = payload;
    }

    public function getStatusCode() returns int {
        return self.statusCode;
    }

    public function getPayload() returns string {
        return self.payload;
    }

};

type FunctionHandler (function (http:Request) returns json|error);

listener http:Listener hl = new(check ints:fromString(system:getEnv("FUNCTIONS_HTTPWORKER_PORT")));

map<FunctionHandler> dispatchMap = {};

@http:ServiceConfig {
    basePath: "/"
}
service AzureFunctionsServer on hl {

    @http:ResourceConfig {
        path: "/{functionName}"
    }
    resource function dispatch(http:Caller caller, http:Request req, string functionName) returns @tainted error? {
        FunctionHandler? handler = dispatchMap[functionName];
        if handler is FunctionHandler {
            check caller->respond(check handler(req));
        } else {
            http:Response resp = new;
            resp.setTextPayload("function handler not found: " + <@untainted> functionName);
            resp.statusCode = 404;
            check caller->respond(resp);
        }
    }

}

public function __register(string name, FunctionHandler funcHandler) {
    dispatchMap[name] = funcHandler;
}

function setHTTPOutput(json content, string name, HTTPBinding binding) returns error? {
    json outputs = check content.Outputs;
    map<json> bvals = { };
    bvals[name] = { statusCode: binding.getStatusCode(), body: binding.getPayload() };
    _ = check outputs.mergeJson(bvals);
}

function setStringOutput(json content, string name, string? binding) returns error? {
    if binding is string {
        json outputs = check content.Outputs;
        map<json> bvals = { };
        bvals[name] = binding;
        _ = check outputs.mergeJson(bvals);
    }
}

function setStringReturn(json content, string value) returns error? {
    _ = check content.mergeJson({ ReturnValue: value });
}