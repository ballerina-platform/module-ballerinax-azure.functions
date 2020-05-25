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
import ballerina/io;
import ballerina/system;
import ballerina/lang.'int as ints;
import ballerina/lang.'array as arrays;

public type HTTPBinding record {
    int statusCode = 200;
    string payload?;
};

public type StringOutputBinding record {
    string value?;
};

public type HTTPRequest record {
    string url;
    string method;
    map<string> query;
    map<string[]> headers;
    map<string> params;
    string identities;
    string body;
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
            self.metadata = check getMetadata(self.hparams);
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

public function unescapeJson(json value) returns json|error {
    io:StringReader sr = new(value.toString(), encoding = "UTF-8");
    return <@untainted> sr.readJson();
}

public function getMetadata(HandlerParams hparams) returns json|error {
    json payload = check <@untainted> hparams.request.getJsonPayload();
    json metadata = check payload.Metadata;
    return metadata;
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

public function getStringFromMetadata(HandlerParams params, string name) returns string|error {
    map<json> metadata = <map<json>> check getMetadata(params);
    json fld = check unescapeJson(metadata[name]);    
    string result = fld.toJsonString();
    if (result.startsWith("\"") && result.endsWith("\"")) {
        result = result.substring(1, result.length() - 1);
    }
    return result;
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
    return data[name].toString();
}

public function getOptionalStringFromInputData(HandlerParams params, string name) returns string?|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    json entry = data[name];
    if entry == () {
        return ();
    } else {
        return entry.toString();
    }
}

public function getBytesFromInputData(HandlerParams params, string name) returns byte[]|error {
    var data = check getStringFromInputData(params, name);
    return arrays:fromBase64(data.toString());
}

public function getOptionalBytesFromInputData(HandlerParams params, string name) returns byte[]?|error {
    string? data = check getOptionalStringFromInputData(params, name);
    if data == () || data == "null" {
        return ();
    } else {
        return arrays:fromBase64(data.toString());
    }
}

public function getBodyFromHTTPInputData(HandlerParams params, string name) returns string|error {
    HTTPRequest req = check getHTTPRequestFromInputData(params, name);
    return req.body;
}

function extractHTTPHeaders(json headers) returns map<string[]> {
    map<json> headerMap = <map<json>> headers;
    map<string[]> result = {};
    foreach var key in headerMap.keys() {
        json[] values = <json[]> headerMap[key];
        string[] headerVals = values.map(function (json j) returns string { return j.toString(); } );
        result[key] = headerVals;
    }
    return result;
}

function extractStringMap(json params) returns map<string> {
    map<json> paramMap = <map<json>> params;
    map<string> result = {};
    foreach var key in paramMap.keys() {
        result[key] = paramMap[key].toString();
    }
    return result;
}

public function getHTTPRequestFromInputData(HandlerParams params, string name) returns HTTPRequest|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    json hreq = data[name];
    string url = hreq.Url.toString();
    string method = hreq.Method.toString();
    map<string[]> headers = extractHTTPHeaders(check hreq.Headers);
    map<string> hparams = extractStringMap(check hreq.Params);
    map<string> query = extractStringMap(check unescapeJson(check hreq.Query));
    string identities = hreq.Identities.toString();
    string body = hreq.Body.toString();
    HTTPRequest req = { url: url, method: method, query: query, headers: headers, 
                        params: hparams, identities: identities, body: body };
    return req;
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