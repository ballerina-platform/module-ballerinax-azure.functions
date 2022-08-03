import ballerinax/azure_functions as af;

listener af:HTTPListener ep = new ();

public type DBEntry record {
    string id;
};

type Person record {
    string name;
    int age;
};

// @af:HTTPTest
service "hello" on ep {
    resource function post .(@af:Payload string greeting) returns @af:HTTPOutput string {
        return "Hello from . path ";
    }
    resource function post httpResTest1(@af:Payload string greeting) returns @af:HTTPOutput af:Unauthorized {
        af:Unauthorized unauth = {
            body: "Helloworld.....",
            mediaType: "application/account+json",
            headers: {
                "Location": "/myServer/084230"
            }
        };
        return unauth;
    }

    resource function post httpResTest2(@af:Payload string greeting) returns @af:HTTPOutput af:Ok {
        af:Ok ok = {body: "Helloworld....."};
        return ok;
    }
    resource function post httpResTest3(@af:Payload string greeting) returns @af:HTTPOutput af:InternalServerError {
        af:InternalServerError err = {
            body: "Helloworld.....",
            headers: {
                "content-type": "application/json+id",
                "Location": "/myServer/084230"
            }
        };
        return err;
    }
    resource function post httpResTest4(@af:Payload string greeting) returns @af:HTTPOutput af:InternalServerError {
        af:InternalServerError err = {};
        return err;
    }

    resource function post foo(@af:Payload string greeting) returns @af:HTTPOutput string {
        return "Hello from foo path " + greeting;
    }

    resource function post foo/[string bar](@af:Payload string greeting) returns @af:HTTPOutput string {
        return "Hello from foo param " + bar;
    }

    resource function post foo/bar(@af:Payload string greeting) returns @af:HTTPOutput string {
        return "Hello from foo bar res";
    }

    resource function post query(string name, @af:Payload string greeting) returns @af:HTTPOutput string|error {
        return "Hello from the query " + greeting + " " + name;
    }

    resource function post db(@af:Payload string greeting, @af:CosmosDBInput {
                                  connectionStringSetting: "CosmosDBConnection",
                                  databaseName: "db1",
                                  collectionName: "c2",
                                  sqlQuery: "SELECT * FROM Items"
                              } DBEntry[] input1) returns @af:HTTPOutput string|error {
        return "Hello " + greeting + input1[0].id;
    }

    resource function post payload/jsonToRecord(@af:Payload Person greeting) returns @af:HTTPOutput string|error {
        return "Hello from json to record " + greeting.name;
    }

    resource function post payload/jsonToJson(@af:Payload json greeting) returns @af:HTTPOutput string|error {
        string name = check greeting.name;
        return "Hello from json to json " + name;
    }

    resource function post payload/xmlToXml(@af:Payload xml greeting) returns @af:HTTPOutput string|error {
        return greeting.toJsonString();
    }

    resource function post payload/textToString(@af:Payload string greeting) returns @af:HTTPOutput string|error {
        return greeting;
    }

    resource function post payload/textToByte(@af:Payload byte[] greeting) returns @af:HTTPOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function post payload/octaToByte(@af:Payload byte[] greeting) returns @af:HTTPOutput string|error {
        return string:fromBytes(greeting);
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

