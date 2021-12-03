import ballerinax/azure_functions as af;

// HTTP request/response with no authentication
@af:Function
public function hello(@af:HTTPTrigger { authLevel: "anonymous" } string payload) 
                      returns @af:HTTPOutput string|error {
    return "Hello, " + payload + "!";
}
