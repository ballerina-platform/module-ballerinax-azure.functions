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
import ballerina/mime;

public type DBEntry record {
    string id;
};

public type Person record {
    string name;
    int age;
};

public type RateLimitHeaders record {|
    int Content\-Length;
    string Content\-Type;
|};

public type NoHeaderVal record {|
    int Content\-Length;
    string Content\-Type;
    string Content\-Type1;
|};

listener af:HttpListener ep1 = new ();

service /hello\- on ep1 {

    resource function post hello\-query() returns string|error {
        return "Hello from the hello-query";
    }
}

listener af:HttpListener ep2 = new ();

@http:ServiceConfig {
    treatNilableAsOptional: false
}
service /httpHeader on ep2 {
    resource function post nonTreatNilAsOpt\-Nil\-noHeaderTest(@http:Header string? hoste) returns string? {
        return hoste;
    }

    resource function post nonTreatNilAsOpt\-nonNil\-noHeaderTest(@http:Header string hoste) returns string {
        return hoste;

    }

    resource function post nonTreatNilAsOpt\-nonNil\-HeaderTest(@http:Header string hos) returns string {
        return hos;

    }

    resource function post nonTreatNilAsOpt\-Nil\-HeaderTest(@http:Header string? hos) returns string? {
        return hos;

    }
}

listener af:HttpListener ep3 = new ();

service /httpHeader on ep3 {
    resource function post retrFromAnnotField(@http:Header {name: "Content-Type"} string contentType) returns string {

        return contentType;
    }

    resource function post retrFromParam(@http:Header string Host) returns string {

        return Host;

    }

    resource function post retrSingleVal(@http:Header {name: "Content-Length"} int contentLength) returns int {

        return contentLength + 10;

    }

    resource function post retrArrVal(@http:Header {name: "Content-Length"} int[] contentLength) returns int {

        return contentLength[0] + 15;

    }

    resource function post retrArrValStr(@http:Header string[] test) returns string {
        return test[0];

    }

    resource function post retrAsRecord(@http:Header RateLimitHeaders rateLimiters) returns int {
        return rateLimiters.Content\-Length + 100;

    }

    resource function post retrNilable(@http:Header string? Host) returns string? {
        return Host;

    }

    resource function post treatNilAsOpt\-nonNil\-noHeaderTest(@http:Header string hoste) returns string {
        return hoste;

    }

    resource function post treatNilAsOpt\-nonNil\-HeaderTest(@http:Header string hos) returns string {
        return hos;

    }

    resource function post retrAsRecordNoField(@http:Header NoHeaderVal noHeaderVal) returns int {
        return noHeaderVal.Content\-Length + 100;

    }

    resource function post treatNilAsOpt\-Nil\-noHeaderTest(@http:Header string? hoste) returns string? {
        return hoste;

    }

    resource function post treatNilAsOpt\-Nil\-HeaderTest(@http:Header string? hos) returns string? {
        return hos;

    }
}
listener af:HttpListener ep = new ();

