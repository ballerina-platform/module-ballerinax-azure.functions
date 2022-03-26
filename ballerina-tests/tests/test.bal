import ballerina/test;
import ballerina/io;
import ballerina/http;

@test:Config { }
function testHelloWorld() returns error? {
    final http:Client clientEndpoint = check new ("http://localhost:3000");
    string jsonFilePath = "./tests/request.json";
    json readJson = check io:fileReadJson(jsonFilePath);
    json resp = check clientEndpoint->post("/hello", readJson);
    json expectedResp = {"Outputs":{},"Logs":[],"ReturnValue":"Hello, Jack!"};
    test:assertEquals(resp, expectedResp);
}
