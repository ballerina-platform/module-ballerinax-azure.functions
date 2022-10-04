import ballerinax/azure_functions as af;
import ballerina/http;

listener af:HttpListener ep = new ();

public type DBEntry record {
    string id;
};

type Person record {
    string name;
    int age;
};

listener af:HttpListener ep1 = new ();

service /hello\- on ep1 {

    resource function post hello\-query() returns string|error {
        return "Hello from the hello-query";
    }
}

// @af:HTTPTest
service /hello on ep {
    resource function default all() returns @af:HttpOutput string {
        return "Hello from all ";
    }
    
    resource function post optional(@http:Payload string greeting) returns string {
        return "Hello from optional output bindin";
    }
    
    resource function post .(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from . path ";
    }

    resource function post foo(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo path " + greeting;
    }

    resource function post foo/[string bar](@http:Payload string greeting) returns @af:HttpOutput string  {
        return "Hello from foo param " + bar;
    }

    resource function post foo/bar(@http:Payload string greeting) returns @af:HttpOutput string  {
        return "Hello from foo bar res";
    }

    resource function post query(string name, @http:Payload string greeting) returns @af:HttpOutput string|error {
                return "Hello from the query " + greeting + " " + name;
    }

    resource function post db(@http:Payload string greeting, @af:CosmosDBInput {
        connectionStringSetting: "CosmosDBConnection",databaseName: "db1",
        collectionName: "c2", sqlQuery: "SELECT * FROM Items"} DBEntry[] input1) returns @af:HttpOutput string|error {
            return "Hello " + greeting + input1[0].id;
    }

    resource function post payload/jsonToRecord (@http:Payload Person greeting) returns @af:HttpOutput string|error {
        return "Hello from json to record " + greeting.name;
    }

    resource function post payload/jsonToJson (@http:Payload json greeting) returns @af:HttpOutput string|error {
        string name = check greeting.name;
        return "Hello from json to json "+ name;
    }

    resource function post payload/xmlToXml (@http:Payload xml greeting) returns @af:HttpOutput string|error {
        return greeting.toJsonString();
    }

    resource function post payload/textToString (@http:Payload string greeting) returns @af:HttpOutput string|error {
        return greeting;
    }

    resource function post payload/textToByte (@http:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }

    resource function post payload/octaToByte (@http:Payload byte[] greeting) returns @af:HttpOutput string|error {
        return string:fromBytes(greeting);
    }
}

@af:QueueTrigger {
    queueName: "queue2"
}
listener af:QueueListener queueListener = new af:QueueListener();

service "queue" on queueListener {
    remote function onMessage (string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
                return "helloo "+ inMsg;
    }
}

@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();

service "cosmos" on cosmosEp {
    remote function onUpdated (DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        string id = inMsg[0].id;
        return "helloo "+ id;
    }
}

@af:TimerTrigger { schedule: "*/10 * * * * *" } 
listener af:TimerListener timerListener = new af:TimerListener();
service "timer" on timerListener {
    remote function onTrigger (af:TimerMetadata inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
            return "helloo "+ inMsg.IsPastDue.toString();
    }
}

@af:BlobTrigger {
    path: "bpath1/{name}"
}
listener af:BlobListener blobListener = new af:BlobListener();

service "blob" on blobListener {
    remote function onUpdated (byte[] blobIn, @af:BindingName { } string name) returns @af:BlobOutput { 
        path: "bpath1/newBlob" } byte[]|error {
        return blobIn;
    }
}
