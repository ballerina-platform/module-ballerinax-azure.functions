import ballerinax/azurefunctions;
import ballerina/system;

@azurefunctions:Function
public function echo(azurefunctions:Context ctx, json input) returns json|error {
   return input;
}

@azurefunctions:Function
public function uuid(azurefunctions:Context ctx, json input) returns json|error {
   return system:uuid();
}

@azurefunctions:Function
public function ctxinfo(azurefunctions:Context ctx, json input) returns json|error {
   json result = { RequestID: ctx.getRequestId(),
                   DeadlineMS: ctx.getDeadlineMs(),
                   InvokedFunctionArn: ctx.getInvokedFunctionArn(),
                   TraceID: ctx.getTraceId(),
                   RemainingExecTime: ctx.getRemainingExecutionTime() };
   return result;
}
