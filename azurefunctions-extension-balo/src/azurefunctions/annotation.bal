// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# @azurefunctions:Function annotation
public const annotation Function on function;

public type AUTH_LEVEL "anonymous"|"function"|"admin";

# HTTPTrigger annotation configuration
# 
# + authLevel - The authentication level of the function
public type HTTPTriggerConfiguration record {|
    AUTH_LEVEL authLevel?;
|};

# @azurefunctions:HTTPTrigger annotation
public const annotation HTTPTriggerConfiguration HTTPTrigger on parameter;

# @azurefunctions:HTTPOutput annotation
public const annotation HTTPOutput on parameter, return;

# QueueOutput annotation configuration
# 
# + queueName - The queue name
# + connection - The storage connection
public type QueueOutputConfiguration record {|
    string queueName;
    string connection?;
|};

# @azurefunctions:QueueOutput annotation
public const annotation QueueOutputConfiguration QueueOutput on parameter, return;
