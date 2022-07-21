public type RemoteService QueueService|CosmosService|TimerService|BlobService;


public type QueueService distinct service object {
    // remote function onMessage(anydata payload) returns anydata|error?;
};


public type CosmosService distinct service object {
    // remote function onUpdated(anydata payload) returns anydata|error?;
};


public type TimerService distinct service object {
    // remote function onTrigger(anydata payload) returns anydata|error?;
};


public type BlobService distinct service object {
    // remote function onTrigger(anydata payload) returns anydata|error?;
};


