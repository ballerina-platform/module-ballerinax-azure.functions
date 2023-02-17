import ballerinax/azure_functions as af;
import ballerina/http;

listener af:HttpListener ep = new ();

public type DBEntry record {
    string id;
};

public type Person record {
    string name;
    int age;
};

type Return [@af:HttpOutput http:Created, @af:QueueOutput {queueName: "people"} string];

listener af:HttpListener ep1 = new ();

service /hello\- on ep1 {

    resource function post hello\-query() returns string|error {
        return "Hello from the hello-query";
    }
}

service /helo on new af:HttpListener() {

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

    resource function post restParamTest/[string... bar](@http:Payload string greeting) returns @af:HttpOutput string  {
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
    
    resource function get tuples(string name) returns [@af:HttpOutput string,@af:QueueOutput{queueName: "queue3"} string] {
        return ["Hello, " + name + "!","To Queue"];
    }
    
    resource function post queue(@http:Payload Person person) returns Return {
        http:Created httpRes = {
            body: person.name + " Added to the Queue!"
        };
        return [httpRes, person.name + " is " + person.age.toString() + " years old."];
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


@af:QueueTrigger {
    queueName: "queue21"
}
service "queue1" on new af:QueueListener() {
    remote function onMessage (string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
                return "helloo "+ inMsg;
    }
}


@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();

service "cosmos" on cosmosEp {
    remote function onUpdate (DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        string id = inMsg[0].id;
        return "helloo "+ id;
    }
}


@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
service "cosmos1" on new af:CosmosDBListener() {
    remote function onUpdate (DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
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


@af:TimerTrigger { schedule: "*/10 * * * * *" } 
service "timer1" on new af:TimerListener() {
    remote function onTrigger (af:TimerMetadata inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
            return "helloo "+ inMsg.IsPastDue.toString();
    }
}

@af:BlobTrigger {
    path: "bpath1/{name}"
}
listener af:BlobListener blobListener = new af:BlobListener();

service "blob" on blobListener {
    remote function onUpdate (byte[] blobIn, @af:BindingName { } string name) returns @af:BlobOutput { 
        path: "bpath1/newBlob" } byte[]|error {
        return blobIn;
    }
}

@af:BlobTrigger {
    path: "bpath1/{name}"
}

service "blob1" on new af:BlobListener() {
    remote function onUpdate (byte[] blobIn, @af:BindingName { } string name) returns @af:BlobOutput { 
        path: "bpath1/newBlob" } byte[]|error {
        return blobIn;
    }
}

service / on new http:Listener(9099) {

    // This function responds with `string` value `Hello, World!` to HTTP GET requests.
    resource function get greeting() returns string {
        return "Hello, World!";
    }
}


@af:HttpTrigger {authLevel: "function"}
listener af:HttpListener ep123 = new ();

service /hello123 on ep123 {
    resource function default all(@af:BlobInput {
                path: "path1",
                connection: "TestConnection"
            } byte[] image) returns @af:HttpOutput string {
        return "Hello from all ";
    }

    resource function default twillio() returns @af:TwilioSmsOutput{accountSidSetting:"AzureWebJobsTwilioAccountSid1", authTokenSetting:"AzureWebJobsTwilioAuthToken1", 'from:"012345", to:"3456"} string {
        return "Hello SMS";
    }
}

@af:QueueTrigger {
    queueName: "queue2",
    connection : "TestConnection"
}
listener af:QueueListener queueListener123 = new af:QueueListener();

service "queue123" on queueListener123 {
    remote function onMessage(string inMsg) returns @af:QueueOutput {queueName: "queue3", connection: "TestConnection"} string|error {
        return "helloo " + inMsg;
    }
}


@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2", createLeaseCollectionIfNotExists: true, leasesCollectionThroughput:400}
listener af:CosmosDBListener cosmosEp123 = new ();

service "cosmos123" on cosmosEp123 {
    remote function onUpdate(DBEntry[] inMsg, @af:CosmosDBInput{connectionStringSetting: "TestDBConn", databaseName: "testDB", collectionName: "TestCollection", sqlQuery: "SELECT * FROM ITEMS", partitionKey: "id",id: "1234"} DBEntry[] input) returns @af:CosmosDBOutput {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"} DBEntry[]|error {
        return inMsg;
    }
}


@af:TimerTrigger {schedule: "*/10 * * * * *", runOnStartup: true}
listener af:TimerListener timerListener123 = new af:TimerListener();

service "timer123" on timerListener123 {
    remote function onTrigger(af:TimerMetadata inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg.IsPastDue.toString();
    }
}

@af:BlobTrigger {
    path: "bpath1/{name}",
    connection: "TestConnection"
}
listener af:BlobListener blobListener123 = new af:BlobListener();

service "blob123" on blobListener123 {
    remote function onUpdate(byte[] blobIn, @af:BindingName string name) returns @af:BlobOutput {
        path: "bpath1/newBlob",
        connection: "TestConnection"
    } byte[]|error {
        return blobIn;
    }
}
 
