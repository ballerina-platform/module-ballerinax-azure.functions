import ballerinax/azure_functions as af;

public type Test record {
    string greet;
};

service / on new af:HttpListener() {
    resource function get products/[string id](Test name) returns string {
        return name.toString();
    }
}
