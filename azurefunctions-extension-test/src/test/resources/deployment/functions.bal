import ballerina/http;
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
                   @af:BlobOutput { path: "bpath1/{Query.name}" } af:BytesOutputBinding bb)
                   returns @af:HTTPOutput string|error {
  bb.value = req.body;
  return "Blob: " + req.query["name"].toString() + " Content: " + bb?.value.toString();
}

@af:Function
public function f9(@af:HTTPTrigger { } af:HTTPRequest req, 
                   @af:TwilioSmsOutput { fromNumber: "+12069845840" } af:TwilioSmsOutputBinding tb)
                   returns @af:HTTPOutput string|error {
  tb.to = req.query["to"].toString();
  tb.body = req.body.toString();
  return "Message - to: " + tb?.to.toString() + " body: " + tb?.body.toString();
}

// executes every 10 seconds
@af:Function
public function f10(@af:TimerTrigger { schedule: "*/10 * * * * *" } json triggerInfo, 
                   @af:QueueOutput { queueName: "queue3" } af:StringOutputBinding msg) 
                   returns error? {
  msg.value = triggerInfo.toString();
}
