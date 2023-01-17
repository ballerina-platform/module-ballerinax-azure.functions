import ballerinax/azure_functions as af;

public type Test record {
    string greet;
};

service / on new af:HttpListener() {
    resource function get products/[string id](map<string>[] name) returns string {
        return name.toString();
    }
}
