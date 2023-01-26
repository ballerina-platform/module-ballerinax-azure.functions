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
