// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

//TODO See if unused methods are required for a listener declaration
public class HttpListener {
    ResourceService[] httpServices;

    public isolated function init() returns error? {
        self.httpServices = [];
    }
    
    public function attach(HttpService svc, string[]|string? name = ()) returns error? {
        HttpToAzureAdaptor adaptor = new(svc);
        string[] resourcePaths = adaptor.getAzureFunctionNames();
        foreach string resourcePath in resourcePaths{
            ResourceService httpService = new (adaptor);
            check httpListener.attach(<ResourceService>httpService, resourcePath);
        }
    }

    public isolated function detach(HttpService svc) returns error? {
    }

    public function 'start() returns error? {
        check httpListener.'start();
    }

    public isolated function gracefulStop() returns error? {
    }

    public isolated function immediateStop() returns error? {
    }
}
public type HttpService distinct service object {

};


