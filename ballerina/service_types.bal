// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/os;
import ballerina/lang.'int as ints;

//TODO rename file and docs
http:Listener httpListener = check new (check ints:fromString(os:getEnv("FUNCTIONS_CUSTOMHANDLER_PORT")));

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


