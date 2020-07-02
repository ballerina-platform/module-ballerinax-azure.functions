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
import ballerina/lang.'string as strings;

# HTTP binding data.
# 
# + statusCode - The HTTP response status code
# + payload - The HTTP response payload
public type HTTPBinding record {
    int statusCode = 200;
    string payload?;
};

# String output binding data.
# 
# + value - The string value
public type StringOutputBinding record {
    string value?;
};

# Byte array output binding data.
# 
# + value - The byte[] value
public type BytesOutputBinding record {
    byte[] value?;
};

# Twilion SMS output binding data.
# 
# + to - The SMS recipient phone number
# + body - The message body
public type TwilioSmsOutputBinding record {
    string to?;
    string body?;
};

# HTTP request binding data.
# 
# + url - The request URL
# + method - The request HTTP method
# + query - The request query parameter map
# + headers - The request HTTP header map
# + params - The request parameters
# + identities - The request identities
# + body - The request body
public type HTTPRequest record {
    string url;
    string method;
    map<string> query;
    map<string[]> headers;
    map<string> params;
    string identities;
    string body;
};

# INTERNAL stucture - the request handler parameter data.
# 
# + request - The HTTP request
# + response - The HTTP response
# + pure - The flag to mention if it's a pure HTTP request
# + result - The result JSON
public type HandlerParams record {
    http:Request request;
    http:Response response;
    boolean pure = false;
    json result = { Outputs: {}, Logs: [] };
};

# The request context holder. 
# 
# + metadata - The context metadata
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

    # Enters to function invocation logs.
    # 
    # + msg - The log message
    public function log(string msg) {
        json[] logs = <json[]> self.hparams.result.Logs;
        logs.push(msg);
    }

};

# Function handler type.
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
            return err;
        } else {
            response.setTextPayload("function handler not found: " + <@untainted> functionName);
            response.statusCode = 404;
            check caller->respond(response);
        }
    }

}

# INTERNAL usage - unescape a JSON encoded in a string.
# 
# + value - String escaped JSON value
# + return - Unescaped JSON value.
public function unescapeJson(json value) returns json|error {
    io:StringReader sr = new(value.toString(), encoding = "UTF-8");
    return <@untainted> sr.readJson();
}

# INTERNAL usage - extracts the metadata.
# 
# + hparams - The handler parameters
# + return - The metadata JSON
public function getMetadata(HandlerParams hparams) returns json|error {
    json payload = check <@untainted> hparams.request.getJsonPayload();
    json metadata = check payload.Metadata;
    return metadata;
}

# INTERNAL usage - creates function context.
# 
# + hparams - The handler parameters
# + populateMetadata - The flag to populate metadata
# + return - The function context
public function createContext(HandlerParams hparams, boolean populateMetadata) returns Context|error {
    return new Context(hparams, populateMetadata);
}

# INTERNAL usage - registers a handler function.
# 
# + name - The name of the function
# + funcHandler - The function handler
public function __register(string name, FunctionHandler funcHandler) {
    dispatchMap[name] = funcHandler;
}

# INTERNAL usage - Sets the HTTP output.
# 
# + params - The handler parameters
# + name - The parameter name
# + binding - The binding data
# + return - An error in failure
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

# INTERNAL usage - Sets the string output.
# 
# + params - The handler parameters
# + name - The parameter name
# + binding - The binding data
# + return - An error in failure
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

# INTERNAL usage - Sets the Blob output.
# 
# + params - The handler parameters
# + name - The parameter name
# + binding - The binding data
# + return - An error in failure
public function setBlobOutput(HandlerParams params, string name, any binding) returns error? {
    string? value = ();
    if binding is BytesOutputBinding {
        // TODO issue: https://github.com/Azure/azure-functions-host/issues/6091
        byte[]? bytes = binding?.value;
        if bytes is byte[] {
            value = bytes.toBase64();
        }
    } else if binding is StringOutputBinding {
        value = binding?.value;        
    }
    if value is string {
        json content = params.result;
        json outputs = check content.Outputs;
        map<json> bvals = { };
        bvals[name] = value;
        _ = check outputs.mergeJson(bvals);
    }
}

# INTERNAL usage - Sets the Twilio output.
# 
# + params - The handler parameters
# + name - The parameter name
# + binding - The binding data
# + return - An error in failure
public function setTwilioSmsOutput(HandlerParams params, string name, TwilioSmsOutputBinding binding) returns error? {
    string? to = binding?.to;
    string? body = binding?.body;
    if to is string && body is string {
        json content = params.result;
        json outputs = check content.Outputs;
        map<json> bvals = { };
        bvals[name] = { body, to };
        _ = check outputs.mergeJson(bvals);
    }
}

