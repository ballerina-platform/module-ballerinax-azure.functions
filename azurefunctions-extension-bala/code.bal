// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/os;
import ballerina/lang.'int as ints;
import ballerina/lang.'array as arrays;
import ballerina/lang.'string as strings;
import ballerina/lang.'boolean as booleans;

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
    json[] identities;
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
public class Context {

    HandlerParams hparams;    

    public json metadata;

    public isolated function init(HandlerParams hparams, boolean populateMetadata) returns error? {
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
    public isolated function log(string msg) {
        log(self.hparams, msg);
    }

}

# INTERNAL usage - Enters to function invocation logs.
# 
# + hparams - the handler parameters object
# + msg - The log message
public isolated function log(HandlerParams hparams, string msg) {
    json[] logs = <json[]> checkpanic hparams.result.Logs;
    logs.push(msg);
}

# INTERNAL usage - Checks if request tracing is enabled.
# 
# + return - The request tracing flag
public isolated function isRequestTrace() returns boolean {
    string? value = os:getEnv("BALLERINA_AZURE_FUNCTIONS_REQUEST_TRACE");
    if value is string {
        var flag = booleans:fromString(value);
        if flag is boolean {
            return flag;
        } else {
            return false;
        }
    } else {
        return false;
    }
}

public isolated function logError(HandlerParams hparams, error err) {
    log(hparams, "ERROR: " + err.toString());
}

public isolated function logRequest(HandlerParams hparams, http:Request request) {
    var payload = request.getTextPayload();
    string val = payload is error ? payload.toString() : payload.toString();
    log(hparams, "REQUEST: " + val);
}

# Function handler type.
type FunctionHandler (function (HandlerParams) returns error?);

@untainted public listener http:Listener hl = new(check ints:fromString(os:getEnv("FUNCTIONS_CUSTOMHANDLER_PORT")));

public isolated function handleFunctionResposne(error? err, HandlerParams hparams) {
    http:Request request = hparams.request;
    http:Response response = hparams.response;
    if err is error {
       logError(hparams, err);
       logRequest(hparams, request);
       response.setJsonPayload(<@untainted> hparams.result);
    } else {
       if !hparams.pure {
           if isRequestTrace() {
               logRequest(hparams, request);
           }
           response.setJsonPayload(<@untainted> hparams.result);
       }
    }
}

# INTERNAL usage - extracts the metadata.
# 
# + hparams - The handler parameters
# + return - The metadata JSON
public isolated function getMetadata(HandlerParams hparams) returns json|error {
    json payload = check <@untainted> hparams.request.getJsonPayload();
    json metadata = check payload.Metadata;
    return metadata;
}

# INTERNAL usage - creates function context.
# 
# + hparams - The handler parameters
# + populateMetadata - The flag to populate metadata
# + return - The function context
public isolated function createContext(HandlerParams hparams, boolean populateMetadata) returns Context|error {
    return new Context(hparams, populateMetadata);
}

# INTERNAL usage - Sets the HTTP output.
# 
# + params - The handler parameters
# + name - The parameter name
# + binding - The binding data
# + return - An error in failure
public isolated function setHTTPOutput(HandlerParams params, string name, HTTPBinding binding) returns error? {
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
public isolated function setStringOutput(HandlerParams params, string name, StringOutputBinding binding) returns error? {
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
public isolated function setBlobOutput(HandlerParams params, string name, any binding) returns error? {
    string? value = ();
    if binding is BytesOutputBinding {
        byte[]? bytes = binding?.value;
        if bytes is byte[] {
            value = bytes.toBase64();
        }
    } else if binding is StringOutputBinding {
        string? text = binding?.value;
        if text is string {
            value = text.toBytes().toBase64();
        }
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
public isolated function setTwilioSmsOutput(HandlerParams params, string name, TwilioSmsOutputBinding binding) returns error? {
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
public isolated function setPureHTTPOutput(HandlerParams params, HTTPBinding binding) returns error? {
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
public isolated function setPureStringOutput(HandlerParams params, string value) returns error? {
    params.response.setTextPayload(value);
    params.pure = true;
}

# INTERNAL usage - Returns the HTTP request data.
# 
# + params - The handler parameters
# + return - The HTTP request
public isolated function getHTTPRequestFromParams(HandlerParams params) returns http:Request|error {
    return params.request;
}

# INTERNAL usage - Returns the string payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The string payload
public isolated function getStringFromHTTPReq(HandlerParams params) returns string|error {
    return check <@untainted> params.request.getTextPayload();
}

# INTERNAL usage - Returns a parsed JSON value.
# 
# + input - The escaped JSON value
# + return - The parsed JSON value
isolated function parseJson(string input) returns json|error {
    json x = check input.fromJsonString();    
    return x;
}

# INTERNAL usage - Returns a json value from metadata.
# 
# + params - The handler parameters
# + name - The metadata entry name
# + return - The metadata entry value
public isolated function getJsonFromMetadata(HandlerParams params, string name) returns json|error {
    map<json> metadata = <map<json>> check getMetadata(params);
    return parseJson(metadata[name].toString());
}

# INTERNAL usage - Returns a string value from metadata.
# 
# + params - The handler parameters
# + name - The metadata entry name
# + return - The metadata entry value
public isolated function getStringFromMetadata(HandlerParams params, string name) returns string|error {
    json result = check getJsonFromMetadata(params, name);
    return result.toString();
}

# INTERNAL usage - Returns the JSON payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The JSON payload
public isolated function getJsonFromHTTPReq(HandlerParams params) returns json|error {
    return check <@untainted> params.request.getJsonPayload();
}

# INTERNAL usage - Returns the binary payload from the HTTP request.
# 
# + params - The handler parameters
# + return - The binary payload
public isolated function getBinaryFromHTTPReq(HandlerParams params) returns byte[]|error {
    return check <@untainted> params.request.getBinaryPayload();
}

# INTERNAL usage - Returns the string value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The string value
public isolated function getStringFromInputData(HandlerParams params, string name) returns string|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> check payload.Data;
    return data[name].toString();
}

# INTERNAL usage - Returns the JSON string value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The string value
public isolated function getJsonStringFromInputData(HandlerParams params, string name) returns string|error {
    return (check getJsonFromInputData(params, name)).toString();
}

# INTERNAL usage - Returns the optional string value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The optional string value
public isolated function getOptionalStringFromInputData(HandlerParams params, string name) returns string?|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> check payload.Data;
    json result = data[name];
    if result == () {
       return ();
    }
    return result.toString();
}

# INTERNAL usage - Returns the binary value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The binary value
public isolated function getBytesFromInputData(HandlerParams params, string name) returns byte[]|error {
    string data = check getStringFromInputData(params, name);
    return arrays:fromBase64(data.toString());
}

# INTERNAL usage - Returns the optional binary value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The optional string value
public isolated function getOptionalBytesFromInputData(HandlerParams params, string name) returns byte[]?|error {
    string? data = check getOptionalStringFromInputData(params, name);
    if data == () {
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
public isolated function getStringConvertedBytesFromInputData(HandlerParams params, string name) returns string|error {
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
public isolated function getOptionalStringConvertedBytesFromInputData(HandlerParams params, string name) returns string?|error {
    string? data = check getOptionalStringFromInputData(params, name);
    if data == () {
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
public isolated function getBodyFromHTTPInputData(HandlerParams params, string name) returns string|error {
    HTTPRequest req = check getHTTPRequestFromInputData(params, name);
    return req.body;
}

# INTERNAL usage - Extracts HTTP headers from the JSON value.
# 
# + headers - The headers JSON
# + return - The headers map
isolated function extractHTTPHeaders(json headers) returns map<string[]> {
    map<json> headerMap = <map<json>> headers;
    map<string[]> result = {};
    foreach var key in headerMap.keys() {
        json[] values = <json[]> headerMap[key];
        string[] headerVals = values.map(isolated function (json j) returns string { return j.toString(); } );
        result[key] = headerVals;
    }
    return result;
}

# INTERNAL usage - Extracts string map from the JSON value.
# 
# + params - The params JSON
# + return - The string map
isolated function extractStringMap(json params) returns map<string> {
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
public isolated function getHTTPRequestFromInputData(HandlerParams params, string name) returns HTTPRequest|error {
    json payload = check getJsonFromHTTPReq(params);
    map<json> data = <map<json>> check payload.Data;
    json hreq = data[name];
    var urlVal = hreq.Url;
    string url = urlVal is error ? urlVal.toString() : urlVal.toString();
    var methodVal = hreq.Method;
    string method = methodVal is error ? methodVal.toString() : methodVal.toString();
    map<string[]> headers = extractHTTPHeaders(check hreq.Headers);
    map<string> hparams = extractStringMap(check hreq.Params);
    json qx = check hreq.Query;
    map<string> query = extractStringMap(check parseJson(qx.toString()));
    json idx = check hreq.Identities;
    json identitiesTemp = check parseJson(idx.toString());
    json[] identities = <json[]> identitiesTemp;
    var bodyVal = hreq.Body;
    string body = bodyVal is error ? bodyVal.toString() : bodyVal.toString();
    HTTPRequest req = { url: url, method: method, query: query, headers: headers, 
                        params: hparams, identities: identities, body: body };
    return req;
}

# INTERNAL usage - Returns the JSON value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public isolated function getJsonFromInputData(HandlerParams params, string name) returns json|error {
    return parseJson(check getStringFromInputData(params, name));
}

# INTERNAL usage - JSON parse the string value available from "getJsonStringFromInputData".
# 
# + params - The handler parameters
# + name - The input data entry name
# + return - The JSON value
public isolated function getParsedJsonFromJsonStringFromInputData(HandlerParams params, string name) returns json|error {
    return parseJson(check getJsonStringFromInputData(params, name));
}

# INTERNAL usage - Returns a converted Ballerina value from input data.
# 
# + params - The handler parameters
# + name - The input data entry name
# + recordType - The record type descriptor
# + return - The JSON value
public isolated function getBallerinaValueFromInputData(HandlerParams params, string name,
                                       typedesc<anydata> recordType) returns anydata|error {
    var result = getJsonFromInputData(params, name);
    if result is error {
        return result;
    } else {
        return result.cloneWithType(recordType);
    }
}

# INTERNAL usage - Returns the optional converted Ballerina value from "getParsedJsonFromJsonStringFromInputData".
# 
# + params - The handler parameters
# + name - The input data entry name
# + recordType - The record type descriptor
# + return - The JSON value
public isolated function getOptionalBallerinaValueFromInputData(HandlerParams params, string name,
                                       typedesc<anydata> recordType) returns anydata?|error {
    var result = getParsedJsonFromJsonStringFromInputData(params, name);
    if result is error {
        return result;
    } else if result == () {
        return ();
    } else {
        return result.cloneWithType(recordType);
    }
}

# INTERNAL usage - Sets the string return value.
# 
# + params - The handler parameters
# + value - The string return value
# + return - An error in failure
public isolated function setStringReturn(HandlerParams params, string value) returns error? {
    json content = params.result;
    _ = check content.mergeJson({ ReturnValue: value });
}

# INTERNAL usage - Sets the JSON return value.
# 
# + params - The handler parameters
# + value - The JSON return value
# + return - An error in failure
public isolated function setJsonReturn(HandlerParams params, json value) returns error? {
    json content = params.result;
    _ = check content.mergeJson({ ReturnValue: value });
}

# INTERNAL usage - Sets the CosmosDS JSON return value.
# 
# + params - The handler parameters
# + value - The JSON return value
# + partitionKey - The partition key
# + return - An error in failure
public isolated function setCosmosDBJsonReturn(HandlerParams params, json value, string partitionKey) returns error? {
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
public isolated function setBallerinaValueAsJsonReturn(HandlerParams params, anydata value) returns error? {
    json content = params.result;
    check setJsonReturn(params, check value.cloneWithType(json));
}

# INTERNAL usage - Converts a CosmosDS Ballerina value to a JSON and set the return value.
# 
# + params - The handler parameters
# + value - The value
# + partitionKey - The partition key
# + return - An error in failure
public isolated function setCosmosDBBallerinaValueAsJsonReturn(HandlerParams params, anydata value,
                                                      string partitionKey) returns error? {
    json content = params.result;
    check setCosmosDBJsonReturn(params, check value.cloneWithType(json), partitionKey);
}

# INTERNAL usage - Sets the HTTP binding return value.
# 
# + params - The handler parameters
# + binding - The HTTP binding return value
# + return - An error in failure
public isolated function setHTTPReturn(HandlerParams params, HTTPBinding binding) returns error? {
    string? payload = binding?.payload;
    if (payload is string) {
        json content = params.result;
        _ = check content.mergeJson({ ReturnValue: { statusCode: binding.statusCode, body: payload } });
    }
}
