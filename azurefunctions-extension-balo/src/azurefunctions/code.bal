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

public type StringBinding record {
    string? value = ();
};

public type HTTPBinding record {
    int statusCode = 200;
    string payload = "";
};

public type HandlerParams record {
    http:Request request;
    json result = { Outputs: {}, Logs: [] };
};

public type Context object {

    HandlerParams hparams;    

    public json metadata;

    public function __init(HandlerParams hparams, boolean populateMetadata) returns error? {
        self.hparams = hparams;
        if populateMetadata {
            json payload = check <@untainted> hparams.request.getJsonPayload();
            self.metadata = check payload.Metadata;
        } else {
            self.metadata = {};
        }
    }

    public function log(string msg) {
        json[] logs = <json[]> self.hparams.result.Logs;
        logs.push(msg);
    }

};

type FunctionHandler (function (HandlerParams) returns error?);

listener http:Listener hl = new(check ints:fromString(system:getEnv("FUNCTIONS_HTTPWORKER_PORT")));

map<FunctionHandler> dispatchMap = {};

@http:ServiceConfig {
    basePath: "/"
}
service AzureFunctionsServer on hl {

    @http:ResourceConfig {
        path: "/{functionName}"
    }
    resource function dispatch(http:Caller caller, http:Request request, string functionName) returns @tainted error? {
        FunctionHandler? handler = dispatchMap[functionName];
        if handler is FunctionHandler {
            HandlerParams hparams = { request };
            error? err = handler(hparams);
            if err is error {
                http:Response resp = new;
                resp.statusCode = 500;
                resp.setTextPayload(err.toString());
                check caller->respond(resp);
            } else {
                check caller->respond(<@untainted> hparams.result);
            }
        } else {
            http:Response resp = new;
            resp.setTextPayload("function handler not found: " + <@untainted> functionName);
            resp.statusCode = 404;
            check caller->respond(resp);
        }
    }

}

public function createContext(HandlerParams hparams, boolean populateMetadata) returns Context|error {
    return check new (hparams, populateMetadata);
}

public function __register(string name, FunctionHandler funcHandler) {
    dispatchMap[name] = funcHandler;
}

public function setHTTPOutput(HandlerParams params, string name, HTTPBinding binding) returns error? {
    json content = params.result;
    json outputs = check content.Outputs;
    map<json> bvals = { };
    bvals[name] = { statusCode: binding.statusCode, body: binding.payload };
    _ = check outputs.mergeJson(bvals);
}

public function getHTTPRequestFromParams(HandlerParams params) returns http:Request|error {
    return params.request;
}

public function getStringFromHTTPReq(HandlerParams params) returns string|error {
    return check <@untainted> params.request.getTextPayload();
}

public function getJsonFromHTTPReq(HandlerParams params) returns json|error {
    return check <@untainted> params.request.getJsonPayload();
}

public function getBinaryFromHTTPReq(HandlerParams params) returns byte[]|error {
    return check <@untainted> params.request.getBinaryPayload();
}

public function getStringFromInputData(HandlerParams params, string name) returns string|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    return <string> data[name];
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