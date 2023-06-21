// Copyright (c) 2023 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerinax/azure.functions as af;
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

