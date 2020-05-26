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

# @azurefunctions:Function annotation.
public const annotation Function on function;

public type AUTH_LEVEL "anonymous"|"function"|"admin";

# HTTPTrigger annotation configuration.
# 
# + authLevel - The authentication level of the function
public type HTTPTriggerConfiguration record {|
    AUTH_LEVEL authLevel?;
|};

# @azurefunctions:HTTPTrigger annotation.
public const annotation HTTPTriggerConfiguration HTTPTrigger on parameter;

# @azurefunctions:HTTPOutput annotation
public const annotation HTTPOutput on parameter, return;

# Queue annotation configuration.
# 
# + queueName - The queue name
# + connection - The storage connection
public type QueueConfiguration record {|
    string queueName;
    string connection?;
|};

# @azurefunctions:QueueOutput annotation.
public const annotation QueueConfiguration QueueOutput on parameter, return;

# @azurefunctions:QueueOutput annotation.
public const annotation QueueConfiguration QueueTrigger on parameter;

# TimerTrigger annotation configuration.
# 
# + schedule - The CRON expression representing the timer schedule.
# + runOnStartup - The flag to state if the timer should be started on a runtime restart
public type TimerTriggerConfiguration record {|
    string schedule;
    boolean runOnStartup = true;
|};

# @azurefunctions:TimerTrigger annotation.
public const annotation TimerTriggerConfiguration TimerTrigger on parameter;

# Blob annotation configuration.
# 
# + path - The blob container path
# + connection - The storage connection
public type BlobConfiguration record {|
    string path;
    string connection?;
|};

# @azurefunctions:BlobTrigger annotation.
public const annotation BlobConfiguration BlobTrigger on parameter;

# @azurefunctions:BlobInput annotation.
public const annotation BlobConfiguration BlobInput on parameter;

# @azurefunctions:BlobOutput annotation.
public const annotation BlobConfiguration BlobOutput on parameter;

# Twilio annotation configuration.
# 
# + accountSidSetting - The app setting which holds the Twilio Account Sid
# + authTokenSetting - The app setting which holds the Twilio authentication token
# + fromNumber - The phone number the SMS is sent from
public type TwilioSmsConfiguration record {|
    string accountSidSetting = "AzureWebJobsTwilioAccountSid";
    string authTokenSetting	= "AzureWebJobsTwilioAuthToken";
    string fromNumber;
|};

# @azurefunctions:TwilioSmsOutput annotation.
public const annotation TwilioSmsConfiguration TwilioSmsOutput on parameter;

# BindingName annotation configuration.
# 
# + name - The binding name
public type BindingNameConfiguration record {|
    string name?;
|};

# @azurefunctions:BindingName annotation.
public const annotation BindingNameConfiguration BindingName on parameter;

