import ballerinax/azure_functions as af;
import ballerina/http;

listener af:HttpListener ep1 = new ();
@http:ServiceConfig{treatNilableAsOptional: true, host : "b7a.default"}
service "hello" on ep1 {
    resource function get test1(@http:Header string? hoste) returns string? {
        return hoste;
    }
}

listener af:HttpListener ep2 = new ();
@http:ServiceConfig{host : "b7a.default"}
service "hello" on ep2 {
    resource function get test1(@http:Header string? hoste) returns string? {
        return hoste;
    }
}

listener af:HttpListener ep3 = new ();
@http:ServiceConfig{treatNilableAsOptional: true}
service "hello" on ep3 {
    resource function get test1(@http:Header string? hoste) returns string? {
        return hoste;
    }
}

