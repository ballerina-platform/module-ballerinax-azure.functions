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

# Represents details about the Timer trigger event
#
# + Schedule - Schedule the timer is being executed on  
# + ScheduleStatus - Status of the Schedule
# + IsPastDue - Weather the timer is past its due time
public type TimerMetadata record {
    TimerSchedule Schedule;
    anydata ScheduleStatus?;
    boolean IsPastDue;
};

# Represents the details about timer schedule
#
# + AdjustForDST - shows weather time is adjusted for DST
public type TimerSchedule record {
    boolean AdjustForDST;
};

type Payload record {
    map<HttpPayload|string> Data;
    Metadata Metadata;
};

type Metadata record {
    map<string> Query;
    map<string> Headers;
    Sys sys;
};

type Sys record {
    string MethodName;
    string UtcNow;
    string RandGuid;
};

type HttpPayload record {
    string Url;
    string Method;
    map<string> Query;
    map<anydata> Headers;
    map<anydata> Params;
    Identity[] Identities;
    anydata? Body?;
};

type Identity record {
};

# Twilion SMS output binding data.
# 
# + to - The SMS recipient phone number
# + body - The message body
public type TwilioSmsOutputBinding record {
    string to?;
    string body?;
};
