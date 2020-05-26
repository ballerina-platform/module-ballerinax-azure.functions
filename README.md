# Ballerina Azure Functions Extension

Annotation based Azure Functions extension implementation for Ballerina. 

## Azure Setup

* An Azure "Function App" needs to be created in a given resource group with the following requirements
   - Runtime stack - "Java"
   - Hosting operating system - "Windows" (default; Linux is not supported in Azure for custom handlers at the moment)

## Supported Annotations:

### @azure.functions:Function

#### Custom 'host.json'

A custom [host.json](https://docs.microsoft.com/en-us/azure/azure-functions/functions-host-json) file for the functions deployment can be optionally provided by placing a 'host.json' file in the current working directory where the Ballerina build is done. The required host.json properties are provided/overridden by the values derived from the source code by the compiler extension. 

#### Usage Sample:

```ballerina
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
                   @af:TwilioSmsOutput { fromNumber: "+1xxxxxxxxxx" } af:TwilioSmsOutputBinding tb)
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
```

The output of the Ballerina build is as follows:

```bash
$ ballerina build x.bal 
Compiling source
	x.bal

Generating executables
	x.jar
	@azure.functions:Function: f1, f2, f3, f4, f5, f6, f7, f8

	Run the following command to deploy Ballerina Azure Functions:
	az functionapp deployment source config-zip -g <resource_group> -n <function_app_name> --src azure-functions.zip
```


