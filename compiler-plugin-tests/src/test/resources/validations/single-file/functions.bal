import ballerinax/azure_functions as af;

// HTTP request/response with no authentication
@af:Function
public isolated function hello(@af:HTTPTrigger { authLevel: "anonymous" } string payload) 
                      returns @af:HttpOutput string|error {
    return "Hello, " + payload + "!";
}
