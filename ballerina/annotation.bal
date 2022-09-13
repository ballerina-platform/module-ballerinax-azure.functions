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

# @azurefunctions:Function annotation.
public const annotation Function on function; //Todo remove
public const annotation AZFunctionConfiguration AzureFunction on function;

public type AZFunctionConfiguration record {|
    string name;
|};

public type AUTH_LEVEL "anonymous"|"function"|"admin";
public const annotation HTTPTriggerConfiguration HttpTrigger on source listener;

# HTTPTrigger annotation configuration.
# 
# + authLevel - The authentication level of the function
public type HTTPTriggerConfiguration record {|
    AUTH_LEVEL authLevel = "anonymous";
|};

public annotation Payload on parameter, return;

# Defines the Header resource signature parameter.
#
# + name - Specifies the name of the required header
public type HttpHeader record {|
    string name?;
|};

# The annotation which is used to define the Header resource signature parameter.
public annotation HttpHeader Header on parameter;

# @azurefunctions:HttpOutput annotation
public const annotation HttpOutput on parameter, return;

# Queue annotation configuration.
# 
# + queueName - The queue name
# + connection - The name of the app setting which contains the Storage connection string
public type QueueConfiguration record {|
    string queueName;
    string connection = "AzureWebJobsStorage";
|};

# @azurefunctions:QueueOutput annotation.
public const annotation QueueConfiguration QueueOutput on parameter, return;

# @azurefunctions:QueueOutput annotation.
public const annotation QueueConfiguration QueueTrigger on source listener, service;

# TimerTrigger annotation configuration.
# 
# + schedule - The CRON expression representing the timer schedule.
# + runOnStartup - The flag to state if the timer should be started on a runtime restart
public type TimerTriggerConfiguration record {|
    string schedule;
    boolean runOnStartup = true;
|};

# @azurefunctions:TimerTrigger annotation.
public const annotation TimerTriggerConfiguration TimerTrigger on source listener, service;

# Blob annotation configuration.
# 
# + path - The blob container path
# + connection - The name of the app setting which contains the Storage connection string
public type BlobConfiguration record {|
    string path;
    string connection = "AzureWebJobsStorage";
|};

# @azurefunctions:BlobTrigger annotation.
public const annotation BlobConfiguration BlobTrigger on source listener, service;

# @azurefunctions:BlobInput annotation.
public const annotation BlobConfiguration BlobInput on parameter;

# @azurefunctions:BlobOutput annotation.
public const annotation BlobConfiguration BlobOutput on return;

# CosmosDB trigger annotation configuration.
# 
# + connectionStringSetting -  The name of the app setting which contains the connection string for CosmosDB account
# + databaseName - The database name
# + collectionName - The collection name
# + leaseConnectionStringSetting - The name of the app setting which contains the lease connection string
# + leaseDatabaseName - The name of the lease database
# + leaseCollectionName - The name of the collection used to store leases
# + createLeaseCollectionIfNotExists - The lease collection is automatically created when this is set to true
# + leasesCollectionThroughput - The request throughput of the lease collection
# + leaseCollectionPrefix - The prefix of the leases created
# + feedPollDelay - The time delay (in milliseconds) in polling a partition for new changes in the feed
# + leaseAcquireInterval - The time (in milliseconds) the interval to create a task to check if partitions are distributed evenly
# + leaseExpirationInterval - The lease expiration interval in milliseconds
# + leaseRenewInterval - The lease renewal interval in milliseconds
# + checkpointFrequency - The interval (in milliseconds) between lease checkpoints
# + maxItemsPerInvocation - The maximum number of items received per function call
# + startFromBeginning - Tells the trigger to read changes from the beginning of the collection's change history
# + preferredLocations - A comma-seperated list of regions as preferred locations for geo-replicated database accounts
public type CosmosDBTriggerConfiguration record {|
    string connectionStringSetting;
    string databaseName;
    string collectionName;
    string leaseConnectionStringSetting?;
    string leaseDatabaseName?;
    string leaseCollectionName?;
    boolean createLeaseCollectionIfNotExists = true;
    int leasesCollectionThroughput?;
    string leaseCollectionPrefix?;
    int feedPollDelay?;
    int leaseAcquireInterval?;
    int leaseExpirationInterval?;
    int leaseRenewInterval?;
    int checkpointFrequency?;
    int maxItemsPerInvocation?;
    boolean startFromBeginning?;
    string preferredLocations?;    
|};

# @azurefunctions:CosmosDBTrigger annotation.
public const annotation CosmosDBTriggerConfiguration CosmosDBTrigger on source listener, service;

# CosmosDB input annotation configuration.
# 
# + connectionStringSetting -  The name of the app setting which contains the connection string for CosmosDB account
# + databaseName - The database name
# + collectionName - The collection name
# + id - The id of the document to retrieve
# + sqlQuery - An Azure Cosmos DB SQL query used to retrieve multiple documents
# + partitionKey - The partition key value for lookups
# + preferredLocations - A comma-seperated list of regions as preferred locations for geo-replicated database accounts
public type CosmosDBInputConfiguration record {|
    string connectionStringSetting;
    string databaseName;
    string collectionName;
    string id?;
    string sqlQuery?;
    string|int|float partitionKey?;
    string preferredLocations?;
|};

# @azurefunctions:CosmosDBInput annotation.
public const annotation CosmosDBInputConfiguration CosmosDBInput on parameter;

# CosmosDB output annotation configuration.
# 
# + connectionStringSetting -  The name of the app setting which contains the connection string for CosmosDB account
# + databaseName - The database name
# + collectionName - The collection name
# + createIfNotExists - Creates the collection is it does not exist
# + partitionKey - The partition key name
# + collectionThroughput - The throughput of a newly created collection
# + preferredLocations - A comma-seperated list of regions as preferred locations for geo-replicated database accounts
# + useMultipleWriteLocations - If true, uses multi-region writes
public type CosmosDBOutputConfiguration record {|
    string connectionStringSetting;
    string databaseName;
    string collectionName;
    boolean createIfNotExists?;
    int collectionThroughput?;
    string partitionKey?;
    string preferredLocations?;
    boolean useMultipleWriteLocations?;
|};

# @azurefunctions:CosmosDBOutput annotation.
public const annotation CosmosDBOutputConfiguration CosmosDBOutput on return;

# Twilio annotation configuration.
# 
# + accountSidSetting - The app setting which holds the Twilio Account Sid
# + authTokenSetting - The app setting which holds the Twilio authentication token
# + from - The phone number the SMS is sent from
# + to - The phone number the SMS is sent to
public type TwilioSmsConfiguration record {|
    string accountSidSetting = "AzureWebJobsTwilioAccountSid";
    string authTokenSetting	= "AzureWebJobsTwilioAuthToken";
    string 'from;
    string to;
    
|};

# @azurefunctions:TwilioSmsOutput annotation.
public const annotation TwilioSmsConfiguration TwilioSmsOutput on return;

# BindingName annotation configuration.
# 
# + name - The binding name
public type BindingNameConfiguration record {|
    string name?;
|};

# @azurefunctions:BindingName annotation.
public const annotation BindingNameConfiguration BindingName on parameter;

