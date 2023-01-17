import ballerinax/azure_functions as af;

public type Test record {
    string greet;
};

service / on new af:HttpListener() {
    resource function get err1(int|string a) returns string {
        return "done";
    }

    resource function get err2(int|string[]|float b) returns string {
        return "done";
    }

    resource function get err3(string? b, map<int> a) returns string {
        return "done";
    }

    resource function get err4(map<string>? c, map<json> d) returns string {
        return "done";
    }
    
    resource function get err5(int[]|json c) returns string {
        return "done";
    }

    resource function get err6(map<int>[]?  c) returns string {
        return "done";
    }

    resource function get err7(map<json>[]?  c) returns string {
        return "done";
    }
}
