import ballerinax/azure_functions as af;
import ballerina/http;
import ballerina/io;
import ballerina/lang.value;
import ballerina/regex;
import ballerina/test;

public type TestRecord record {
    string test1;
    string test2;
};

service /http  on new af:HttpListener() {
    resource function get path1/[int x]() returns string { //fails
        return "Hello, " + x.toBalString() + "!";
    }

    resource function get path1/[int x]/[int y]() returns string { //not tested
        return "Hello, " + x.toString() + y.toString() + "!";
    }

    resource function get query(int name) returns string { //works
        return "Hello, " + name.toString() + "!";
    }

    resource function post payload(@http:Payload TestRecord payload, string name) returns string { // works
        return "Hello, " + name + "!" + payload.toString();
    }

    resource function get query/optionals(int? age) returns string {
        if (age is ()) {
            return "age not specified";
        }
        return "Hello from the query "+ age.toString();
    }

    resource function post payload/optionals(@http:Payload TestRecord? payload) returns string { // empty 201, invalid payload 400, payload 201 
        if (payload is ()) {
            return "payload not specified";
        }
        return "Hello from the query "+ payload.toString();
    }
}

@test:Config {}
function retrFromAnnotField1() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/resources/httpHeaderTest.json";
    string readString = check io:fileReadString(jsonFilePath);
    string replacedString = regex:replaceAll(readString, "(FUNC_NAME)", "retrFromAnnotField");
    json readJson = check value:fromJsonString(replacedString);
    json resp = check clientEndpoint->post("/post-httpHeader-retrFromAnnotField", readJson);
    json expectedResp = {"Outputs":{"resp":{"statusCode":"201", "headers":{"Content-Type":"text/plain"}, "body":"text/plain"}}, "Logs":[], "ReturnValue":null};
    test:assertEquals(resp, expectedResp);
}