# INTERNAL usage - Sets the pure HTTP output.
# 
# + params - The handler parameters
# + binding - The binding data
# + return - An error in failure
public function setPureHTTPOutput(HandlerParams params, HTTPBinding binding) returns error? {
    string? payload = binding?.payload;
    if payload is string {
        params.response.statusCode = binding.statusCode;
        params.response.setTextPayload(payload);
    }
    params.pure = true;
}

# INTERNAL usage - Sets the pure string output.
# 
# + params - The handler parameters
# + value - The value
# + return - An error in failure
public function setPureStringOutput(HandlerParams params, string value) returns error? {
    params.response.setTextPayload(value);
    params.pure = true;
}

# INTERNAL usage - Returns the HTTP request data.
# 
# + params - The handler parameters
# + return - The HTTP request
public function getHTTPRequestFromParams(HandlerParams params) returns http:Request|error {
    return params.request;
}

# INTERNAL usage - Returns the string payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The string payload
public function getStringFromHTTPReq(HandlerParams params) returns string|error {
    return check <@untainted> params.request.getTextPayload();
}

# INTERNAL usage - Returns a string value from metadata.
# 
# + params - The handler parameters
# + name - The metadata entry name
# + return - The metadata entry value
public function getStringFromMetadata(HandlerParams params, string name) returns string|error {
    map<json> metadata = <map<json>> check getMetadata(params);
    json fld = check unescapeJson(metadata[name]);    
    string result = fld.toJsonString();
    if (result.startsWith("\"") && result.endsWith("\"")) {
        result = result.substring(1, result.length() - 1);
    }
    return result;
}

# INTERNAL usage - Returns the JSON payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The JSON payload
public function getJsonFromHTTPReq(HandlerParams params) returns json|error {
    return check <@untainted> params.request.getJsonPayload();
}

# INTERNAL usage - Returns the binary payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The binary payload
public function getBinaryFromHTTPReq(HandlerParams params) returns byte[]|error {
    return check <@untainted> params.request.getBinaryPayload();
}

# INTERNAL usage - Returns the string value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The string value
public function getStringFromInputData(HandlerParams params, string name) returns string|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    return data[name].toString();
}

# INTERNAL usage - Returns the optional string value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The optional string value
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

# INTERNAL usage - Returns the binary value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The binary value
public function getBytesFromInputData(HandlerParams params, string name) returns byte[]|error {
    string data = check getStringFromInputData(params, name);
    return arrays:fromBase64(data.toString());
}

# INTERNAL usage - Returns the optional binary value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The optional string value
public function getOptionalBytesFromInputData(HandlerParams params, string name) returns byte[]?|error {
    string? data = check getOptionalStringFromInputData(params, name);
    if data == () || data == "null" {
        return ();
    } else {
        return arrays:fromBase64(data.toString());
    }
}

# INTERNAL usage - Returns the string value converted from input binary data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The string value
public function getStringConvertedBytesFromInputData(HandlerParams params, string name) returns string?|error {
    string data = check getStringFromInputData(params, name);
    var result = arrays:fromBase64(data.toString());
    if result is error {
        return result;
    } else {
        return check strings:fromBytes(result);
    }
}

# INTERNAL usage - Returns the optional string value converted from input binary data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The optional binary value
public function getOptionalStringConvertedBytesFromInputData(HandlerParams params, string name) returns string?|error {
    string? data = check getOptionalStringFromInputData(params, name);
    if data == () || data == "null" {
        return ();
    } else {
        var result = arrays:fromBase64(data.toString());
        if result is error {
            return result;
        } else {
            return check strings:fromBytes(result);
        }
    }
}

# INTERNAL usage - Returns the HTTP body value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The HTTP body
public function getBodyFromHTTPInputData(HandlerParams params, string name) returns string|error {
    HTTPRequest req = check getHTTPRequestFromInputData(params, name);
    return req.body;
}

# INTERNAL usage - Extracts HTTP headers from the JSON value.
# 
# + headers - The headers JSON
# + return - The headers map
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

# INTERNAL usage - Extracts string map from the JSON value.
# 
# + params - The params JSON
# + return - The string map
function extractStringMap(json params) returns map<string> {
    map<json> paramMap = <map<json>> params;
    map<string> result = {};
    foreach var key in paramMap.keys() {
        result[key] = paramMap[key].toString();
    }
    return result;
}

# INTERNAL usage - Populates the HTTP request structure from an input data entry.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The HTTP request
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

# INTERNAL usage - Returns the JSON value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public function getJsonFromInputData(HandlerParams params, string name) returns json|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    // the unescape is because the input data JSON values are string escaped 
    return unescapeJson(data[name]);
}

# INTERNAL usage - Returns the JSON value from input data - double escape.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public function getJsonFromInputDataDoubleEscape(HandlerParams params, string name) returns json|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    json entry = data[name];
    // the unescape is because the input data JSON values are string escaped 
    return unescapeJson(check unescapeJson(data[name]));
}

