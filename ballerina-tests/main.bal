import ballerinax/azure_functions as af;

listener af:HTTPListener ep = new ();

public type DBEntry record {
    string id;
};

type Person record {
    string name;
    int age;
};

listener af:HTTPListener ep1 = new ();

service /hello\- on ep1 {

    resource function post hello\-query() returns string|error {
        return "Hello from the hello-query";
    }
}

service /hello on ep {
    resource function default all() returns @af:HttpOutput string {
        return "Hello from all";
    }

    resource function post optional/out(@af:Payload string greeting) returns string {
        return "Hello from optional output binding";
    }

    resource function post optional/payload(@af:Payload string? greeting) returns string {
        if (greeting is string) {
            return "Hello, the payload found " + greeting;
        }
        return "Hello, the payload wasn't set but all good ;)";
    }

    resource function post .(@af:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from . path ";
    }
    resource function post httpResTest1(@af:Payload string greeting) returns @af:HttpOutput af:Unauthorized {
        af:Unauthorized unauth = {
            body: "Helloworld.....",
            mediaType: "application/account+json",
            headers: {
                "Location": "/myServer/084230"
            }
        };
        return unauth;
    }

    resource function post httpResTest2(@af:Payload string greeting) returns @af:HttpOutput af:Ok {
        af:Ok ok = {body: "Helloworld....."};
        return ok;
    }
    resource function post httpResTest3(@af:Payload string greeting) returns @af:HttpOutput af:InternalServerError {
        af:InternalServerError err = {
            body: "Helloworld.....",
            headers: {
                "Content-Type": "application/json+id",
                "Location": "/myServer/084230"
            }
        };
        return err;
    }
    resource function post httpResTest4(@af:Payload string greeting) returns @af:HttpOutput af:InternalServerError {
        af:InternalServerError err = {};
        return err;
    }

    resource function post foo(@af:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo path " + greeting;
    }

    resource function post foo/[string bar](@af:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo param " + bar;
    }

    resource function post foo/bar(@af:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo bar res";
    }

    resource function post db(@af:Payload string greeting, @af:CosmosDBInput {
                                  connectionStringSetting: "CosmosDBConnection",
                                  databaseName: "db1",
                                  collectionName: "c2",
                                  sqlQuery: "SELECT * FROM Items"
                              } DBEntry[] input1) returns @af:HttpOutput string|error {
        return "Hello " + greeting + input1[0].id;
    }

    resource function post payload/jsonToRecord(@af:Payload Person greeting) returns @af:HttpOutput string|error {
        return "Hello from json to record " + greeting.name;
    }

    resource function post payload/jsonToJson(@af:Payload json greeting) returns @af:HttpOutput string|error {
        string name = check greeting.name;
        return "Hello from json to json " + name;
    }

    resource function post payload/xmlToXml(@af:Payload xml greeting) returns @af:HttpOutput string|error {
        return greeting.toJsonString();
    }

    resource function post payload/textToString(@af:Payload string greeting) returns @af:HttpOutput string|error {
        return greeting;
    }

    resource function post payload/textToByte(@af:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function post payload/octaToByte(@af:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function get err/empty/payload(@af:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from get empty payload";
    }

    resource function post err/invalid/payload(@af:Payload string greeting) returns @af:HttpOutput string {
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

    resource function post httpResTest5() returns af:StatusCodeResponse {
        af:InternalServerError err = {};
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

    resource function get blobInput(@af:Payload string greeting, string name, @af:BlobInput {path: "bpath1/{Query.name}"} byte[]? blobIn) returns string|error {
        if blobIn is byte[] {
            string content = check string:fromBytes(blobIn);
            return "Blob from " + name + ", content is " + content;
        } else {
            return "Blob from " + name + " not found";
        }
    }

    resource function post query(string name, @af:Payload string greeting) returns @af:HttpOutput string|error {
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
}

@af:QueueTrigger {
    queueName: "queue2"
}
service "queue" on new af:QueueListener() {
    remote function onMessage(@af:Payload string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg;
    }
}

@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();

service "cosmos" on cosmosEp {
    remote function onUpdated(@af:Payload DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        string id = inMsg[0].id;
        return "helloo " + id;
    }
}

@af:TimerTrigger {schedule: "*/10 * * * * *"}
listener af:TimerListener timerListener = new af:TimerListener();

service "timer" on timerListener {
    remote function onTrigger(@af:Payload af:TimerMetadata inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg.IsPastDue.toString();
    }
}

@af:QueueTrigger {
    queueName: "queue4"
}

listener af:QueueListener queueListener1 = new af:QueueListener();

service "queue-input" on queueListener1 {
    remote function onMessage(@af:Payload string inMsg, @af:CosmosDBInput {
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
    remote function onUpdated(@af:Payload byte[] blobIn, @af:BindingName {} string name) returns @af:BlobOutput {
        path: "bpath1/newBlob"
    } byte[]|error {
        return blobIn;
    }
}
