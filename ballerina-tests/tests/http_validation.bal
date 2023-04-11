// Copyright (c) 2023 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerinax/azure_functions as af;
import ballerina/http;
import ballerina/io;
import ballerina/lang.value;
import ballerina/test;

public type TestRecord record {
    string test1;
    string test2;
};

service /http  on new af:HttpListener() {
    resource function get path/[int x]() returns string {
        return "Hello, " + x.toBalString() + "!";
    }

    resource function get query(int name) returns string {
        return "Hello, " + name.toString() + "!";
    }

    resource function get query/optionals(int? age) returns string {
        if (age is ()) {
            return "age not specified";
        }
        return "Hello from the query "+ age.toString();
    }

    resource function post payload(@http:Payload TestRecord payload, string name) returns string {
        return "Hello, " + name + "!" + payload.toString();
    }

    resource function post payload/optionals(@http:Payload TestRecord? payload) returns string {
        if (payload is ()) {
            return "payload not specified";
        }
        return "Hello from the query "+ payload.toString();
    }

    resource function get input/cosmos(@af:CosmosDBInput {
                                connectionStringSetting: "CosmosDBConnection",
                                databaseName: "reviewdb",
                                collectionName: "c1",
                                sqlQuery: "SELECT * FROM Items"
                            } TestRecord[] entries) returns TestRecord[]|error {
        return entries;
    }
}

final http:Client clientEndpoint1 = check new ("http://localhost:3000");

type WebWorkerRequest record {
    record{record{string MethodName;} sys;} Metadata;
};

@test:Config {
    dataProvider: httpValidationDataProvider
}
function httpValidationTest(string jsonPath, json expectedResp) returns error? {
    string jsonFilePath = "./tests/resources/http/"+ jsonPath;
    string readString = check io:fileReadString(jsonFilePath);
    json readJson = check value:fromJsonString(readString);
    WebWorkerRequest webWorkerRequest = check readJson.cloneWithType(WebWorkerRequest);
    json resp = check clientEndpoint1->post("/"+ webWorkerRequest.Metadata.sys.MethodName, readJson);
    test:assertEquals(resp, expectedResp);
}

function httpValidationDataProvider() returns map<[string, json]>|error {
    map<[string, json]> dataSet = {
        "httpPathValid": ["path/valid.json", {"Outputs":{"outResp":{"statusCode":200, "headers":{"Content-Type":"text/plain"}, "body":"Hello, 2!"}}, "Logs":[], "ReturnValue":null}],
        "httpPathInvalidPayload": ["path/invalid-payload.json", {"Outputs":{"outResp":{"statusCode":400, "headers":{"Content-Type":"text/plain"}, "body":"{ballerina/lang.int}NumberParsingError"}}, "Logs":[], "ReturnValue":null}],
        
        "httpPayloadInvalidPayload": ["payload/invalid-payload.json", {"Outputs":{"outResp":{"statusCode":400, "headers":{"Content-Type":"text/plain"}, "body":"{ballerina}ConversionError"}}, "Logs":[], "ReturnValue":null}],
        "httpPayloadOptionalInvalid": ["payload/optional-invalid.json", {"Outputs":{"outResp":{"statusCode":400, "headers":{"Content-Type":"text/plain"}, "body":"{ballerina}ConversionError"}}, "Logs":[], "ReturnValue":null}],
        "httpPayloadOptionalNegative": ["payload/optional-negative.json", {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"payload not specified"}}, "Logs":[], "ReturnValue":null}],
        "httpPayloadOptionalPositive": ["payload/optional-positive.json", {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"Hello from the query {\"test1\":\"Test 1\",\"test2\":\"Test 2\"}"}}, "Logs":[], "ReturnValue":null}],
        
        "httpQueryInvalid": ["query/invalid-payload.json", {"Outputs":{"outResp":{"statusCode":400, 
        "headers":{"Content-Type":"text/plain"}, "body":"Query param value parsing failed for 'name'"}}, "Logs":[], "ReturnValue":null}],
        "httpQueryOptionalInvalid": ["query/optional-invalid.json", {"Outputs":{"outResp":{"statusCode":400, "headers":{"Content-Type":"text/plain"}, "body":"Query param value parsing failed for 'age'"}}, "Logs":[], "ReturnValue":null}],
        "httpQueryOptionalNegative": ["query/optional-negative.json", {"Outputs":{"outResp":{"statusCode":200, "headers":{"Content-Type":"text/plain"}, "body":"age not specified"}}, "Logs":[], "ReturnValue":null}],
        "httpQueryOptionalPositive": ["query/optional-positive.json", {"Outputs":{"outResp":{"statusCode":200, "headers":{"Content-Type":"text/plain"}, "body":"Hello from the query 23"}}, "Logs":[], "ReturnValue":null}],

        "httpInputCosmosEmpty": ["input/cosmos/empty.json", {"Outputs":{"outResp":{"statusCode":200, "headers":{"Content-Type":"application/json"}, "body":[]}}, "Logs":[], "ReturnValue":null}],
        "httpInputCosmosInvalid": ["input/cosmos/invalid.json", {"Outputs":{"outResp":{"statusCode":400, "headers":{"Content-Type":"text/plain"}, "body":"{ballerina}ConversionError"}}, "Logs":[], "ReturnValue":null}],
        "httpInputCosmosValid": ["input/cosmos/valid.json", {"Outputs":{"outResp":{"statusCode":200, "headers":{"Content-Type":"application/json"}, "body": [{"test1":"Test 1","test2":"Test 2","_attachments":"attachments/","_rid":"YcIcALZZoCSChB4AAAAAAA==","id":"123","_self":"dbs/YcIcAA==/colls/YcIcALZZoCQ=/docs/YcIcALZZoCSChB4AAAAAAA==/","_etag":"\"0601def4-0000-0700-0000-63d0cc740000\"","_ts":1674628212},{"test1":"Test 11","test2":"Test 22","_attachments":"attachments/","_rid":"YcIcALZZoCSDhB4AAAAAAA==","id":"124","_self":"dbs/YcIcAA==/colls/YcIcALZZoCQ=/docs/YcIcALZZoCSDhB4AAAAAAA==/","_etag":"\"06019cf8-0000-0700-0000-63d0cc8c0000\"","_ts":1674628236}]}}, "Logs":[], "ReturnValue":null}]

    };
    return dataSet;
}
