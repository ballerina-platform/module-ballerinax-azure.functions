# Specification: Ballerina Azure Function Library

_Owners_:  @Hevayo @Anuruddhal @Xlight05 @Thevakumar-Luheerathan
_Reviewers_: @Hevayo @Anuruddhal @Xlight05 @Thevakumar-Luheerathan 
_Created_:  2022/10/28
_Updated_:  2022/10/28
_Edition_: Swan Lake

## Introduction
This is the specification for the Azure Function library of [Ballerina language](https://ballerina.io/), which provides necessary functionalities to produce and deploy functions in Azure platforms.

This specification has evolved and may continue to evolve in the future.
If you have any feedback or suggestions about the library, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-standard-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). Based on the outcome of the discussion, the specification and implementation can be updated. Community feedback is always welcome.
The conforming implementation of the specification is released and included in the distribution. Any deviation from the specification is considered a bug.

## Contents
1. [Overview](#1-overview)
2. [Constructs](#2-constructs)
    * 2.1. [Listener](#21-listener)
    * 2.2. [Service](#22-service)
        * 2.2.1. [Service type](#221-service-type)
        * 2.2.2. [Service base path](#222-service-base-path)
        * 2.2.3. [Service declaration](#223-service-declaration)
    * 2.3. [Annotations](#23-annotations)
        * 2.3.1. [Listener annotations](#231-listener-annotations)
        * 2.3.2. [Service annotations](#232-service-annotations)
        * 2.3.3. [Function annotation](#233-function-annotation)
        * 2.3.4. [Input parameter annotations](#234-input-parameter-annotations)
        * 2.3.5. [Return type annotations](#235-return-type-annotations)
        * 2.4. [Functions](#24-functions)
            * 2.4.1. [Resource functions](#241-resource-functions)
              * 2.4.1.1. [Accessor](#2411-accessor)
              * 2.4.1.2. [Resource name](#2412-resource-name)
              * 2.4.1.3. [Path parameter](#2413-path-parameter)
              * 2.4.1.4. [Signature parameters](#2414-signature-parameters)
                  * 2.4.1.4.1. [Query parameter](#24141-query-parameter)
                  * 2.4.1.4.2. [Payload parameter](#24142-payload-parameter)
                  * 2.4.1.4.3. [Header parameter](#24143-header-parameter)
              * 2.4.1.5. [Return types](#2415-return-types)
                  * 2.4.1.5.1. [Status code response](#24151-status-code-response)
                  * 2.4.1.5.2. [Return nil](#24152-return-nil)
                  * 2.4.1.5.3. [Default response status codes](#24153-default-response-status-codes)
            * 2.4.2. [Remote functions](#242-remote-functions)
3. [Functionalities](#3-functionalities)
    * 3.1. [Artifact generation(Compiler functionality)](#31-artifact-generation-compiler-functionality)
      * 3.1.1. [Folder structure](#311-folder-structure)
      * 3.1.2. [Function name generation](#312-function-name-generation)
      * 3.1.3. [`function.json` generation](#313-functionjson-generation)
    * 3.2. [Runtime routing and data binding (Runtime functionality)](#32-runtime-routing-and-data-binding-runtime-functionality)
4. [Triggers and bindings](#4-triggers-and-bindings)
    * 4.1. [Triggers](#41-triggers)
      * 4.1.1. [HttpTrigger](#411-httptrigger)
      * 4.1.2. [QueueTrigger](#412-queuetrigger)
      * 4.1.3. [CosmosDBTrigger](#413-cosmosdbtrigger)
      * 4.1.4. [TimerTrigger](#414-timertrigger)
      * 4.1.5. [BlobTrigger](#415-blobtrigger)
    * 4.2. [Output bindings](#42-output-bindings)
      * 4.2.1. [HttpOutput](#421-httpoutput)
      * 4.2.2. [QueueOutput](#422-queueoutput)
      * 4.2.3. [BlobOutput](#423-bloboutput)
      * 4.2.4. [CosmosDBOutput](#424-cosmosdboutput)
      * 4.2.5. [TwilioSmsOutput](#425-twiliosmsoutput)
    * 4.3. [Input bindings](#42-output-bindings)
      * 4.3.1. [CosmosDBInput](#431-cosmosdbinput)
      * 4.3.2. [BlobInput](#432-blobinput)


## 1. Overview    
Ballerina language provides first-class support for writing cloud oriented programs. The Azure functions library uses these language constructs and creates the programming model to produce and consume Azure function APIs through annotation-based and service-based implementation.
Ballerina Azure Function library is designed in a manner that it can support standard Azure Function triggers and bindings concept. And also, The code written with Ballerina HTTP library can be utilised as Azure Function code without much modification on it. 
In a high level of abstraction, Each Azure Function trigger is represented with a Ballerina service listener. Each service, which is attached to above listener, consists of set-of resource functions or a single remote function with a predefined name. 
## 2. Constructs
This section provides the information on necessary Ballerina constructs which are used to define an Azure function.
### 2.1. Listener
In Azure function Ballerina implementation, Each trigger is represented with a listener. Following table shows mapping between triggers and listeners.

| Trigger             	 | Listener         | 
|-----------------------|------------------|
| HttpTrigger           | HttpListener	    |
| BlobTrigger           | BlobListener	    |
| QueueTrigger          | QueueListener	   | 
| CosmosDBTrigger       | CosmosDBListener | 
| TimerTrigger          | TimerListener	   |

A listener can be declared as follows honoring to the generic
[listener declaration](https://ballerina.io/spec/lang/2021R1/#section_8.3.1). A listener can be declared inline with a service or using object constructor as follows. 

```ballerina
import ballerinax/azure_functions as af;

// Listener declaration with object constructor
listener af:HttpListener ep = new ();

service "hello" on ep {
}
```


```ballerina
import ballerinax/azure_functions as af;

//Inline listener declaration
service "hello" on new af:HttpListener() {
}
```
While declaring the listener, port should not be specified.

### 2.2. Service
Service is a collection of resource functions in the case of `HttpListener`. In the case of other listener,It contains single remote function. These functions are the network entry points of a ballerina program.
In addition to that a service can contain public and private functions which can be accessed by calling with `self`.
#### 2.2.1. Service type
```ballerina
public type HttpService distinct service object {

};
```
#### 2.2.2. Service base path
The base path is considered during the request dispatching to discover the service. Identifiers and string literals
are allowed to be stated as base path and it should be started with `/`. The base path is optional and it will be
defaulted to `/` when not defined. If the base path contains any special characters, those should be escaped or defined
as string literals

```ballerina
service hello\-world on new af:HttpListener() {
   resource function get foo() {

   }
}

service http:Service "hello-world" on new af:HttpListener() {
   resource function get foo() {

   }
}
```
#### 2.2.3. Service declaration
The [Service declaration](https://ballerina.io/spec/lang/2021R1/#section_8.3.2) is a syntactic sugar for creating a
service and it is the mostly used approach for creating a service. The declaration gets desugared into creating a
listener object, creating a service object, attaching the service object to the listener object.

```ballerina
service /foo/bar on new af:HttpListener() {
  resource function get greeting() returns string {
      return "hello world";
  }
}
```
### 2.3. Annotations
In Ballerina Azure Function library, Annotations are used to provide necessary configurations and meta data. 
#### 2.3.1. Listener annotations
Each Listener can be attached with an optional annotation which contains the configurations required for the azure platform. `HttpTrigger`, `QueueTrigger`, `BlobTrigger`, `CosmosDBTrigger`,
`TimerTrigger` are only the supported listener annotations.
```ballerina
@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();
```
`HttpTrigger` supports following fields.

| Supported field | Description                                                 |
|-----------------|-------------------------------------------------------------|
| authLevel       | Authentication level ("anonymous" or "function" or "admin") |

`QueueTrigger` supports following fields. 

| Supported field | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| queueName       | The queue name                                                           |
| connection      | The name of the app setting which contains the Storage connection string |

`BlobTrigger` supports following fields.

| Supported field | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| path            | The blob container path                                                  |
| connection      | The name of the app setting which contains the Storage connection string |

`CosmosDBTrigger` supports following fields.

| Supported field                  | Description                                                                                            |
|----------------------------------|--------------------------------------------------------------------------------------------------------|
| connectionStringSetting          | The name of the app setting which contains the connection string for CosmosDB account                  |
| databaseName                     | The database name                                                                                      |
| collectionName                   | The collection name                                                                                    |
| leaseConnectionStringSetting     | The name of the app setting which contains the lease connection string                                 |
| leaseDatabaseName                | The name of the lease database                                                                         |
| leaseCollectionName              | The name of the collection used to store leases                                                        |
| createLeaseCollectionIfNotExists | The lease collection is automatically created when this is set to true                                 |
| leasesCollectionThroughput       | The request throughput of the lease collection                                                         |
| leaseCollectionPrefix            | The prefix of the leases created                                                                       |
| feedPollDelay                    | The time delay (in milliseconds) in polling a partition for new changes in the feed                    |
| leaseAcquireInterval             | The time (in milliseconds) the interval to create a task to check if partitions are distributed evenly |
| leaseExpirationInterval          | The lease expiration interval in milliseconds                                                          |
| leaseRenewInterval               | The lease renewal interval in milliseconds                                                             |
| checkpointFrequency              | The interval (in milliseconds) between lease checkpoints                                               |
| maxItemsPerInvocation            | The maximum number of items received per function call                                                 |
| startFromBeginning               | Tells the trigger to read changes from the beginning of the collection's change history                |
| preferredLocations               | A comma-seperated list of regions as preferred locations for geo-replicated database accounts          |

`TimerTrigger` supports following fields.

| Supported field | Description                                                           |
|-----------------|-----------------------------------------------------------------------|
| schedule        | The CRON expression representing the timer schedule.                  |
| runOnStartup    | The flag to state if the timer should be started on a runtime restart |

#### 2.3.2. Service annotations
Service annotation are provided immediately before a service definition starts. Service definitions contains the configurations which necessary for whole service. Currently, [`@http:ServiceConfig`](https://github.com/ballerina-platform/module-ballerina-http/blob/master/docs/spec/spec.md#41-service-configuration) is used to provide the necessary configuration for a service defined on HttpListener. `HttpTrigger`, `QueueTrigger`, `BlobTrigger`, `CosmosDBTrigger`,
`TimerTrigger` are trigger annotations which can be used as a service annotations in the case of inline listener.
```ballerina
// Service config annotation usage 
@http:ServiceConfig {
    treatNilableAsOptional : false
}
service af:HttpService /foo/bar on new af:HttpListener() {
  resource function get greeting() returns string {
      return "hello world";
  }
}

// Inline listener case
@af:QueueTrigger {
    queueName: "queue2"
}
service "queue" on new af:QueueListener() {
    remote function onMessage(string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg;
    }
}
```
`treatNilableAsOptional` (from `@http:ServiceConfig`) is the only field supported by Ballerina Azure Function.
#### 2.3.3. Function annotation
`Function` is the only annotation supported by functions. It is used to specify the Azure function name. It is an optional annotation. When the annotation is not specified,
function name is generated based on [service path and function declaration](#312-function-name-generation).

| Supported field | Description                     |
|-----------------|---------------------------------|
| name            | Provide the Azure Function name |

#### 2.3.4. Input parameter annotations
[`@http:Payload`](https://github.com/ballerina-platform/module-ballerina-http/blob/master/docs/spec/spec.md#43-payload-annotation) and [`@http:header`](https://github.com/ballerina-platform/module-ballerina-http/blob/master/docs/spec/spec.md#45-header-annotation) are the only annotations supported from Ballerina Http library. These annotations are allowed only within a service defined on `HttpListener`. 
Other than that, `BlobInput`, `CosmosDBInput` and `BindingName` are allowed annotations from Ballerina Azure Function library itself.

`BlobInput` supports following fields.

| Supported field | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| path            | The blob container path                                                  |
| connection      | The name of the app setting which contains the Storage connection string |

`CosmosDBInput` supports following fields.

| Supported field         | Description                                                                                   |
|-------------------------|-----------------------------------------------------------------------------------------------|
| connectionStringSetting | The name of the app setting which contains the connection string for CosmosDB account         |
| databaseName            | The database name                                                                             |
| collectionName          | The collection name                                                                           |
| id                      | The id of the document to retrieve                                                            |
| sqlQuery                | An Azure Cosmos DB SQL query used to retrieve multiple documents                              |
| partitionKey            | The partition key value for lookups                                                           |
| preferredLocations      | A comma-separated list of regions as preferred locations for geo-replicated database accounts |

`BindingName` supports following fields.

| Supported field | Description      |
|-----------------|------------------|
| name            | The binding name |

```ballerina
resource function post payload/octaToByte(@http:Payload byte[] greeting) returns @af:HttpOutput string|error {
    return string:fromBytes(greeting);
}
```

#### 2.3.5. Return type annotations
`CosmosDBOutput`, `TwilioSmsOutput`, `HttpOutput` , `QueueOutput` and `BlobOutput` are only the possible return type annotations.
```ballerina
    resource function get err/empty/payload(@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from get empty payload";
    }
```
If any annotation is not specified in the return type, HttpOutput will be taken implicitly.

`CosmosDBOutput` supports following fields.

| Supported field           | Description                                                                                   |
|---------------------------|-----------------------------------------------------------------------------------------------|
| connectionStringSetting   | The name of the app setting which contains the connection string for CosmosDB account         |
| databaseName              | The database name                                                                             |
| collectionName            | The collection name                                                                           |
| createIfNotExists         | Creates the collection is it does not exist                                                   |
| partitionKey              | The partition key name                                                                        |
| collectionThroughput      | The throughput of a newly created collection                                                  |
| preferredLocations        | A comma-separated list of regions as preferred locations for geo-replicated database accounts |
| useMultipleWriteLocations | If true, uses multi-region writes                                                             |

`TwilioSmsOutput` supports following fields.

| Supported field   | Description                                                 |
|-------------------|-------------------------------------------------------------|
| accountSidSetting | The app setting which holds the Twilio Account Sid          |
| authTokenSetting  | The app setting which holds the Twilio authentication token |
| from              | The phone number the SMS is sent from                       |
| to                | The phone number the SMS is sent to                         |

`HttpOutput` does not have any fields.

`QueueOutput` supports following fields.

| Supported field | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| queueName       | The queue name                                                           |
| connection      | The name of the app setting which contains the Storage connection string |

`BlobOutput` supports following fields.

| Supported field | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| path            | The blob container path                                                  |
| connection      | The name of the app setting which contains the Storage connection string |


### 2.4. Functions
Azure function in Ballerina supports both resource function and remote function. Resource functions and remote functions are supported by HttpListener and non-HttpListeners respectively.
#### 2.4.1. Resource functions
A method of a service can be declared as a [resource function](https://ballerina.io/spec/lang/2021R1/#resources)
which is associated with configuration data that is invoked by a network message by a Listener. Users write the
business logic inside a resource and expose it over the network.
##### 2.4.1.1. Accessor
The accessor-name of the resource represents the HTTP method and it can be get, post, put, delete, head, patch, options
and default. If the accessor is unmatched, 405 Method Not Allowed response is returned. When the accessor name is
stated as default, any HTTP method can be matched to it in the absence of an exact match. Users can define custom
methods such as copy, move based on their requirement. A resource which can handle any method would look like as
follows. This is useful when handling unmatched verbs.

```ballerina
resource function 'default NAME_TEMPLATE () {
    
}
```
##### 2.4.1.2. Resource name
The resource-name represents the path of the resource which is considered during the request dispatching. The name can
be hierarchical(foo/bar/baz). Each path identifier should be separated by `/` and first path identifier should not
contain a prefixing `/`. If the paths are unmatched, 404 NOT FOUND response is returned.
```ballerina
resource function post hello() {
    
}
```
Only the identifiers can be used as resource path not string literals. Dot identifier is
used to denote the `/` only if the path contains a single identifier.
```ballerina
resource function post .() {
    
}
```
Any special characters can be used in the path by escaping.
```ballerina
resource function post hello\-world() {
    
}
```
##### 2.4.1.3. Path parameter
The path parameter segment is also a part of the resource name which is declared within brackets along with the type.
As per the following resource name, baz is the path param segment and it’s type is string. Like wise users can define
string, int, boolean, float, and decimal typed path parameters. If the paths are unmatched, 404 NOT FOUND response
is returned. If the segment failed to parse into the expected type, 500 Internal Server Error response is returned.

```ballerina
resource function post foo/bar/[string baz]/qux() {
    // baz is the path param
}

resource function get data/[int age]/[string name]/[boolean status]/[float weight]() returns json {
   int balAge = age + 1;
   float balWeight = weight + 2.95;
   string balName = name + " lang";
   if (status) {
       balName = name;
   }
   json responseJson = { Name:name, Age:balAge, Weight:balWeight, Status:status, Lang: balName};
   return responseJson;
}
```

If multiple path segments needs to be matched after the last identifier, Rest param should be used at the end of the
resource name as the last identifier. string, int, boolean, float, and decimal types are supported as rest parameters.
```ballerina
resource function get foo/[string... bar]() returns json {
   json responseJson = {"echo": bar[0]};
   return responseJson;
}
```

Using both `'default` accessor and the rest parameters, a default resource can be defined to a service. This
default resource can act as a common destination where the unmatched requests (either HTTP method or resource path) may
get dispatched.

```ballerina
resource function 'default [string... s]() {

}
```

##### 2.4.1.4. Signature parameters
###### 2.4.1.4.1. Query parameter

The query param is a URL parameter which is available as a resource function parameter and it's not associated
with any annotation or additional detail. This parameter is not compulsory and not ordered. The type of query param
are as follows

```ballerina
type BasicType boolean|int|float|decimal|string|map<json>;
```

The same query param can have multiple values. In the presence of multiple such values,  If the user has specified
the param as an array type, then all values will return. If not the first param values will be returned. As per the
following resource function, the request may contain at least two query params with the key of bar and id.
Eg : “/hello?bar=hi&id=56”

```ballerina
resource function get hello(string bar, int id) { 
    
}
```

If the query parameter is not defined in the function signature, then the query param binding does not happen. If a
query param of the request URL has no corresponding parameter in the resource function, then that param is ignored.
If the parameter is defined in the function, but there is no such query param in the URL, that request will lead
to a 400 BAD REQUEST error response unless the type is nilable (string?)

<table>
<tr>
<th> Case </th>
<th>  Resource argument </th>
<th>  Query </th>
<th>  Mapping </th>
</tr>
<tr>
<td rowspan=4> 1 </td>
<td rowspan=4> string foo </td>
<td> foo=bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo=</td>
<td> "" </td>
</tr>
<tr>
<td> foo</td>
<td> Error : no query param value found for 'foo' </td>
</tr>
<tr>
<td> No query</td>
<td> Error : no query param value found for 'foo' </td>
</tr>
<tr>
<td rowspan=4> 2 </td>
<td rowspan=4> string? foo </td>
<td> foo=bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo=</td>
<td> "" </td>
</tr>
<tr>
<td> foo</td>
<td> nil </td>
</tr>
<tr>
<td> No query</td>
<td> nil </td>
</tr>
</table>

###### 2.4.1.4.2. Payload parameter

The payload parameter is used to access the request payload during the resource invocation. When the payload param is
defined with @http:Payload annotation, the listener deserialize the inbound request payload based on the media type
which retrieved by the `Content-type` header of the request. The data binding happens thereafter considering the
parameter type. The type of payload parameter can be one of the `anytype`. If the header is not present or not a
standard header, the binding type is inferred by the parameter type.

Following table explains the compatible `anydata` types with each common media type. In the absence of a standard media
type, the binding type is inferred by the payload parameter type itself. If the type is not compatible with the media
type, error is returned.

| Ballerina Type | Structure               | "text" | "xml" | "json" | "x-www-form-urlencoded" | "octet-stream" |
|----------------|-------------------------|--------|-------|--------|-------------------------|----------------|
| boolean        |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | boolean[]               | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<boolean\>          | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<boolean\>\> | ❌      | ❌     | ✅      | ❌                       | ❌              |
| int            |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | int[]                   | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<int\>              | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<int\>\>     | ❌      | ❌     | ✅      | ❌                       | ❌              |
| float          |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | float[]                 | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<float\>            | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<float\>\>   | ❌      | ❌     | ✅      | ❌                       | ❌              |
| decimal        |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | decimal[]               | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<decimal\>          | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<decimal\>\> | ❌      | ❌     | ✅      | ❌                       | ❌              |
| byte[]         |                         | ✅      | ❌     | ✅      | ❌                       | ✅              |
|                | byte[][]                | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<byte[]\>           | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<byte[]\>\>  | ❌      | ❌     | ✅      | ❌                       | ❌              |
| string         |                         | ✅      | ❌     | ✅      | ✅                       | ❌              |
|                | string[]                | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<string\>           | ❌      | ❌     | ✅      | ✅                       | ❌              |
|                | table\<map\<string\>\>  | ❌      | ❌     | ✅      | ❌                       | ❌              |
| xml            |                         | ❌      | ✅     | ❌      | ❌                       | ❌              |
| json           |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | json[]                  | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<json\>             | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<json\>\>    | ❌      | ❌     | ✅      | ❌                       | ❌              |
| map            |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map[]                   | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<map\>              | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<map\<map\>\>     | ❌      | ❌     | ✅      | ❌                       | ❌              |
| record         |                         | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | record[]                | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | map\<record\>           | ❌      | ❌     | ✅      | ❌                       | ❌              |
|                | table\<record\>         | ❌      | ❌     | ✅      | ❌                       | ❌              |

 ```bal
   resource function post query(string name, @http:Payload string greeting) returns @af:HttpOutput string|error {
               return "Hello from the query " + greeting + " " + name;
   }
```


###### 2.4.1.4.3. Header parameter
The header parameter is to access the inbound request headers The header param is defined with `@http:Header` annotation
The type of header param can be defined as follows;

```ballerina
type BasicType string|int|float|decimal|boolean;
public type HeaderParamType ()|BasicType|BasicType[]|record {| BasicType...; |};
```

When multiple header values are present for the given header, the first header value is returned when the param
type is `string` or any of the basic types. To retrieve all the values, use `string[]` type or any array of the
basic types. This parameter is not compulsory and not ordered.

The header param name is considered as the header name during the value retrieval. However, the header annotation name
field can be used to define the header name whenever user needs some different variable name for the header.

User cannot denote the type as a union of pure type, array type, or record type together, that way the resource
cannot infer a single type to proceed. Hence, returns a compiler error.

In the absence of a header when the param is defined in the resource signature, listener returns 400 BAD REQUEST unless
the type is nilable.

```ballerina
//Single header value extraction
resource function post hello1(@http:Header string referer) {
    
}

//Multiple header value extraction
resource function post hello2(@http:Header {name: "Accept"} string[] accept) {
    
}

public type RateLimitHeaders record {|
    string x\-rate\-limit\-id;
    int x\-rate\-limit\-remaining;
    string[] x\-rate\-limit\-types;
|};

//Populate selected headers to a record
resource function get hello3(@http:Header RateLimitHeaders rateLimitHeaders) {
}
```

If the requirement is to access all the header of the inbound request, it can be achieved through the `http:Headers`
typed param in the signature. It does not need the annotation and not ordered.

```ballerina
resource function get hello3(http:Headers headers) {
   String|error referer = headers.getHeader("Referer");
   String[]|error accept = headers.getHeaders("Accept");
   String[] keys = headers.getHeaderNames();
}
```

The header consists of header name and values. Sometimes user may send header without value(`foo:`). In such
situations, when the header param type is nilable, the values returns nil and same happened when the complete header is
not present in the request. In order to avoid the missing detail, a service level configuration has introduced naming
`treatNilableAsOptional`

```ballerina
@http:ServiceConfig {
    treatNilableAsOptional : false
}
service /headerparamservice on HeaderBindingIdealEP {

    resource function get test1(@http:Header string? foo) returns json {
        
    }
}
```

<table>
<tr>
<th>  Case </th>
<th>  Resource argument </th>
<th>  Header </th>
<th>  Current Mapping (treatNilableAsOptional=true - Default) </th>
<th>  Ideal Mapping (treatNilableAsOptional=false) </th>
</tr>
<tr>
<td rowspan=3> 1 </td>
<td rowspan=3> string foo </td>
<td> foo:bar </td>
<td> bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo:</td>
<td> Error : no header value found for 'foo' </td>
<td> Error : no header value found for 'foo' </td>
</tr>
<tr>
<td> No header</td>
<td> Error : no header value found for 'foo' </td>
<td> Error : no header value found for 'foo' </td>
</tr>
<tr>
<td rowspan=3> 2 </td>
<td rowspan=3> string? foo </td>
<td> foo:bar </td>
<td> bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo:</td>
<td> nil </td>
<td> nil </td>
</tr>
<tr>
<td> No header</td>
<td> nil </td>
<td> Error : no header value found for 'foo' </td>
</tr>
</table>

##### 2.4.1.5. Return types
The resource function supports anydata, error?, http:Response and http:StatusCodeResponse as return types.

```ballerina
resource function XXX NAME_TEMPLATE () returns @http:Payload anydata|http:Response|http:StatusCodeResponse|http:Error? {
}
```

In addition to that the `@http:Payload` annotation can be specified along with anydata return type
mentioning the content type of the outbound payload.

```ballerina
resource function get test() returns @http:Payload {mediaType:"text/id+plain"} string {
    return "world";
}
```

Based on the return types respective header value is added as the `Content-type` of the `http:Response`.

| Type                                                                  | Content Type             |
|-----------------------------------------------------------------------|--------------------------|
| ()                                                                    | -                        |
| string                                                                | text/plain               |
| xml                                                                   | application/xml          |
| byte[]                                                                | application/octet-stream |
| int, float, decimal, boolean                                          | application/json         |
| map\<json\>, table<map\<json\>>, map\<json\>[], table<map\<json\>>)[] | application/json         |
| http:StatusCodeResponse                                               | application/json         |

###### 2.4.1.5.1. Status code response
The status code response records are defined in the HTTP module for every HTTP status code.

```ballerina
type Person record {
   string name;
};
resource function put person(string name) returns record {|*http:Created; Person body;|} {
   Person person = {name:name};
   return {
       mediaType: "application/person+json",
       headers: {
           "X-Server": "myServer"
       },
       body: person
   };
}
```

Following is the `http:Ok` definition. Likewise, all the status codes are provided.

```ballerina
public type Ok record {
   readonly StatusOk status;
   string mediaType;
   map<string|string[]> headers?;
   anydata body?;
};

resource function get greeting() returns http:Ok|http:InternalServerError {
   http:Ok ok = { body: "hello world", headers: { xtest: "foo"} };
   return ok;
}
```
###### 2.4.1.5.2. Return nil
The return nil from the resource will return 202 ACCEPTED response.
```ballerina
resource function post person(@http:Payload Person p) {
    int age = p.age;
    io:println(string `Age is: ${age}`);
}
```
###### 2.4.1.5.3. Default response status codes
To improve the developer experience for RESTful API development, following default status codes will be used in outbound
response when returning `anydata` directly from a resource function.

| Resource Accessor | Semantics                                                     | Status Code             |
|-------------------|---------------------------------------------------------------|-------------------------|
| GET               | Retrieve the resource                                         | 200 OK                  |
| POST              | Create a new resource                                         | 201 Created             |
| PUT               | Create a new resource or update an existing resource          | 200 OK                  |
| PATCH             | Partially update an existing resource                         | 200 OK                  |
| DELETE            | Delete an existing resource                                   | 200 OK                  |
| HEAD              | Retrieve headers                                              | 200 OK                  |
| OPTIONS           | Retrieve permitted communication options                      | 200 OK                  |

#### 2.4.2. Remote functions
[Remote methods](https://ballerina.io/spec/lang/master/#section_5.5.3.4) are used for network interaction. Remote functions are supported for all listeners except `HttpListener`. 
For each of the above listeners a remote functions needs to be defined with a predefined name. Following table shows the predefined name and listener mapping.

| Listener           | method name |
|--------------------|-------------|
| QueueListener      | onMessage   |
| BlobListener       | onUpdated   |
| CosmosDBListener   | onUpdated   |
| TimerListener      | onTrigger   |

These method bodies are executed during the trigger operation(Eg: Queue is updated).

## 3. Functionalities
### 3.1. Artifact generation (Compiler functionality)
#### 3.1.1. Folder structure
Generated artifacts are placed as follows.
```bash
target
└─── azure_functions
      ├── derived-functions-1
      │    └── function.json
      ├── derived-functions-2
      │    └── function.json
      ├── host.json
      ├── local.settings.json
      └── JAR/Native file
```
#### 3.1.2. Function name generation
Function name is generated by default unless the `Function` annotation is specified. In the case of `HttpListener`, Function name is generated as `accessor-serviceBasePath-resourceFunctionName-pathParm1-pathParm2`. If the function definition contains any path parameter, It is used as a part of function name derivation.
```Ballerina
service /hello on new af:HttpListener() {
    // Function name is "post-hello-foo-bar"
    resource function post foo/[string bar](@http:Payload string greeting) returns @af:HttpOutput string {
        return "Hello from foo param " + bar;
    }
}
```
Meanwhile, `serviceBasePath` is used as function name in all the non HttpListener cases.
```Ballerina
// Function name is "queue"
service "queue" on new af:QueueListener() {
    remote function onMessage(string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
        return "helloo " + inMsg;
    }
}
```
In case multiple functions lead to single function name derivation, numerical values are appended at the end of each derived function name with a hyphen.
```Ballerina
service /hello on new af:HttpListener() {
    // Function name is "post-hello-foo-bar-1"
    resource function post foo/[string bar](@http:Payload string greeting) returns @af:HttpOutput string  {
        return "Hello from foo param " + bar;
    }
    // Function name is "post-hello-foo-bar-2"
    resource function post foo/bar(@http:Payload string greeting) returns @af:HttpOutput string  {
        return "Hello from foo bar res";
    }
}
```
Generated function name is attached to each function with `Function` annotation as a part of compilation process.
#### 3.1.3. `function.json` generation
`function.json` is generated based on the trigger configurations (fields of listener annotations) and Ballerina function declaration.

### 3.2. Runtime routing and data binding (Runtime functionality)
Incoming request-function name (function name is obtained from the [`request payload`](https://learn.microsoft.com/en-us/azure/azure-functions/functions-custom-handlers#request-payload) in Azure platform) is matched with all the available function names
(All function names are extracted using `Function` annotations) and matching function is extracted. Similarly, `request payload` is used for data binding. All the parameters are extracted from `request payload` and bound based on the parameters' type in function definition(signature parameters, path parameters etc.). Once the necessary 
parameters are bound together and function body is executed,[Response payload](https://learn.microsoft.com/en-us/azure/azure-functions/functions-custom-handlers#response-payload) is generated and sent to Azure platform.

## 4. Triggers and bindings
Following table illustrates the supported triggers and bindings in Ballerina Azure Function.

Supported From Ballerina - :heavy_check_mark:     
Supported From Azure (Currently, No Ballerina support) - :white_check_mark:

| Type                	 | Trigger            	 | Input              	 | Output             	 |
|-----------------------|----------------------|----------------------|----------------------|
| Blob storage        	 | :heavy_check_mark: 	 | :heavy_check_mark: 	 | :heavy_check_mark: 	 |
| Azure Cosmos DB     	 | :heavy_check_mark: 	 | :heavy_check_mark: 	 | :heavy_check_mark: 	 |
| Azure SQL (preview) 	 | 	                    | :white_check_mark: 	 | :white_check_mark: 	 |
| Dapr                	 | 	                    | :white_check_mark: 	 | :white_check_mark: 	 |
| Event Grid          	 | :white_check_mark: 	 | 	                    | :white_check_mark: 	 |
| Event Hubs          	 | :white_check_mark: 	 | 	                    | :white_check_mark: 	 |
| HTTP & webhooks     	 | :heavy_check_mark: 	 | 	                    | :heavy_check_mark: 	 |
| IoT Hub             	 | :white_check_mark: 	 | 	                    | 	                    |
| Kafka               	 | :white_check_mark: 	 | 	                    | :white_check_mark: 	 |
| Mobile Apps         	 | 	                    | :white_check_mark: 	 | :white_check_mark: 	 |
| Notification Hubs   	 | 	                    | 	                    | :white_check_mark: 	 |
| Queue storage       	 | :heavy_check_mark: 	 | 	                    | :heavy_check_mark: 	 |
| RabbitMQ            	 | :white_check_mark: 	 | 	                    | :white_check_mark: 	 |
| SendGrid            	 | 	                    | 	                    | :white_check_mark: 	 |
| Service Bus         	 | :white_check_mark: 	 | 	                    | :white_check_mark: 	 |
| SignalR             	 | :white_check_mark: 	 | :white_check_mark: 	 | :white_check_mark: 	 |
| Table storage       	 | 	                    | :white_check_mark: 	 | :white_check_mark: 	 |
| Timer               	 | :heavy_check_mark: 	 | 	                    | 	                    |
| Twilio              	 | 	                    | 	                    | :heavy_check_mark: 	 |


### 4.1. Triggers
[Triggers](https://learn.microsoft.com/en-us/azure/azure-functions/functions-triggers-bindings?tabs=csharp) are the starting point of functions. They cause functions to invoke. A function must have only one trigger. This is the motivation for listener-trigger mapping. Currently, Ballerina supports 5 triggers.
#### 4.1.1. HttpTrigger
[HttpTrigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook-trigger?tabs=in-process%2Cfunctionsv2&pivots=programming-language-java) is something that can be invoked with an HTTP request. This can be defined as follows in Ballerina Azure Function.
```ballerina
service /hello on new af:HttpListener() {
    resource function post foo/[string bar](@http:Payload json greeting) returns @af:HttpOutput json  {
        return greeting;
    }
}
```
Above function can be accessed through `curl -d <DATA> -H 'Content-Type: application/json' https://<AZURE_FUNCTION_NAME>.azurewebsites.net/foo/bar` endpoint. Request payload and header can be accessed within the function body through `@http:Payload` and `@http:Header` respectively. Most of the Ballerina http resource functions can be used directly.
#### 4.1.2. QueueTrigger
The [QueueTrigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-queue-trigger?tabs=in-process%2Cextensionv5&pivots=programming-language-java) runs a function as messages are added to Azure Queue storage.
 ```ballerina
@af:QueueTrigger {
   queueName: "queue2"
}
listener af:QueueListener queueListener = new af:QueueListener();
service "queue" on queueListener {
   remote function onMessage (string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
               return "helloo "+ inMsg;
   }
}
```
When `queue2` gets a message, above remote function is invoked. The message can be accessed through `inMsg` variable(Since queueTrigger `name` field is set to `inMsg` in `function.json`.).
#### 4.1.3. CosmosDBTrigger
This function (function with [CosmosDBTrigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-cosmosdb-v2-trigger?tabs=in-process%2Cfunctionsv2&pivots=programming-language-java) ) is invoked when there are inserts or updates in the specified database and collection.
 ```ballerina
@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();
 
service "cosmos" on cosmosEp {
   remote function onUpdated (DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```
When collection `c2` in `db1` database gets an entry, above remote function is invoked. The entry can be accessed through `inMsg` variable(Since CosmosDBTrigger `name` field is set to `inMsg` in the `function.json`.).
#### 4.1.4. TimerTrigger
A [timer trigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-timer?tabs=in-process&pivots=programming-language-java) lets to run a function on a schedule.
 ```ballerina
@af:TimerTrigger { schedule: "*/10 * * * * *" }
listener af:TimerListener timerEp = new ();
 
service "timer" on timerEp {
   remote function onTriggered (json inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```
Based on schedule, above remote function is invoked. Payload can be accessed through `inMsg` variable(Since TimerTrigger `name` field is set to `inMsg` in the `function.json`.).
#### 4.1.5. BlobTrigger
The [BlobTrigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-blob-trigger?tabs=in-process%2Cextensionv5&pivots=programming-language-java) starts a function when a new or updated blob is detected in the given path.
```ballerina
@af:BlobTrigger {
    path: "bpath1/{name}"
}
listener af:BlobListener blobListener = new af:BlobListener();

service "blob" on blobListener {
    remote function onUpdated(byte[] blobIn, @af:BindingName {} string name) returns @af:BlobOutput {
        path: "bpath1/newBlob"
    } byte[]|error {
        return blobIn;
    }
}
```
With the blob `bpath1` update, above function is invoked. The updated blob contents can be accessed through `blobIn` variable(Since BlobTrigger `name` field is set to `blobIn` in the `function.json`.).
`BindingName` is used to access the name of the created blob in the function.
### 4.2. Output bindings
[Output bindings](https://learn.microsoft.com/en-us/azure/azure-functions/functions-triggers-bindings?tabs=csharp) are used to provide outputs to some other resources. In Ballerina Azure Function, Output bindings are handled as return parameters. They are attached with the `returns` keyword with proper annotation and return type. 
#### 4.2.1. HttpOutput
[HttpOutput binding](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook-output?tabs=in-process&pivots=programming-language-java) is used to respond to the HTTP request sender (HTTP trigger). This binding requires an HTTP trigger and allows to customize the response associated with the trigger's request.
It is an optional annotation in Ballerina Azure Function.
```ballerina
service /hello on new af:HttpListener() {
    resource function post foo/[string bar](@http:Payload string greeting,) returns @af:HttpOutput string  {
        return "Hello from foo param " + bar;
    }
```
#### 4.2.2. QueueOutput
With the [QueueOutput bindings](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-queue-output?tabs=in-process%2Cextensionv5&pivots=programming-language-java), Specified queue can be updated with messages.
 ```ballerina
@af:QueueTrigger {
   queueName: "queue2"
}
listener af:QueueListener queueListener = new af:QueueListener();
service "queue" on queueListener {
   remote function onMessage (string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
               return "helloo "+ inMsg;
   }
}
```

#### 4.2.3. BlobOutput
[BlobOutput bindings](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-blob-output?tabs=in-process%2Cextensionv5&pivots=programming-language-java) allows to modify the specified blob.
```ballerina
service "blob" on blobListener {
    remote function onUpdated (byte[] blobIn, @af:BindingName { } string name) returns @af:BlobOutput { 
        path: "bpath1/newBlob" } byte[]|error {
        return blobIn;
    }
}
```
#### 4.2.4. CosmosDBOutput
[CosmosDBOutput binding](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-cosmosdb-v2-output?tabs=in-process%2Cfunctionsv2&pivots=programming-language-java) allows to write a new document to the cosmosDB storage.
```ballerina
@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c1"}
listener af:CosmosDBListener cosmosEp = new ();
service "cosmos" on cosmosEp {
  remote function onUpdated (@http:Payload DBEntry[] inMsg) returns @af:CosmosDBOutput {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"} DBEntry[]|error {
    return inMsg;
  }
}
```
#### 4.2.5. TwilioSmsOutput
[TwilioSmsOutput binding](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-twilio?tabs=in-process%2Cfunctionsv2&pivots=programming-language-java) allows to send messages to the specified telephone number.
```ballerina
@af:TimerTrigger { schedule: "*/10 * * * * *" }
listener af:TimerListener timerEp = new ();

service "timer" on timerEp {
  remote function onTriggered (@http:Payload json inMsg) returns @af:QueueOutput @af:TwilioSmsOutput { fromNumber: "+12069845840" }  string|error {
      string id = inMsg[0].id;
      return "helloo "+ id;
  }
}
```
### 4.3. Input bindings
[Input bindings](https://learn.microsoft.com/en-us/azure/azure-functions/functions-triggers-bindings?tabs=csharp) allows to get inputs from other resources. In Ballerina Azure Function, Input bindings are handled as input parameters with proper annotation and type.
#### 4.3.1. CosmosDBInput
[CosmosDBInput binding](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-cosmosdb-v2-input?tabs=in-process%2Cfunctionsv2&pivots=programming-language-java) uses the SQL API to retrieve one or more Azure Cosmos DB documents and passes them to the input parameter of the function.
```ballerina
resource function post db(@http:Payload string greeting, @af:CosmosDBInput {
    connectionStringSetting: "CosmosDBConnection",databaseName: "db1",
    collectionName: "c2", sqlQuery: "SELECT * FROM Items"} DBEntry[] input1) returns @af:HttpOutput string|error {
        return "Hello " + greeting + input1[0].id;
}
```
#### 4.3.2. BlobInput
[BlobInput binding](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-blob-input?tabs=in-process%2Cextensionv5&pivots=programming-language-java) allows to read blob storage data as input to an Azure Function.
```ballerina
resource function get isBlobEmpty(@af:BlobInput { path: "bpath1/newBlob"} byte[]|() input1) returns @af:HttpOutput boolean {
    if (input1 == ()) {
        return true;
    }
    return false;
}
```