service /hello on ep {
    
    resource function default all() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function get nonNilableQueryParamTest(string foo) returns string? {
        if (foo == "") {
            return "alpha";
        }
        return foo;
    }

    resource function post optional/out(@http:Payload string greeting) returns string {
        return "Hello from optional output binding";
    }

    resource function post optional/payload(@http:Payload string? greeting) returns string {
        if (greeting is string) {
            return "Hello, the payload found " + greeting;
        }
        return "Hello, the payload wasn't set but all good ;)";
    }

    resource function post .(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from . path ";
    }
    resource function post httpResTest1(@http:Payload string greeting) returns @af:HttpOutput http:Unauthorized {
        http:Unauthorized unauth = {
            body: "Helloworld.....",
            mediaType: "application/account+json",
            headers: {
                "Location": "/myServer/084230"
            }
        };
        return unauth;
    }

    resource function post httpResTest2(@http:Payload string greeting) returns @af:HttpOutput http:Ok {
        http:Ok ok = {body: "Helloworld....."};
        return ok;
    }
    resource function post httpResTest3(@http:Payload string greeting) returns @af:HttpOutput http:InternalServerError {
        http:InternalServerError err = {
            body: "Helloworld.....",
            headers: {
                "Content-Type": "application/json+id",
                "Location": "/myServer/084230"
            }
        };
        return err;
    }
    resource function post httpResTest4(@http:Payload string greeting) returns @af:HttpOutput http:InternalServerError {
        http:InternalServerError err = {};
        return err;
    }

    resource function post foo(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo path " + greeting;
    }

    resource function post foo/[string bar](@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo param " + bar;
    }

    resource function post foo/bar(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo bar res";
    }
    resource function post restParamTest/[string... bar]() returns string {
        return "Hellow from rest param " + bar[0];
    }

    resource function post db(@http:Payload string greeting, @af:CosmosDBInput {
                                  connectionStringSetting: "CosmosDBConnection",
                                  databaseName: "db1",
                                  collectionName: "c2",
                                  sqlQuery: "SELECT * FROM Items"
                              } DBEntry[] input1) returns @af:HttpOutput string|error {
        return "Hello " + greeting + input1[0].id;
    }

    resource function post payload/jsonToRecord(@http:Payload Person greeting) returns @af:HttpOutput string|error {
        return "Hello from json to record " + greeting.name;
    }

    resource function post payload/jsonToJson(@http:Payload json greeting) returns @af:HttpOutput string|error {
        string name = check greeting.name;
        return "Hello from json to json " + name;
    }

    resource function post payload/xmlToXml(@http:Payload xml greeting) returns @af:HttpOutput string|error {
        return greeting.toJsonString();
    }

    resource function post payload/textToString(@http:Payload string greeting) returns @af:HttpOutput string|error {
        return greeting;
    }

    resource function post payload/textToByte(@http:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function post payload/octaToByte(@http:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function get err/empty/payload(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from get empty payload";
    }

    resource function post err/invalid/payload(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from get invalid payload " + greeting;
    }

    resource function get httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function put httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function patch httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function delete httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function head httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function options httpAccessorTest() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function post httpResTest5() returns http:StatusCodeResponse {
        http:InternalServerError err = {};
        return err;
    }

    resource function post nonHttpResTest1() returns string {
        string s1 = "alpha";
        return s1;
    }

    resource function post nonHttpResTest2() returns xml {
        xml x1 = xml `<book>The Lost World</book>`;
        return x1;
    }

    resource function post nonHttpResTest3() returns byte[] {
        byte[] b1 = base64 `yPHaytRgJPg+QjjylUHakEwz1fWPx/wXCW41JSmqYW8=`;
        return b1;

    }

    resource function post nonHttpResTest4() returns int {
        int i1 = 100;
        return i1;
    }

    resource function post nonHttpResTest6() returns decimal {
        decimal d1 = 100;
        return d1;
    }

    resource function post nonHttpResTest7() returns boolean {
        boolean bo1 = true;
        return bo1;
    }

    resource function post nonHttpResTest8() returns map<json> {
        map<json> mj1 = {"a": {"b": 12, "c": "helloworld"}};
        return mj1;

    }

    resource function post nonHttpResTest9() returns table<map<json>> {

        table<map<json>> t = table [
                {"a": {"b": 12, "c": "helloworld"}},
                {"b": 1100}
            ];

        return t;
    }

    resource function post nonHttpResTest10() returns map<json>[] {

        map<json>[] mjarr1 = [{"a": {"b": 12, "c": "helloworld"}}, {"b": 12}];

        return mjarr1;
    }

    resource function post nonHttpResTest11() returns table<map<json>>[] {
        table<map<json>>[] tarr = [
            table [
                    {"a": {"b": 12, "c": "helloworld"}},
                    {"b": 12}
                ],
            table [
                    {"a": {"b": 14, "c": "helloworld"}},
                    {"b": 100}
                ]
        ];
        return tarr;
    }

    resource function post nonReturnTest1() {

    }

    resource function get blobInput(@http:Payload string greeting, string name, @af:BlobInput {path: "bpath1/{Query.name}"} byte[]? blobIn) returns string|error {
        if blobIn is byte[] {
            string content = check string:fromBytes(blobIn);
            return "Blob from " + name + ", content is " + content;
        } else {
            return "Blob from " + name + " not found";
        }
    }

    resource function post query(string name, @http:Payload string greeting) returns @af:HttpOutput string|error {
        return "Hello from the query " + greeting + " " + name;
    }

    resource function get query/optional(string? name) returns string|error {
        if (name is string) {
            return "Hello from the optional query " + name;
        } else {
            return "Query not found but all good ;)";
        }
    }

    resource function get query/bool(boolean name) returns string|error {
        return "Hello from the bool query " + name.toString();
    }

    resource function get query/floatt(float name) returns string|error {
        return "Hello from the float query " + name.toString();
    }

    resource function get query/arr(string[] name) returns string|error {
        string out = "";
        foreach string i in name {
            out += i + " ";
        }
        return "Hello from the arr query " + out;
    }

    resource function get query/arrOrNil(string[]|() name) returns string|error {
        if (name is string[]) {
            string out = "";
            foreach string i in name {
                out += i + " ";
            }
            return "Hello from the arr or nil query " + out;
        } else {
            return "Query arr not found but all good ;)";
        }
    }
    
    resource function post payload/formdata(@http:Payload byte[] image) returns string? {
        if image.length() > 0 {
            return "success";
        }
    }
    
    resource function post payload/mimeformdata(@http:Payload mime:Entity image) returns int|error {
        mime:Entity[] bodyParts = check image.getBodyParts();
        mime:Entity bodyPart = bodyParts[0];
        byte[] byteArray = check bodyPart.getByteArray();
        return byteArray.length();
    }
}

@af:QueueTrigger {
    queueName: "queue2"
}
service "queue" on new af:QueueListener() {
    remote function onMessage(string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg;
    }
}

@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();

service "cosmos" on cosmosEp {
    remote function onUpdated(DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        string id = inMsg[0].id;
        return "helloo " + id;
    }
}

@af:TimerTrigger {schedule: "*/10 * * * * *"}
listener af:TimerListener timerListener = new af:TimerListener();

service "timer" on timerListener {
    remote function onTrigger(af:TimerMetadata inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg.IsPastDue.toString();
    }
}

@af:QueueTrigger {
    queueName: "queue4"
}

listener af:QueueListener queueListener1 = new af:QueueListener();

service "queue-input" on queueListener1 {
    remote function onMessage(string inMsg, @af:CosmosDBInput {
                                  connectionStringSetting: "CosmosDBConnection",
                                  databaseName: "db1",
                                  collectionName: "c2",
                                  sqlQuery: "SELECT * FROM Items"
                              } DBEntry[] input1) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg + " " + input1[0].id;
    }
}

@af:BlobTrigger {
    path: "bpath1/{name}"
}
listener af:BlobListener blobListener = new af:BlobListener();

service "blob" on blobListener {
    remote function onUpdated(byte[] blobIn, @af:BindingName {} string name) returns @af:BlobOutput {
        path: "bpath1/newBlob"
    } byte[]|error {
        return blobIn;
    }
}
