import ballerinax/azure.functions as af;

service / on new af:HttpListener() {
    resource function get hello() returns string {
        return "Hello, World!";
    }
}
