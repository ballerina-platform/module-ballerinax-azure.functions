import ballerinax/azure_functions as af;

service / on new af:HttpListener() {
    resource function get hello(string name) returns @af:BlobOutput {path: "output"} string {
        return name;
    }
}