# INTERNAL usage - Returns the optional JSON value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public function getOptionalJsonFromInput(HandlerParams params, string name) returns json|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    json entry = data[name];
    if entry == () || entry == "null" {
        return ();
    } else {
        // the unescape is because the input data JSON values are string escaped 
        return unescapeJson(data[name]);
    }
}

# INTERNAL usage - Returns the optional JSON value from input data - double escape.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public function getOptionalJsonFromInputDataDoubleEscape(HandlerParams params, string name) returns json|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> payload.Data;
    json entry = data[name];
    if entry == () || entry == "null" {
        return ();
    } else {
        // the unescape is because the input data JSON values are string escaped 
        return unescapeJson(check unescapeJson(data[name]));
    }
}

# INTERNAL usage - Returns a converted Ballerina value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + recordType - The record type descriptor
# + return - The JSON value
public function getBallerinaValueFromInputData(HandlerParams params, string name, 
                                       typedesc<anydata> recordType) returns anydata|error {
    var result = getJsonFromInputData(params, name);
    if result is error {
        return result;
    } else {
        return recordType.constructFrom(result);
    }
}

# INTERNAL usage - Returns the converted Ballerina value from input data - double escape.
# 
# + params - The handler parameters
# + name - The input data entry name
# + recordType - The record type descriptor
# + return - The JSON value
public function getBallerinaValueFromInputDataDoubleEscape(HandlerParams params, string name, 
                                       typedesc<anydata> recordType) returns anydata?|error {
    var result = getJsonFromInputDataDoubleEscape(params, name);
    if result is error {
        return result;
    } else {
        return recordType.constructFrom(result);
    }
}

# INTERNAL usage - Returns the optional converted Ballerina value from input data - double escape.
# 
# + params - The handler parameters
# + name - The input data entry name
# + recordType - The record type descriptor
# + return - The JSON value
public function getOptionalBallerinaValueFromInputDataDoubleEscape(HandlerParams params, string name, 
                                       typedesc<anydata> recordType) returns anydata?|error {
    var result = getOptionalJsonFromInputDataDoubleEscape(params, name);
    if result is error {
        return result;
    } else if result == () {
        return result;
    } else {
        return recordType.constructFrom(result);
    }
}

# INTERNAL usage - Sets the string return value.
# 
# + params - The handler parameters
# + value - The string return value
# + return - An error in failure
public function setStringReturn(HandlerParams params, string value) returns error? {
    json content = params.result;
    _ = check content.mergeJson({ ReturnValue: value });
}

# INTERNAL usage - Sets the JSON return value.
# 
# + params - The handler parameters
# + value - The JSON return value
# + return - An error in failure
public function setJsonReturn(HandlerParams params, json value) returns error? {
    json content = params.result;
    _ = check content.mergeJson({ ReturnValue: value });
}

# INTERNAL usage - Sets the CosmosDS JSON return value.
# 
# + params - The handler parameters
# + value - The JSON return value
# + partitionKey - The partition key
# + return - An error in failure
public function setCosmosDBJsonReturn(HandlerParams params, json value, string partitionKey) returns error? {
    json content = params.result;
    if partitionKey.length() > 0 {
        if value is json[] {
            json[] valArray = <json[]> value;
            foreach json valEntry in valArray {
                _ = check valEntry.mergeJson({ pk: partitionKey });
            }
        } else {
            _ = check value.mergeJson({ pk: partitionKey });
        }
    }
    _ = check content.mergeJson({ ReturnValue: value });
}

# INTERNAL usage - Converts a Ballerina value to a JSON and set the return value.
# 
# + params - The handler parameters
# + value - The value
# + return - An error in failure
public function setBallerinaValueAsJsonReturn(HandlerParams params, anydata value) returns error? {
    json content = params.result;
    check setJsonReturn(params, check json.constructFrom(value));
}

# INTERNAL usage - Converts a CosmosDS Ballerina value to a JSON and set the return value.
# 
# + params - The handler parameters
# + value - The value
# + partitionKey - The partition key
# + return - An error in failure
public function setCosmosDBBallerinaValueAsJsonReturn(HandlerParams params, anydata value, 
                                                      string partitionKey) returns error? {
    json content = params.result;
    check setCosmosDBJsonReturn(params, check json.constructFrom(value), partitionKey);
}

# INTERNAL usage - Sets the HTTP binding return value.
# 
# + params - The handler parameters
# + binding - The HTTP binding return value
# + return - An error in failure
public function setHTTPReturn(HandlerParams params, HTTPBinding binding) returns error? {
    string? payload = binding?.payload;
    if (payload is string) {
        json content = params.result;
        _ = check content.mergeJson({ ReturnValue: { statusCode: binding.statusCode, body: payload } });
    }
}
