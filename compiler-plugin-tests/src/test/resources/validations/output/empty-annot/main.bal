import ballerinax/azure_functions as af;

service / on new af:HttpListener() {
    resource function get hello(string name) returns [@af:HttpOutput string, string] {
        return [name];
    }
}
