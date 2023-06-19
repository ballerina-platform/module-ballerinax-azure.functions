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

import ballerinax/azure.functions as af;
import ballerina/http;
import ballerina/test;
import ballerina/lang.value;
import ballerina/io;

type DDPerson record {|
    readonly int id;
|};

type Pp DDPerson;

service /http/default/data on new af:HttpListener() {

    resource function post imp/prime(string data) returns string {
        return data; //query
    }

    resource function post imp/struct(DBEntry data) returns DBEntry {
        return data; //payload
    }

    resource function post exp/prime(@http:Payload string data) returns string {
        return data; //payload
    }

    resource function post okWithBody(string? xyz, DDPerson abc) returns http:Ok {
        return {body: abc};
    }

    resource function post singleStructured(DDPerson? p) returns DDPerson? {
        return p; // p is payload param
    }

    resource function post singleStructuredArray(DDPerson[] p) returns DDPerson[] {
        return p; // p is payload param
    }

    resource function post singleStructuredTypeRef(Pp p) returns Pp {
        return p; // p is payload param
    }

    resource function post singleStructuredWithBasicType(string q, DDPerson p) returns map<json> {
        return {person: p, query: q}; // p is payload param, q is query param
    }

    resource function post singleStructuredWithHeaderParam(@http:Header string h, DDPerson p) returns map<json> {
        return {person: p, header: h}; // p is payload param
    }

    resource function post singleBasicType(string q) returns string {
        return q; // q is query param
    }

    resource function post singleBasicTypeArray(string[] q) returns string[] {
        return q; // q is query param
    }

    resource function post singlePayloadBasicType(@http:Payload string p) returns string {
        return p; // p is payload param
    }

    resource function post singlePayloadBasicTypeArray(@http:Payload string[] p) returns string[] {
        return p; // p is payload param
    }

    resource function post queryLikeMapJson(map<json> p) returns map<json> {
        return p; // p is payload param
    }

    resource function post queryParamCheck(@http:Query map<json> q) returns map<json> {
        return q; // q is query param
    }
}

@test:Config {
    dataProvider: httpDefaultBindingDataProvider
}
function httpDefaultBindingTest(string jsonPath, json expectedResp) returns error? {
    string jsonFilePath = "./tests/resources/http/default/"+ jsonPath;
    string readString = check io:fileReadString(jsonFilePath);
    json readJson = check value:fromJsonString(readString);
    WebWorkerRequest webWorkerRequest = check readJson.cloneWithType(WebWorkerRequest);
    json resp = check clientEndpoint1->post("/"+ webWorkerRequest.Metadata.sys.MethodName, readJson);
    test:assertEquals(resp, expectedResp);
}

function httpDefaultBindingDataProvider() returns map<[string, json]>|error {
    map<[string, json]> dataSet = {
        "httpDefaultImplicitPrimitive": ["imp-prime.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"text/plain"},
                                                                                   "body":"hello"}},"Logs":[],"ReturnValue":null}],
        "httpDefaultImplicitStructured": ["imp-struct.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"id":"Anjana"}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultExplicitPrimitive": ["exp-prime.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"text/plain"},
                                                                                   "body":"Anjana"}},"Logs":[],"ReturnValue":null}],
        "httpDefaultOptionalQueryRequiredImplPayload": ["okWithBody.json", {"Outputs":{"outResp":{"statusCode":200,"body":{"id":234},"headers":{"Content-Type":"application/json"}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleStructured": ["singleStructured.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"id":234}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleStructuredArray": ["singleStructuredArray.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":[{"id":234},{"id":345}]}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleStructuredTypeRef": ["singleStructuredTypeRef.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"id":234}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleStructuredWithBasicType": ["singleStructuredWithBasicType.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"person":{"id":234},"query":"hello"}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleStructuredWithHeaderParam": ["singleStructuredWithHeaderParam.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"person":{"id":234},"header":"this_is_header"}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleBasicType": ["singleBasicType.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"text/plain"},"body":"hello"}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSingleBasicTypeArray": ["singleBasicTypeArray.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":["hello","go"]}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSinglePayloadBasicType": ["singlePayloadBasicType.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"text/plain"},"body":"\"ballerina\""}},"Logs":[],"ReturnValue":null}],
        "httpDefaultSinglePayloadBasicTypeArray": ["singlePayloadBasicTypeArray.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":["ballerina","lang"]}},"Logs":[],"ReturnValue":null}],
        "httpDefaultQueryLikeMapJson": ["queryLikeMapJson.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"name":"Payload","value":"MapJson"}}},"Logs":[],"ReturnValue":null}],
        "httpDefaultQueryParamCheck": ["queryParamCheck.json", {"Outputs":{"outResp":{"statusCode":201,"headers":{"Content-Type":"application/json"},"body":{"name":"test","value":"json"}}},"Logs":[],"ReturnValue":null}]
    };
    return dataSet;
}
