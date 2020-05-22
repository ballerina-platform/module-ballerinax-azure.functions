# Ballerina Azure Functions Extension

Annotation based Azure Functions extension implementation for Ballerina. 

## Supported Annotations:

### @azure.functions:Function

### Annotation Usage Sample:

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
                   returns @af:HTTPOutput string|error {
  msg.value = req.body;
  return "Request: " + req.toString();
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

// executes every 10 seconds
@af:Function
public function f7(@af:TimerTrigger { schedule: "*/10 * * * * *" } json triggerInfo, 
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
	@azure.functions:Function: f1, f2, f3, f4, f5, f6, f7

	Run the following command to deploy Ballerina Azure Functions:
	az functionapp deployment source config-zip -g <resource_group> -n <function_app_name> --src azure-functions.zip
```


