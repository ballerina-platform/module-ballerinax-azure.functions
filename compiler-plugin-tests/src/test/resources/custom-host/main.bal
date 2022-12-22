import ballerinax/azure_functions as af;
import ballerina/http;

listener af:HttpListener ep1 = new ();

service "hello" on ep1 {
    resource function get test1(@http:Header string host) returns string {
        return host;
    }
}

