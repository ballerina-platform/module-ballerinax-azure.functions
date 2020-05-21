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

public type HTTPBinding record {
    int statusCode = 200;
    string payload?;
};

public type StringOutputBinding record {
    string value?;
};

public type HandlerParams record {
    http:Request request;
    http:Response response;
    boolean pure = false;
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
        http:Response response = new;
        if handler is FunctionHandler {
            HandlerParams hparams = { request, response };
            error? err = trap handler(hparams);
            if err is error {
                response.statusCode = 500;
                response.setTextPayload(err.toString());
                check caller->respond(response);
            } else {
                if !hparams.pure {
                    response.setJsonPayload(<@untainted> hparams.result);
                }
                check caller->respond(response);
            }
        } else {
            response.setTextPayload("function handler not found: " + <@untainted> functionName);
            response.statusCode = 404;
            check caller->respond(response);
        }
    }

}

public function createContext(HandlerParams hparams, boolean populateMetadata) returns Context|error {
    return new Context(hparams, populateMetadata);
}

public function __register(string name, FunctionHandler funcHandler) {
    dispatchMap[name] = funcHandler;
}

public function setHTTPOutput(HandlerParams params, string name, HTTPBinding binding) returns error? {
    string? payload = binding?.payload;
    if (payload is string) {
        json content = params.result;
        json outputs = check content.Outputs;
        map<json> bvals = { };
        bvals[name] = { statusCode: binding.statusCode, body: payload };
        _ = check outputs.mergeJson(bvals);
    }
}

public function setStringOutput(HandlerParams params, string name, StringOutputBinding binding) returns error? {
    string? value = binding?.value;
    if (value is string) {
        json content = params.result;
        json outputs = check content.Outputs;
        map<json> bvals = { };
        bvals[name] = value;
        _ = check outputs.mergeJson(bvals);
    }
}

public function setPureHTTPOutput(HandlerParams params, HTTPBinding binding) returns error? {
    string? payload = binding?.payload;
    if (payload is string) {
        params.response.statusCode = binding.statusCode;
        params.response.setTextPayload(payload);
    }
    params.pure = true;
}

public function setPureStringOutput(HandlerParams params, string value) returns error? {
    params.response.setTextPayload(value);
    params.pure = true;
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

public function getJsonFromInputData(HandlerParams params, string name) returns json|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    return data[name];
}

public function setStringReturn(HandlerParams params, string value) returns error? {
    json content = params.result;
    _ = check content.mergeJson({ ReturnValue: value });
}