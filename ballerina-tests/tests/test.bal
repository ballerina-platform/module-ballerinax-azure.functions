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

import ballerina/http;
import ballerina/io;
import ballerina/lang.value;
import ballerina/regex;
import ballerina/test;

@test:Config {}
function retrFromAnnotField() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrFromAnnotField");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrFromAnnotField", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"text/plain"}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrFromParam() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrFromParam");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrFromParam", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"az-func-http-test.azurewebsites.net"}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrSingleVal() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrSingleVal");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrSingleVal", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"application/json"}, "body":15}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrArrVal() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrArrVal");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrArrVal", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"application/json"}, "body":20}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrArrValStr() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrArrValStr");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrArrValStr", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"12"}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrAsRecord() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrAsRecord");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrAsRecord", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"application/json"}, "body":105}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrNilable() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrNilable");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrNilable", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":201, "headers":{"Content-Type":"text/plain"}, "body":"az-func-http-test.azurewebsites.net"}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nnonTreatNilAsOptNilnoHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonTreatNilAsOpt-Nil-noHeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-nonTreatNilAsOpt-Nil-noHeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'hoste'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function treatNilAsOptnonNilnoHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "treatNilAsOpt-nonNil-noHeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-treatNilAsOpt-nonNil-noHeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'hoste'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function treatNilAsOptnonNilHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "treatNilAsOpt-nonNil-HeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-treatNilAsOpt-nonNil-HeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'hos'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function retrAsRecordNoField() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrAsRecordNoField");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrAsRecordNoField", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'Content-Type1'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function treatNilAsOptNilnoHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "treatNilAsOpt-Nil-noHeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-treatNilAsOpt-Nil-noHeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":202}},"Logs":[],"ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function treatNilAsOptNilHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "treatNilAsOpt-Nil-HeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-treatNilAsOpt-Nil-HeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":202}},"Logs":[],"ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonTreatNilAsOptnonNilnoHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonTreatNilAsOpt-nonNil-noHeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-nonTreatNilAsOpt-nonNil-noHeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'hoste'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonTreatNilAsOptnonNilHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonTreatNilAsOpt-nonNil-HeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-nonTreatNilAsOpt-nonNil-HeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":400, "body":"no header value found for 'hos'", "headers":{"Content-Type":"text/plain"}}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonTreatNilAsOptNilHeaderTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonTreatNilAsOpt-Nil-HeaderTest");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-nonTreatNilAsOpt-Nil-HeaderTest", readJson);
    json expectedResp = {"Outputs":{"outResp":{"statusCode":202}},"Logs":[],"ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testEscapeSequences() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/escape-seq.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello--hello-query", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from the hello-query"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testDefault() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/default.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/default-hello-all", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function getHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "get");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/get-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function putHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "put");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/put-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function patchHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "patch");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/patch-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function deleteHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "delete");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/delete-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function headHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "head");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/head-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function optionsHttpAccessorTest() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpAccessorTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(ACCESSOR_NAME)", "options");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/options-hello-httpAccessorTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 200, "headers": {"Content-Type": "text/plain"}, "body": "Hello from all"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testBaseDot() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/base-dot.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from . path "}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function httpResTest1() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpResTest1.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-httpResTest1", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 401,
                "body": "Helloworld.....",
                "headers": {"Location": "/myServer/084230", "Content-Type": "application/account+json"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function httpResTest2() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpResTest2.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-httpResTest2", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "body": "Helloworld.....",
                "headers": {"Content-Type": "text/plain"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function httpResTest3() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpResTest3.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-httpResTest3", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 500,
                "body": "Helloworld.....",
                "headers": {"Content-Type": "application/json+id", "Location": "/myServer/084230"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function httpResTest4() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpResTest4.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-httpResTest4", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 500,
                "headers": {}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest1() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest1");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest1", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "body": "alpha",
                "headers": {"Content-Type": "text/plain"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest2() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest2");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest2", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "application/xml"}, "body": "<book>The Lost World</book>"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest3() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest3");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest3", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "headers": {"Content-Type": "application/octet-stream"},
                "body": "yPHaytRgJPg+QjjylUHakEwz1fWPx/wXCW41JSmqYW8="
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest4() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest4");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest4", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "application/json"}, "body": 100}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest6() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest6");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest6", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "application/json"}, "body": 100}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest7() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest7");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest7", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "application/json"}, "body": true}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest8() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest8");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest8", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "body": {"a": {"b": 12, "c": "helloworld"}},
                "headers": {"Content-Type": "application/json"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest9() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest9");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest9", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "body": [{"a": {"b": 12, "c": "helloworld"}}, {"b": 1100}],
                "headers": {"Content-Type": "application/json"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest10() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest10");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest10", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "body": [{"a": {"b": 12, "c": "helloworld"}}, {"b": 12}],
                "headers": {"Content-Type": "application/json"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonHttpResTest11() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonHttpResTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "nonHttpResTest11");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest11", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "body": [[{"a": {"b": 12, "c": "helloworld"}}, {"b": 12}], [{"a": {"b": 14, "c": "helloworld"}}, {"b": 100}]],
                "headers": {"Content-Type": "application/json"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function nonReturnTest1() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/nonReturnTest1.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-nonHttpResTest1", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 202
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSimpleResourcePath() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/res-path.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-foo", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from foo path Jack"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSimpleMultiResourcePath() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/res-path-param.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-foo-bar-2", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from foo bar res"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSimpleConflictingPathParam() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/res-path-conflict-param.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-foo-bar-1", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from foo param meow"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testRestPathParam() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/path-rest-param.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-restParamTest-bar", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hellow from rest param i"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSimpleMultiQueryPath() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-param.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-query", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from the query Jack test1"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testNonNilableQueryParam() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-param-nonNilable.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-nonNilableQueryParamTest", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 400, "headers": {"Content-Type": "text/plain"}, "body": "Error : no query param value found for 'foo'"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testOptionalQueryWithQuery() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-optional-with.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-optional", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Hello from the optional query test1"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryBool() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-bool.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-bool", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Hello from the bool query false"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryFloatt() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-float.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-floatt", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Hello from the float query 10.5"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryArr() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-arr.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-arr", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Hello from the arr query red green "
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryOptionalArrWithout() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-arr-optional-without.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-arrOrNil", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Query arr not found but all good ;)"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryOptionalArrWith() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-arr-optional-with.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-arrOrNil", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Hello from the arr or nil query red green "
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testOptionalQueryWithoutQuery() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-optional-without.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-query-optional", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {
                    "Content-Type": "text/plain"
                },
                "body": "Query not found but all good ;)"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSimpleQueue() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/queue-string.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/queue", readJson);
    json expectedResp = {"Outputs": {"outResp": "helloo aaaaa"}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testCosmosInputArr() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/cosmos-db-arr.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-db", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello Jackhello1"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testJsonJsonPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-json-json.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-jsonToJson", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from json to json Anjana"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testJsonRecordPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-json-record.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-jsonToRecord", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from json to record Anjana"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testXmlPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-xml-xml.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-xmlToXml", readJson);
    string xmlPayload = "\"<root>\\n  <name>Anjana<\\/name>\\n  <age>12<\\/age>\\n<\\/root>\"";
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": xmlPayload}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testTextStringPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-text-string.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-textToString", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "hello from byte\n"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testTextBytePayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-text-byte.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-textToByte", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "hello from byte\n"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testOctaBytePayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/payload-octa-byte.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-octaToByte", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "hello from byte arr\n"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testCosmosTrigger() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/trigger-cosmos-base.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/cosmos", readJson);
    json expectedResp = {"Outputs": {"outResp": "helloo ehee"}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testTimerTrigger() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/timer.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/timer", readJson);
    json expectedResp = {"Outputs": {"outResp": "helloo false"}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueueTrigger() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/blob-trigger.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/blobspread", readJson);
    json expectedResp = {"Outputs": {"outResp":"aGVsbG8gZnJvbSBieXRlCg==","outResp1":"world"}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueueInput() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/queue-input.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/queue-input", readJson);
    json expectedResp = {"Outputs": {"outResp": "helloo qqeeewwww hello1"}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

function replaceFuncName(string actual) {

}

@test:Config {}
function testOptionalOutputBinding() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-optional-out.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-optional-out", readJson);
    json expectedResp = {
        "Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello from optional output binding"}},
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testErrorPayloadNotFound() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/error-missing-payload.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-err-empty-payload", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 400,
                "body": "payload not found for the variable 'greeting'",
                "headers": {"Content-Type": "text/plain"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testErrorInvalidPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/error-invalid-payload.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-err-invalid-payload", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 400,
                "body": "incompatible type found: 'string",
                "headers": {"Content-Type": "text/plain"}
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testOptionalPayloadWithPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-optional-with-payload.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-optional-payload", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello, the payload found Jack"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testOptionalPayloadWithoutPayload() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-optional-without-payload.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-optional-payload", readJson);
    json expectedResp = {"Outputs": {"outResp": {"statusCode": 201, "headers": {"Content-Type": "text/plain"}, "body": "Hello, the payload wasn't set but all good ;)"}}, "Logs": [], "ReturnValue": null};
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testHttpBlobInput() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-query-blob-input.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-blobInput", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "headers": {"Content-Type": "text/plain"},
                "body": "Blob from hello.txt, content is hello from byte\n"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testHttpBlobInputOptional() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-query-blob-optional-input.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-blobInput", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "headers": {"Content-Type": "text/plain"},
                "body": "Blob from hello1.txt not found"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}


@test:Config {}
function testMultipartFormData() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-multipart-formdata.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-formdata", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "headers": {"Content-Type": "text/plain"},
                "body": "success"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testMultipartMimeFormData() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/http-multipart-formdata-mime.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/post-hello-payload-mimeformdata", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 201,
                "headers": {"Content-Type": "application/json"}, //TODO see if the implicit return is valid
                "body": 21553
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryMapJson() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-map-json.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-products", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "testjson"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testQueryArrFail() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/query-array-mixed.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-catalog", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 400,
                "headers": {"Content-Type": "text/plain"},
                "body": "Query param value parsing failed for 'name'"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testMultiOut() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-multiout", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello"
            },
            "outResp1": "world"
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testMultiOutRef() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out-ref.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-multiout-ref", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello1"
            },
            "outResp1": "world1"
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testSingleTuple() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out-single.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-singleout", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testThreeMultiOut() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out-three.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-multiout", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello"
            },
            "outResp1": "world",
            "outResp2": "YW5qYW5h"
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testMultiOutDiffValue() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out-val.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-multiout-val", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello"
            },
            "outResp1": "world"
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testMultiOutDiffMethod() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/multi-out-method.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/get-hello-multiout-method", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "hello"
            },
            "outResp1": "world"
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}

@test:Config {}
function testCustomFunctionName() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/custom-func-name.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/customAnnotFunc", readJson);
    json expectedResp = {
        "Outputs": {
            "outResp": {
                "statusCode": 200,
                "headers": {"Content-Type": "text/plain"},
                "body": "Hello World!"
            }
        },
        "Logs": [],
        "ReturnValue": null
    };
    test:assertEquals(resp, expectedResp);
}
