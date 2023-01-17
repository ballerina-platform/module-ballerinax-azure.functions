import ballerinax/azure_functions as af;

@af:BlobTrigger {
    path: "bpath1/{name}"
}
listener af:BlobListener blobListener = new af:BlobListener();

service "blob" on blobListener {
    remote function onUpdated (byte[] blobIn, @af:BindingName { } string name) returns byte[]|error {
        return blobIn;
    }
}
