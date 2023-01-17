import ballerinax/azure_functions as af;

service / on new af:HttpListener() {
    resource function get products() returns string {
        return "hello";
    }
    
    remote function onUpdated (byte[] blobIn, @af:BindingName { } string name) returns @af:BlobOutput { path: "bpath1/newBlob" } byte[]|error {
        return blobIn;
    }
     
    function test() {
        
    }
}
