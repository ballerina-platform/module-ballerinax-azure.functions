import ballerinax/azure_functions as af;
import ballerina/http;
public listener http:Listener __testListener=af:hl ;type PersonOptionalGenerated Person? ;type PersonArrayGenerated         Person[] ;service http:Service / on __testListener {resource function 'default hello (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap helloHandler(params),params);check caller->respond(response);}resource function 'default fromHttpToQueue (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap fromHttpToQueueHandler(params),params);check caller->respond(response);}resource function 'default fromQueueToQueue (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap fromQueueToQueueHandler(params),params);check caller->respond(response);}resource function 'default fromBlobToQueue (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap fromBlobToQueueHandler(params),params);check caller->respond(response);}resource function 'default httpTriggerBlobInput (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerBlobInputHandler(params),params);check caller->respond(response);}resource function 'default httpTriggerBlobOutput (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerBlobOutputHandler(params),params);check caller->respond(response);}resource function 'default sendSMS (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap sendSMSHandler(params),params);check caller->respond(response);}resource function 'default cosmosDBToQueue1 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap cosmosDBToQueue1Handler(params),params);check caller->respond(response);}resource function 'default cosmosDBToQueue2 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap cosmosDBToQueue2Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBInput1 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBInput1Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBInput2 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBInput2Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBInput3 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBInput3Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBOutput1 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBOutput1Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBOutput2 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBOutput2Handler(params),params);check caller->respond(response);}resource function 'default httpTriggerCosmosDBOutput3 (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap httpTriggerCosmosDBOutput3Handler(params),params);check caller->respond(response);}resource function 'default queuePopulationTimer (http:Caller caller,http:Request request)returns error? {
http:Response response =new;af:HandlerParams params ={request,response};af:handleFunctionResposne(trap queuePopulationTimerHandler(params),params);check caller->respond(response);}}public function helloHandler (af:HandlerParams params)returns error? {
string v1=check hello(check af:getBodyFromHTTPInputData(params,"payload"));
()  v2=check af:setStringReturn(params,v1);
}public function fromHttpToQueueHandler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
af:HTTPBinding  v2=fromHttpToQueue(check af:createContext(params,true),check af:getHTTPRequestFromInputData(params,"req"),v1);
()  v3=check af:setStringOutput(params,"msg",v1);
()  v4=check af:setHTTPReturn(params,v2);
}public function fromQueueToQueueHandler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
()  v2=fromQueueToQueue(check af:createContext(params,true),check af:getJsonStringFromInputData(params,"inMsg"),v1);
()  v3=check af:setStringOutput(params,"outMsg",v1);
}public function fromBlobToQueueHandler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
error?  v2=fromBlobToQueue(check af:createContext(params,true),check af:getBytesFromInputData(params,"blobIn"),check af:getStringFromMetadata(params,"name"),v1);
()  v3=check af:setStringOutput(params,"outMsg",v1);
}public function httpTriggerBlobInputHandler (af:HandlerParams params)returns error? {
string  v1=httpTriggerBlobInput(check af:getHTTPRequestFromInputData(params,"req"),check af:getOptionalBytesFromInputData(params,"blobIn"));
()  v2=check af:setStringReturn(params,v1);
}public function httpTriggerBlobOutputHandler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
string v2=check httpTriggerBlobOutput(check af:getHTTPRequestFromInputData(params,"req"),v1);
()  v3=check af:setBlobOutput(params,"bb",v1);
()  v4=check af:setStringReturn(params,v2);
}public function sendSMSHandler (af:HandlerParams params)returns error? {
af:TwilioSmsOutputBinding v1={};
string  v2=sendSMS(check af:getHTTPRequestFromInputData(params,"req"),v1);
()  v3=check af:setTwilioSmsOutput(params,"tb",v1);
()  v4=check af:setStringReturn(params,v2);
}public function cosmosDBToQueue1Handler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
()  v2=cosmosDBToQueue1(<Person[] >check af:getBallerinaValueFromInputData(params,"req",PersonArrayGenerated),v1);
()  v3=check af:setStringOutput(params,"outMsg",v1);
}public function cosmosDBToQueue2Handler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
()  v2=cosmosDBToQueue2(check af:getJsonFromInputData(params,"req"),v1);
()  v3=check af:setStringOutput(params,"outMsg",v1);
}public function httpTriggerCosmosDBInput1Handler (af:HandlerParams params)returns error? {
string v1=check httpTriggerCosmosDBInput1(check af:getHTTPRequestFromInputData(params,"httpReq"),check af:getParsedJsonFromJsonStringFromInputData(params,"dbReq"));
()  v2=check af:setStringReturn(params,v1);
}public function httpTriggerCosmosDBInput2Handler (af:HandlerParams params)returns error? {
string v1=check httpTriggerCosmosDBInput2(check af:getHTTPRequestFromInputData(params,"httpReq"),<Person? >check af:getOptionalBallerinaValueFromInputData(params,"dbReq",PersonOptionalGenerated));
()  v2=check af:setStringReturn(params,v1);
}public function httpTriggerCosmosDBInput3Handler (af:HandlerParams params)returns error? {
string v1=check httpTriggerCosmosDBInput3(check af:getHTTPRequestFromInputData(params,"httpReq"),<        Person[] >check af:getBallerinaValueFromInputData(params,"dbReq",PersonArrayGenerated));
()  v2=check af:setStringReturn(params,v1);
}public function httpTriggerCosmosDBOutput1Handler (af:HandlerParams params)returns error? {
af:HTTPBinding v1={};
json  v2=httpTriggerCosmosDBOutput1(check af:getHTTPRequestFromInputData(params,"httpReq"),v1);
()  v3=check af:setHTTPOutput(params,"hb",v1);
()  v4=check af:setJsonReturn(params,v2);
}public function httpTriggerCosmosDBOutput2Handler (af:HandlerParams params)returns error? {
af:HTTPBinding v1={};
json  v2=httpTriggerCosmosDBOutput2(check af:getHTTPRequestFromInputData(params,"httpReq"),v1);
()  v3=check af:setHTTPOutput(params,"hb",v1);
()  v4=check af:setJsonReturn(params,v2);
}public function httpTriggerCosmosDBOutput3Handler (af:HandlerParams params)returns error? {
Person[]  v1=httpTriggerCosmosDBOutput3(check af:getHTTPRequestFromInputData(params,"httpReq"));
()  v2=check af:setBallerinaValueAsJsonReturn(params,v1);
}public function queuePopulationTimerHandler (af:HandlerParams params)returns error? {
af:StringOutputBinding v1={};
()  v2=queuePopulationTimer(check af:getJsonFromInputData(params,"triggerInfo"),v1);
()  v3=check af:setStringOutput(params,"msg",v1);
}
