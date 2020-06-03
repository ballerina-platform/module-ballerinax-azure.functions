import ballerina/http;
import ballerina/system;
import ballerinax/azure.functions as af;

@af:Function
public function f1(@af:HTTPTrigger { authLevel: "anonymous" } string req, 
                   @af:HTTPOutput af:HTTPBinding hb) returns error? {
  hb.payload = "Hello!";
}

@af:Function
public function f2(@af:HTTPTrigger {} http:Request req) 
                   returns @af:HTTPOutput string|error {
  return "Hi - " + check <@untainted> req.getTextPayload();
}

@af:Function
public function f3(af:Context ctx, 
                   @af:HTTPTrigger {} af:HTTPRequest req, 
                   @af:QueueOutput { queueName: "queue1" } af:StringOutputBinding msg) 
                   returns @af:HTTPOutput af:HTTPBinding|error {
  msg.value = req.body;
  return { statusCode: 200, payload: "Request: " + req.toString() };
}

@af:Function
public function f4(af:Context ctx, 
                   @af:HTTPTrigger {} string req, 
                   @af:QueueOutput { queueName: "queue1" } af:StringOutputBinding msg) 
                   returns @af:HTTPOutput string|error {
  msg.value = req;
  ctx.log("Adding request to queue1: " + req);
  return "Added to queue1: " + req;
}

@af:Function
public function f5(af:Context ctx, 
                   @af:QueueTrigger { queueName: "queue1" } string inMsg,
                   @af:QueueOutput { queueName: "queue2" } af:StringOutputBinding outMsg) 
                   returns error? {
  ctx.log("In Message: " + inMsg);
  ctx.log("Metadata: " + ctx.metadata.toString());
  outMsg.value = inMsg;
}

@af:Function
public function f6(af:Context ctx, 
                   @af:BlobTrigger { path: "bpath1/{name}" } byte[] blobIn,
                   @af:BindingName { } string name,
                   @af:QueueOutput { queueName: "queue3" } af:StringOutputBinding outMsg) 
                   returns error? {
  ctx.log("Blob Message: " + blobIn.toString());
  outMsg.value = "Name: " + name + " Content: " + blobIn.toString();
}

@af:Function
public function f7(@af:HTTPTrigger { } af:HTTPRequest req, 
                   @af:BlobInput { path: "bpath1/{Query.name}" } byte[]? blobIn)
                   returns @af:HTTPOutput string|error {
  int length = 0;
  if blobIn is byte[] {
      length = blobIn.length();
  }
  return "Blob: " + req.query["name"].toString() + " Length: " + length.toString() + " Content: " + blobIn.toString();
}

@af:Function
public function f8(@af:HTTPTrigger { } af:HTTPRequest req, 
                   @af:BlobOutput { path: "bpath1/{Query.name}" } af:StringOutputBinding bb)
                   returns @af:HTTPOutput string|error {
  bb.value = req.body;
  return "Blob: " + req.query["name"].toString() + " Content: " + bb?.value.toString();
}

@af:Function
public function f9(@af:HTTPTrigger { } af:HTTPRequest req, 
                   @af:BlobInput { path: "bpath1/{Query.name}" } string? blobIn)
                   returns @af:HTTPOutput string|error {
  int length = 0;
  if blobIn is string {
      length = blobIn.length();
  }
  return "Blob: " + req.query["name"].toString() + " Length: " + length.toString() + " Content: " + blobIn.toString();
}

@af:Function
public function f10(@af:HTTPTrigger { } af:HTTPRequest req, 
                   @af:TwilioSmsOutput { fromNumber: "+12069845840" } af:TwilioSmsOutputBinding tb)
                   returns @af:HTTPOutput string|error {
  tb.to = req.query["to"].toString();
  tb.body = req.body.toString();
  return "Message - to: " + tb?.to.toString() + " body: " + tb?.body.toString();
}

public type Person record {
  string id;
  string name;
  int birthYear;
};

@af:Function
public function f11(@af:CosmosDBTrigger { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c1" } Person[] req, 
                    @af:QueueOutput { queueName: "queue3" } af:StringOutputBinding outMsg) {
  outMsg.value = req.toString();
}


@af:Function
public function f12(@af:CosmosDBTrigger { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2" } json req, 
                    @af:QueueOutput { queueName: "queue3" } af:StringOutputBinding outMsg) {
  outMsg.value = req.toString();
}

@af:Function
public function f13(@af:HTTPTrigger { } af:HTTPRequest httpReq, 
                    @af:CosmosDBInput { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c1", 
                                        id: "{Query.id}", partitionKey: "p1" } json dbReq)
                    returns @af:HTTPOutput string|error {
  return dbReq.toString();
}

@af:Function
public function f14(@af:HTTPTrigger { } af:HTTPRequest httpReq, 
                    @af:CosmosDBInput { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c1", 
                                        id: "{Query.id}", partitionKey: "p1" } Person? dbReq)
                    returns @af:HTTPOutput string|error {
  return dbReq.toString();
}

@af:Function
public function f15(@af:HTTPTrigger { route: "c1/{country}" } af:HTTPRequest httpReq, 
                    @af:CosmosDBInput { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c1", 
                                        sqlQuery: "select * from c1 where c1.country = {country}", 
                                        partitionKey: "p1" } Person[] dbReq)
                    returns @af:HTTPOutput string|error {
  return dbReq.toString();
}

@af:Function
public function f16(@af:HTTPTrigger { } af:HTTPRequest httpReq, @af:HTTPOutput af:HTTPBinding hb) 
                    returns @af:CosmosDBOutput { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", 
                                                 collectionName: "c1", partitionKey: "p1" } json {
  json entry = { id: system:uuid(), name: "John Doe", birthYear: 1980 };
  hb.payload = "Adding entry: " + entry.toString();
  return entry;
}

@af:Function
public function f17(@af:HTTPTrigger { } af:HTTPRequest httpReq, @af:HTTPOutput af:HTTPBinding hb) 
                    returns @af:CosmosDBOutput { connectionStringSetting: "CosmosDBConnection", databaseName: "db1", 
                                                 collectionName: "c1", partitionKey: "p1" } json {
  json entry = [{ id: system:uuid(), name: "John Doe A", birthYear: 1985 }, { id: system:uuid(), name: "John Doe B", birthYear: 1990 }];
  hb.payload = "Adding entries: " + entry.toString();
  return entry;
}

@af:Function
public function f18(@af:HTTPTrigger { } af:HTTPRequest httpReq) 
                    returns @af:CosmosDBOutput { connectionStringSetting: "CosmosDBConnection", 
                                                 databaseName: "db1", collectionName: "c1", 
                                                 partitionKey: "p1" } Person[] {
  Person[] persons = [];
  persons.push({id: system:uuid(), name: "Jack", birthYear: 2001});
  persons.push({id: system:uuid(), name: "Will", birthYear: 2005});
  return persons;
}

// executes every 10 seconds
@af:Function
public function f19(@af:TimerTrigger { schedule: "*/10 * * * * *" } json triggerInfo, 
                    @af:QueueOutput { queueName: "queue3" } af:StringOutputBinding msg) 
                    returns error? {
  msg.value = triggerInfo.toString();
}
